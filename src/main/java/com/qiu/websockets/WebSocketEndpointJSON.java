package com.qiu.websockets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiu.services.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketEndpointJSON extends AbstractWebSocketHandler {
    private static final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Autowired
    private StatsService statsService;


    private static final Map<Long, Set<WebSocketSession>> auctionSubscribers = new ConcurrentHashMap<>();

    private static final Map<String, Set<Long>> sessionSubscriptions = new ConcurrentHashMap<>();


    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessionSubscriptions.put(session.getId(), ConcurrentHashMap.newKeySet());
        sessions.add(session);
        broadcastUsers(String.valueOf(statsService.getOnlineUsersCount()));
        broadcastAuctions(String.valueOf(statsService.countAllAuctions()));
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        sessions.remove(session);

        Set<Long> subscribed = sessionSubscriptions.remove(session.getId());
        if (subscribed != null) {
            for (Long auctionId : subscribed) {
                Set<WebSocketSession> subscribers = auctionSubscribers.get(auctionId);
                if (subscribers != null) {
                    subscribers.remove(session);

                    if (subscribers.isEmpty()) {
                        auctionSubscribers.remove(auctionId);
                    }
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
    protected void handleTextMessage(@NonNull WebSocketSession session, TextMessage message) throws Exception {
        Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
        String action = (String) payload.get("action");
        Long auctionId = Long.valueOf(payload.get("auctionId").toString());

        switch (action) {
            case "SUBSCRIBE" -> subscribeToAuction(session, auctionId);
            case "UNSUBSCRIBE" -> unsubscribeFromAuction(session, auctionId);
        }
    }

    private void subscribeToAuction(WebSocketSession session, Long auctionId) {
        auctionSubscribers
                .computeIfAbsent(auctionId, id -> ConcurrentHashMap.newKeySet())
                .add(session);
        sessionSubscriptions
                .computeIfAbsent(session.getId(), id -> ConcurrentHashMap.newKeySet())
                .add(auctionId);

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

        auctionSubscribers.remove(auctionId);
    }

    public static void broadcastGraphData(double val1, double val2, double val3) {
        String message = buildMessage("BAR_GRAPH", Map.of(
                "dbMain", val1,
                "dbAuth", val2,
                "activity", val3
        ));

        sessions.forEach(s -> sendToSession(s, message));
    }

    private static void sendToSession(WebSocketSession session, String message) {
        if (session != null && session.isOpen()) {
            try {
                synchronized (session) {
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