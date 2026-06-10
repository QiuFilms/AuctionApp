package com.qiu.repositories;

import com.qiu.entities.Auction;
import com.qiu.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {
    @Query("SELECT a FROM Auction a WHERE a.endDate > :now AND a.seller.username != :username")
    List<Auction> findAvailableAuctions(LocalDateTime now, String username);

    @Query("SELECT a FROM Auction a WHERE a.endDate > :now")
    List<Auction> findAvailableAuctions(LocalDateTime now);

    List<Auction> findBySellerNot(User seller);

    List<Auction> findBySeller(User seller);
}
