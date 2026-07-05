package com.qiu.services;

import com.qiu.entities.Auction;
import com.qiu.entities.AuctionHistory;
import com.qiu.repositories.AuctionHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AuctionHistoryService {

    @Autowired
    private AuctionHistoryRepository auctionHistoryRepository;

    public List<Long> findByOwnerUsernameAndEventType(String username, String type){
        return auctionHistoryRepository
                .findByOwnerUsernameAndEventType(username, "BID")
                .stream()
                .map(h -> h.getAuction().getId())
                .toList();
    }

    public Boolean isActivelyWatching(String username, Long id){
        return auctionHistoryRepository.isActivelyWatching(username, id);
    }

    public List<AuctionHistory> getAuctionHistory(Auction auction){
        return auctionHistoryRepository.findByAuctionId(auction.getId());
    }
}
