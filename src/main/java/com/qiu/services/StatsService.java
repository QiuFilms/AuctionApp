package com.qiu.services;

import com.qiu.repositories.AuctionRepository;
import com.qiu.websockets.WebSocketEndpointJSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;

@Service
public class StatsService {
    @Autowired
    private SessionRegistry sessionRegistry;

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private AuctionRepository auctionRepository;


    public int getOnlineUsersCount() {
        return sessionRegistry.getAllPrincipals().size();
    }

    public void updateAuctionsCount() {
        long count = countAllAuctions();
        WebSocketEndpointJSON.broadcastAuctions(String.valueOf(count));
    }

    public long countAllAuctions() {
        return auctionRepository.count();
    }
}