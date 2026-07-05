package com.qiu.services;

import com.qiu.websockets.WebSocketEndpointJSON;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class GraphDataService {
    private final Random random = new Random();

    @Scheduled(fixedRate = 2000)
    public void pushBarData() {
        double dbMain = 10 + (250 - 10) * random.nextDouble();
        double dbAuth = 10 + (250 - 10) * random.nextDouble();
        double activity = 10 + (250 - 10) * random.nextDouble();

        WebSocketEndpointJSON.broadcastGraphData(dbMain, dbAuth, activity);
    }
}
