package com.qiu.repositories;

import com.qiu.entities.AuctionHistory;
import com.qiu.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuctionHistoryRepository extends JpaRepository<AuctionHistory, Long> {
    List<AuctionHistory> findByAuctionIdOrderByEventDateDesc(Long auctionId);
    // Już używane w AuctionService.closeExpiredAuctions()

    // Nowa — używana w /auctions/my-active-bids
    // Zwraca historię dla danego usera i typu zdarzenia (np. "BID")
    List<AuctionHistory> findByOwnerAndEventType(User owner, String eventType);

    // Alternatywna wersja po username — przydatna żeby uniknąć ładowania encji User
    @Query("SELECT h FROM AuctionHistory h WHERE h.owner.username = :username AND h.eventType = :eventType")
    List<AuctionHistory> findByOwnerUsernameAndEventType(
            @Param("username") String username,
            @Param("eventType") String eventType
    );

    // Zwraca ostatnie zdarzenie WATCH lub UNWATCH per aukcja dla danego użytkownika
// Usuń findLatestWatchEventPerAuction i zastąp dwoma prostymi metodami:

    // Wszystkie WATCH dla usera

    // Sprawdź czy istnieje WATCH nowszy niż ostatni UNWATCH dla danej aukcji i usera
    @Query("""
    SELECT COUNT(h) > 0 FROM AuctionHistory h
    WHERE h.owner.username = :username
      AND h.auction.id = :auctionId
      AND h.eventType = 'WATCH'
      AND h.eventDate > COALESCE(
          (SELECT MAX(h2.eventDate) FROM AuctionHistory h2
           WHERE h2.owner.username = :username
             AND h2.auction.id = :auctionId
             AND h2.eventType = 'UNWATCH'),
          CAST('1970-01-01' AS java.time.LocalDateTime)
      )
    """)
    boolean isActivelyWatching(@Param("username") String username, @Param("auctionId") Long auctionId);
}