package com.qiu.controllers;

import com.qiu.services.StatsService;
import com.qiu.websockets.WebSocketEndpointJSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class StatsController {
    @Autowired
    private WebSocketEndpointJSON webSocketEndpoint; // Wstrzyknij swój handler
    @Autowired
    private StatsService statsService;

    public void updateOnlineUsersCount() {
        int count = statsService.getOnlineUsersCount();
        System.out.println("Aktualizacja liczby użytkowników: " + count); // Sprawdź to w konsoli IntelliJ
        WebSocketEndpointJSON.broadcastUsers(String.valueOf(count));
    }
}