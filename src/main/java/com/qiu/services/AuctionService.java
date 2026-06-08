package com.qiu.services;

import com.qiu.entities.*;
import com.qiu.repositories.*;
import com.qiu.websockets.WebSocketEndpointJSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuctionService {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private AuctionHistoryRepository auctionHistoryRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemUserRepository itemUserRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;


    public List<Auction> findAll() {
        return auctionRepository.findAll();
    }



    @Transactional
    public void placeBid(Long auctionId, float bidAmount, User bidder) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Aukcja nie istnieje"));

        if (auction.getEndDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Ta aukcja już się zakończyła i nie można jej licytować.");
        }

        if (bidAmount <= 0) {
            throw new IllegalArgumentException("Oferta musi być większa niż 0.");
        }
        if (bidAmount <= auction.getPrice()) {
            throw new IllegalArgumentException("Oferta musi być wyższa niż aktualna cena: " + auction.getPrice());
        }

        User freshBidder = userRepository.findById(bidder.getId())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika"));


        var lastBidOpt = auctionHistoryRepository
                .findByAuctionIdOrderByEventDateDesc(auctionId)
                .stream()
                .filter(h -> "BID".equals(h.getEventType()))
                .findFirst();

        if (lastBidOpt.isPresent()) {
            User previousBidder = lastBidOpt.get().getOwner();

            if (previousBidder.getId().equals(freshBidder.getId())) {
                float difference = bidAmount - auction.getPrice();
                if (freshBidder.getWallet() < difference) {
                    throw new IllegalArgumentException("Niewystarczające środki na podbicie oferty. Potrzebujesz dodatkowo: " + difference);
                }
                freshBidder.setWallet(freshBidder.getWallet() - difference);
            } else {
                if (freshBidder.getWallet() < bidAmount) {
                    throw new IllegalArgumentException("Niewystarczające środki. Masz: " + freshBidder.getWallet());
                }

                User freshPrevious = userRepository.findById(previousBidder.getId()).orElse(null);
                if (freshPrevious != null) {
                    freshPrevious.setWallet(freshPrevious.getWallet() + auction.getPrice());
                    userRepository.save(freshPrevious);
                }

                freshBidder.setWallet(freshBidder.getWallet() - bidAmount);
            }
        } else {
            if (freshBidder.getWallet() < bidAmount) {
                throw new IllegalArgumentException("Niewystarczające środki. Masz: " + freshBidder.getWallet());
            }
            freshBidder.setWallet(freshBidder.getWallet() - bidAmount);
        }

        userRepository.save(freshBidder);
        auction.setPrice(bidAmount);
        auctionRepository.save(auction);

        AuctionHistory history = AuctionHistory.builder()
                .auction(auction)
                .eventDate(LocalDateTime.now())
                .eventType("BID")
                .owner(freshBidder)
                .build();
        auctionHistoryRepository.save(history);

        WebSocketEndpointJSON.broadcastBidUpdate(auctionId, bidAmount, freshBidder.getUsername());
    }



    @Scheduled(fixedRate = 2000)
    @Transactional
    public void closeExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();
        List<Auction> expired = auctionRepository.findAll().stream()
                .filter(a -> a.getEndDate().isBefore(now))
                .toList();

        for (Auction auction : expired) {
            List<AuctionHistory> history =
                    auctionHistoryRepository.findByAuctionIdOrderByEventDateDesc(auction.getId());

            String winnerUsername = null;

            var lastBidOpt = history.stream()
                    .filter(h -> "BID".equals(h.getEventType()))
                    .findFirst();

            if (lastBidOpt.isPresent()) {
                AuctionHistory lastBid = lastBidOpt.get();
                User winner = lastBid.getOwner();
                winnerUsername = winner.getUsername();

                if (auction.getSeller() != null) {
                    User seller = userRepository.findById(auction.getSeller().getId())
                            .orElse(null);
                    if (seller != null) {
                        seller.setWallet(seller.getWallet() + auction.getPrice());
                        userRepository.save(seller);
                    }
                }

                transferItem(auction.getItem().getId(), winner.getId());

            } else {
                clearAuctionFlag(auction.getItem().getId());
            }

            WebSocketEndpointJSON.broadcastAuctionEnded(auction.getId(), winnerUsername);

        }
    }



    private void transferItem(Long itemId, Long newOwnerId) {
        String sql = "UPDATE items_user SET user_id = ?, on_auction = false WHERE item_id = ?";

        int rowsUpdated = jdbcTemplate.update(sql, newOwnerId, itemId);
    }

    private void clearAuctionFlag(Long itemId) {
        jdbcTemplate.update("UPDATE items_user SET on_auction = false WHERE item_id = ?", itemId);
    }



    @Transactional
    public void createAuctionFromItem(Long uniqueItemId, double price, int hours) {
        if (price <= 0) {
            throw new IllegalArgumentException("Cena musi być większa od zera.");
        }

        if (hours < 1 || hours > 24) {
            throw new IllegalArgumentException("Czas aukcji musi wynosić od 1 do 24 godzin.");
        }

        ItemUser itemUser = itemUserRepository.findById(uniqueItemId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono przedmiotu o ID: " + uniqueItemId));

        if (itemUser.getOnAuction()) {
            throw new IllegalArgumentException("Ten przedmiot jest już wystawiony na aukcji.");
        }

        itemUser.setOnAuction(true);
        itemUserRepository.save(itemUser);

        Auction auction = new Auction();
        auction.setSeller(itemUser.getUser());
        auction.setItem(itemUser.getItem());
        auction.setPrice((float) price);
        auction.setCreationDate(LocalDateTime.now());
        auction.setEndDate(LocalDateTime.now().plusHours(hours));
        auction.setTimeDuration(hours);
        auctionRepository.save(auction);

        AuctionHistory history = new AuctionHistory();
        history.setAuction(auction);
        history.setEventType("CREATED");
        history.setEventDate(LocalDateTime.now());
        history.setOwner(itemUser.getUser());
        auctionHistoryRepository.save(history);
    }


    @Transactional
    public void saveWatchEvent(Long auctionId, User user, boolean watching) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Aukcja nie istnieje"));

        AuctionHistory history = AuctionHistory.builder()
                .auction(auction)
                .eventDate(LocalDateTime.now())
                .eventType(watching ? "WATCH" : "UNWATCH")
                .owner(user)
                .build();
        auctionHistoryRepository.save(history);
    }
}