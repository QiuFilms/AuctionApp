package com.qiu.websockets;

import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiu.services.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketEndpointJSON extends AbstractWebSocketHandler {
    // Statyczny zbiór sesji musi być zainicjowany
    private static final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Autowired
    private StatsService statsService;


    // Mapa: auctionId -> zbiór sesji subskrybujących tę aukcję
    // To jest serce unicastu — żadna dodatkowa tabela w DB nie jest potrzebna
    private static final Map<Long, Set<WebSocketSession>> auctionSubscribers = new ConcurrentHashMap<>();

    // Mapa odwrotna: sessionId -> zbiór auctionId które obserwuje ta sesja
    // Potrzebna do czyszczenia po rozłączeniu
    private static final Map<String, Set<Long>> sessionSubscriptions = new ConcurrentHashMap<>();


    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessionSubscriptions.put(session.getId(), ConcurrentHashMap.newKeySet());
        sessions.add(session);
        // Wyślij nową liczbę do WSZYSTKICH
        broadcastUsers(String.valueOf(statsService.getOnlineUsersCount()));
        broadcastAuctions(String.valueOf(statsService.countAllAuctions()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session); // Usunięcie sesji przy rozłączeniu

        Set<Long> subscribed = sessionSubscriptions.remove(session.getId());
        if (subscribed != null) {
            for (Long auctionId : subscribed) {
                Set<WebSocketSession> subscribers = auctionSubscribers.get(auctionId);
                if (subscribers != null) {
                    subscribers.remove(session);
                    // Usuń mapę aukcji jeśli nikt już nie obserwuje
                    if (subscribers.isEmpty()) {
                        auctionSubscribers.remove(auctionId);
                    }
                }
            }
        }
        System.out.println("Sesja usunięta. Aktualna liczba sesji: " + sessions.size());
    }

    public static void broadcast(String message) {
        System.out.println("Próba wysłania do " + sessions.size() + " sesji: " + message);

        for (WebSocketSession s : sessions) {
            System.out.println("Próba wysłania do sesji: " + s.getId() + ", czy jest otwarta: " + s.isOpen());
            if (s.isOpen()) {
                try {
                    s.sendMessage(new TextMessage(message));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void broadcastUsers(String count) {
        String message = buildMessage("USERS", count);
        sessions.forEach(s -> sendToSession(s, message));
    }

    public static void broadcastAuctions(String count) {
        String message = buildMessage("AUCTIONS", count);
        sessions.forEach(s -> sendToSession(s, message));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
        String action = (String) payload.get("action");
        Long auctionId = Long.valueOf(payload.get("auctionId").toString());

        switch (action) {
            case "SUBSCRIBE" -> subscribeToAuction(session, auctionId);
            case "UNSUBSCRIBE" -> unsubscribeFromAuction(session, auctionId);
            default -> System.out.println("Nieznana akcja WebSocket: " + action);
        }
    }

    private void subscribeToAuction(WebSocketSession session, Long auctionId) {
        auctionSubscribers
                .computeIfAbsent(auctionId, id -> ConcurrentHashMap.newKeySet())
                .add(session);
        sessionSubscriptions
                .computeIfAbsent(session.getId(), id -> ConcurrentHashMap.newKeySet())
                .add(auctionId);

        System.out.println("Sesja " + session.getId() + " subskrybuje aukcję " + auctionId);
    }

    private void unsubscribeFromAuction(WebSocketSession session, Long auctionId) {
        Set<WebSocketSession> subscribers = auctionSubscribers.get(auctionId);
        if (subscribers != null) {
            subscribers.remove(session);
            if (subscribers.isEmpty()) {
                auctionSubscribers.remove(auctionId);
            }
        }
        Set<Long> userSubs = sessionSubscriptions.get(session.getId());
        if (userSubs != null) {
            userSubs.remove(auctionId);
        }

        System.out.println("Sesja " + session.getId() + " anuluje subskrypcję aukcji " + auctionId);
    }

    public static void broadcastBidUpdate(Long auctionId, float newPrice, String bidderUsername) {
        Set<WebSocketSession> subscribers = auctionSubscribers.get(auctionId);
        if (subscribers == null || subscribers.isEmpty()) return;

        String message = buildMessage("BID_UPDATE", Map.of(
                "auctionId", auctionId,
                "newPrice", newPrice,
                "bidder", bidderUsername
        ));

        for (WebSocketSession s : subscribers) {
            sendToSession(s, message);
        }
    }

    public static void broadcastAuctionEnded(Long auctionId, String winnerUsername) {
        Set<WebSocketSession> subscribers = auctionSubscribers.get(auctionId);
        if (subscribers == null || subscribers.isEmpty()) return;

        String message = buildMessage("AUCTION_ENDED", Map.of(
                "auctionId", auctionId,
                "winner", winnerUsername != null ? winnerUsername : "Brak ofert"
        ));

        for (WebSocketSession s : subscribers) {
            sendToSession(s, message);
        }

        // Wyczyść subskrybentów po zakończeniu aukcji
        auctionSubscribers.remove(auctionId);
    }

    private static void sendToSession(WebSocketSession session, String message) {
        if (session != null && session.isOpen()) {
            try {
                synchronized (session) { // synchronized bo Spring WS nie jest thread-safe na sesji
                    session.sendMessage(new TextMessage(message));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static String buildMessage(String type, Object data) {
        try {
            return objectMapper.writeValueAsString(Map.of("type", type, "data", data));
        } catch (Exception e) {
            return "{\"type\":\"ERROR\",\"data\":\"serialization error\"}";
        }
    }
}