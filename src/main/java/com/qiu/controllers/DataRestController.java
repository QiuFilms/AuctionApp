package com.qiu.controllers;

import com.qiu.entities.*;
import com.qiu.repositories.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api/data")
public class DataRestController {


    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;

    public DataRestController(UserRepository userRepository, AuctionRepository auctionRepository) {
        this.userRepository = userRepository;
        this.auctionRepository = auctionRepository;
    }
    @GetMapping("/equipment")
    public ResponseEntity<?> getEquipment(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ItemUser> items = user.getItems();

        if (items == null || items.isEmpty()) {
            return ResponseEntity.ok("Lista jest pusta");
        }

        return ResponseEntity.ok(items);
    }

    @GetMapping("/my-auctions")
    public ResponseEntity<?> getMyAuctions(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Auction> userAuctions = auctionRepository.findBySeller(user);

        return ResponseEntity.ok(userAuctions);
    }

    @GetMapping("/auctions")
    public ResponseEntity<?> getOtherAuctions(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Auction> otherAuctions = auctionRepository.findBySellerNot(user);
        return ResponseEntity.ok(otherAuctions);
    }
}