package com.qiu.repositories;

import com.qiu.entities.Auction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {
    // W razie potrzeby możesz tu dodać metody wyszukujące, np.:
    // List<Auction> findByItemName(String name);
}
