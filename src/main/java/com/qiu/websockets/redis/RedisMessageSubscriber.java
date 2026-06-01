package com.qiu.websockets.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiu.websockets.WebSocketEndpointJSON;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RedisMessageSubscriber {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void handleMessage(String message) {
        try {
            Map<String, Object> data = objectMapper.readValue(message, Map.class);

            Long auctionId = Long.valueOf(data.get("auctionId").toString());
            float newPrice = Float.parseFloat(data.get("newPrice").toString());
            String bidder = (String) data.get("bidder");

            WebSocketEndpointJSON.broadcastBidUpdate(auctionId, newPrice, bidder);
            System.out.println("Odebrano z Redisa i rozesłano do klientów: Aukcja " + auctionId + " - " + newPrice);

        } catch (Exception e) {
            System.err.println("Błąd podczas parsowania wiadomości z Redisa: " + message);
        }
    }
}