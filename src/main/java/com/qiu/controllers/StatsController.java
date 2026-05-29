package com.qiu.controllers;

import com.qiu.services.StatsService;
import com.qiu.websockets.WebSocketEndpointJSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class StatsController {
    @Autowired
    private WebSocketEndpointJSON webSocketEndpoint;
    @Autowired
    private StatsService statsService;

    public void updateOnlineUsersCount() {
        int count = statsService.getOnlineUsersCount();
        WebSocketEndpointJSON.broadcastUsers(String.valueOf(count));
    }
}