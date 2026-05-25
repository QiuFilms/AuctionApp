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

    // ----------------------------------------------------------------
    // Licytacja — odejmij środki, zwróć poprzedniemu licytującemu
    // ----------------------------------------------------------------

    @Transactional
    public void placeBid(Long auctionId, float bidAmount, User bidder) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Aukcja nie istnieje"));

        // Walidacja — oferta musi być wyższa niż aktualna cena
        if (bidAmount <= auction.getPrice()) {
            throw new IllegalArgumentException(
                    "Oferta musi być wyższa niż aktualna cena: " + auction.getPrice()
            );
        }

        // Sprawdź czy licytujący ma wystarczające środki
        User freshBidder = userRepository.findById(bidder.getId())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika"));
        if (freshBidder.getWallet() < bidAmount) {
            throw new IllegalArgumentException(
                    "Niewystarczające środki. Masz: " + freshBidder.getWallet()
            );
        }

        // Znajdź poprzedniego licytującego z historii i zwróć mu środki
        auctionHistoryRepository
                .findByAuctionIdOrderByEventDateDesc(auctionId)
                .stream()
                .filter(h -> "BID".equals(h.getEventType()))
                .findFirst()
                .ifPresent(lastBid -> {
                    User previousBidder = lastBid.getOwner();
                    // Odśwież encję żeby mieć aktualny stan portfela
                    User freshPrevious = userRepository.findById(previousBidder.getId())
                            .orElse(null);
                    if (freshPrevious != null) {
                        freshPrevious.setWallet(freshPrevious.getWallet() + auction.getPrice());
                        userRepository.save(freshPrevious);
                        System.out.println("DEBUG: Zwrócono " + auction.getPrice()
                                + " dla " + freshPrevious.getUsername());
                    }
                });

        // Odejmij środki od nowego licytującego
        freshBidder.setWallet(freshBidder.getWallet() - bidAmount);
        userRepository.save(freshBidder);
        System.out.println("DEBUG: Odjęto " + bidAmount + " od " + freshBidder.getUsername()
                + " | Pozostało: " + freshBidder.getWallet());

        // Zapisz nową cenę
        auction.setPrice(bidAmount);
        auctionRepository.save(auction);

        // Zapisz zdarzenie BID w historii
        AuctionHistory history = AuctionHistory.builder()
                .auction(auction)
                .eventDate(LocalDateTime.now())
                .eventType("BID")
                .owner(freshBidder)
                .build();
        auctionHistoryRepository.save(history);

        // Powiadom subskrybentów przez WebSocket
        WebSocketEndpointJSON.broadcastBidUpdate(auctionId, bidAmount, freshBidder.getUsername());
    }

    // ----------------------------------------------------------------
    // Zamknięcie aukcji — przelew do sprzedającego + transfer przedmiotu
    // ----------------------------------------------------------------

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

                // Przelej środki (finalną cenę) do sprzedającego
                if (auction.getSeller() != null) {
                    User seller = userRepository.findById(auction.getSeller().getId())
                            .orElse(null);
                    if (seller != null) {
                        seller.setWallet(seller.getWallet() + auction.getPrice());
                        userRepository.save(seller);
                        System.out.println("DEBUG: Przelano " + auction.getPrice()
                                + " do sprzedającego " + seller.getUsername());
                    }
                }

                // Przenieś przedmiot do ekwipunku zwycięzcy
                transferItem(auction.getItem().getId(), winner.getId());

            } else {
                // Brak ofert — zwróć przedmiot sprzedającemu (tylko wyczyść flagę)
                clearAuctionFlag(auction.getItem().getId());
            }

            // Powiadom subskrybentów WS
            WebSocketEndpointJSON.broadcastAuctionEnded(auction.getId(), winnerUsername);

            auctionRepository.delete(auction);
        }
    }

    // ----------------------------------------------------------------
    // Transfer przedmiotu — zmień właściciela w items_user
    // ----------------------------------------------------------------

    private void transferItem(Long itemId, Long newOwnerId) {
        // Używamy UPDATE, aby zmienić user_id dla danego przedmiotu
        // WHERE item_id = ? zapewnia, że zmieniamy właściciela właściwego przedmiotu
        String sql = "UPDATE items_user SET user_id = ?, on_auction = false WHERE item_id = ?";

        int rowsUpdated = jdbcTemplate.update(sql, newOwnerId, itemId);

        if (rowsUpdated > 0) {
            System.out.println("DEBUG: Przedmiot " + itemId + " przeniesiony do usera " + newOwnerId);
        } else {
            System.out.println("DEBUG: Błąd! Nie znaleziono przedmiotu " + itemId + " w tabeli items_user.");
        }
    }

    private void clearAuctionFlag(Long itemId) {
        jdbcTemplate.update("UPDATE items_user SET on_auction = false WHERE item_id = ?", itemId);
    }

    // ----------------------------------------------------------------
    // Tworzenie aukcji
    // ----------------------------------------------------------------

    @Transactional
    public void createAuctionFromItem(Long itemId, Long userId, double price, int hours) {
        ItemUser itemUser = itemUserRepository.findByUserIdAndItemId(userId, itemId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono przedmiotu u użytkownika"));

        itemUser.setOnAuction(true);
        itemUserRepository.save(itemUser);

        Auction auction = new Auction();
        auction.setSeller(itemUser.getUser());
        auction.setItem(itemUser.getItem());
        auction.setPrice((float) price);
        auction.setCreationDate(LocalDateTime.now());
        auction.setEndDate(LocalDateTime.now().plusMinutes(hours));
        auction.setTimeDuration(hours);
        auctionRepository.save(auction);

        AuctionHistory history = new AuctionHistory();
        history.setAuction(auction);
        history.setEventType("CREATED");
        history.setEventDate(LocalDateTime.now());
        history.setOwner(itemUser.getUser());
        auctionHistoryRepository.save(history);
    }

    // ----------------------------------------------------------------
    // Obserwowanie aukcji
    // ----------------------------------------------------------------

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