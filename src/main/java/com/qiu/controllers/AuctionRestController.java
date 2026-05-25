package com.qiu.controllers;


import com.qiu.dto.SellRequest;
import com.qiu.entities.Auction;
import com.qiu.entities.User;
import com.qiu.repositories.UserRepository;
import com.qiu.services.AuctionService;
import com.qiu.services.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/auctions")
public class AuctionRestController {
    @Autowired
    private AuctionService auctionService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/all")
    public List<Auction> getAllAuctions() {
        return auctionService.findAll(); // Pobiera dane z bazy
    }

    @Autowired
    private StatsService statsService; // Wstrzyknięcie serwisu z logiką WebSockets[cite: 3]

    @PostMapping("/sell")
    public ResponseEntity<?> sellItem(@RequestBody SellRequest request, Principal principal) {
        // Pobieramy nazwę użytkownika z sesji
        String username = principal.getName();

        // Znajdujemy użytkownika w bazie, aby uzyskać jego ID
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika"));

        try {
            // Przekazujemy ID pobrane z serwera, a nie z żądania HTTP
            auctionService.createAuctionFromItem(request.itemId, user.getId(), request.price, request.hours);
            statsService.updateAuctionsCount();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Błąd: " + e.getMessage());
        }
    }


}
