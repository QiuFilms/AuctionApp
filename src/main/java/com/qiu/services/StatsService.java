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
        // Returns the number of principal objects in the registry
        return sessionRegistry.getAllPrincipals().size();
    }

    public void updateAuctionsCount() {
        long count = countAllAuctions(); // Metoda w serwisie zwracająca liczbę aukcji
        // Wysyłamy informację o aukcjach
        WebSocketEndpointJSON.broadcastAuctions(String.valueOf(count));
    }

    public long countAllAuctions() {
        return auctionRepository.count(); // Zwraca typ long
    }
}