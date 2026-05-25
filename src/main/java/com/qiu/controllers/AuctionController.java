package com.qiu.controllers;

import com.qiu.dto.SellRequest;
import com.qiu.entities.Auction;
import com.qiu.entities.AuctionHistory;
import com.qiu.entities.User;
import com.qiu.repositories.AuctionHistoryRepository;
import com.qiu.repositories.AuctionRepository;
import com.qiu.repositories.UserRepository;
import com.qiu.services.AuctionService;
import com.qiu.services.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
public class AuctionController {

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StatsService statsService;

    @Autowired
    private AuctionHistoryRepository auctionHistoryRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    // Metoda obsługująca widok HTML
    @GetMapping("/auctions")
    public String showAuctions(Model model, Principal principal) {
        List<Auction> allAuctions = auctionService.findAll();
        String currentUsername = principal.getName();

        // Logowanie dla diagnostyki
        System.out.println("DEBUG: Zalogowany użytkownik to: " + currentUsername);
        allAuctions.forEach(a -> {
            String sellerName = (a.getSeller() != null) ? a.getSeller().getUsername() : "Brak";
            System.out.println("DEBUG: Aukcja ID " + a.getId() + " sprzedawca: " + sellerName +
                    " | Czy to ten sam użytkownik? " + sellerName.equals(currentUsername));
        });

        // Twój filtr
        List<Auction> filteredAuctions = allAuctions.stream()
                .filter(a -> a.getSeller() != null && !a.getSeller().getUsername().equals(currentUsername))
                .collect(Collectors.toList());

        model.addAttribute("auctions", filteredAuctions);
        model.addAttribute("auctionsCount", filteredAuctions.size());
        return "auctions";
    }


    @PostMapping("/sell")
    public String sellItem(@RequestParam Long itemId,
                           @RequestParam double price,
                           @RequestParam int hours,
                           Principal principal) {

        String username = principal.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika"));

        // Wywołanie logiki biznesowej
        auctionService.createAuctionFromItem(itemId, user.getId(), price, hours);
        statsService.updateAuctionsCount();

        // Po wystawieniu przedmiotu przekierowujemy z powrotem na ekwipunek
        return "redirect:/equipment?success=true";
    }

    // AuctionController — nowy endpoint
//    @GetMapping("/auctions/my-active-bids")
//    @ResponseBody
//    public List<Long> getMyActiveBids(Principal principal) {
//        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
//        // Zwróć ID aukcji gdzie ten user ma wpis BID w historii
//        return auctionHistoryRepository
//                .findByOwnerAndEventType(user, "BID")
//                .stream()
//                .map(h -> h.getAuction().getId())
//                // Filtruj tylko aktywne (nie zakończone)
//                .distinct()
//                .collect(Collectors.toList());
//    }

    @PostMapping("/auctions/{auctionId}/bid")
    @ResponseBody
    public ResponseEntity<?> placeBid(
            @PathVariable Long auctionId,
            @RequestParam float amount,
            Principal principal) {

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika"));
        try {
            auctionService.placeBid(auctionId, amount, user);
            return ResponseEntity.ok(Map.of("success", true, "newPrice", amount));
        } catch (IllegalArgumentException e) {
            // Zwróć błąd walidacji ceny do frontendu
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/auctions/{auctionId}/watch")
    @ResponseBody
    public ResponseEntity<?> toggleWatch(
            @PathVariable Long auctionId,
            @RequestParam boolean watching,
            Principal principal) {

        System.out.println("DEBUG WATCH endpoint: auctionId=" + auctionId
                + " watching=" + watching + " user=" + principal.getName()); // <-- dodaj

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika"));

        auctionService.saveWatchEvent(auctionId, user, watching);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/auctions/my-active-bids")
    @ResponseBody
    public List<Long> getMyActiveBids(Principal principal) {
        String username = principal.getName();

        // 1. Aukcje gdzie user licytował
        List<Long> fromBids = auctionHistoryRepository
                .findByOwnerUsernameAndEventType(username, "BID")
                .stream()
                .map(h -> h.getAuction().getId())
                .collect(Collectors.toList());

        // 2. Aukcje gdzie user kliknął serduszko i nie odsubskrybował
        //    Pobierz wszystkie aukcje gdzie był kiedykolwiek WATCH
        List<Long> watchedAuctionIds = auctionHistoryRepository
                .findByOwnerUsernameAndEventType(username, "WATCH")
                .stream()
                .map(h -> h.getAuction().getId())
                .distinct()
                .collect(Collectors.toList());

        //    Filtruj tylko te gdzie WATCH jest aktywny (brak nowszego UNWATCH)
        List<Long> fromWatch = watchedAuctionIds.stream()
                .filter(id -> auctionHistoryRepository.isActivelyWatching(username, id))
                .collect(Collectors.toList());

        System.out.println("DEBUG: fromBids = " + fromBids);
        System.out.println("DEBUG: fromWatch = " + fromWatch);

        return Stream.concat(fromBids.stream(), fromWatch.stream())
                .distinct()
                .filter(id -> auctionRepository.findById(id)
                        .map(a -> a.getEndDate().isAfter(LocalDateTime.now()))
                        .orElse(false))
                .collect(Collectors.toList());
    }
}