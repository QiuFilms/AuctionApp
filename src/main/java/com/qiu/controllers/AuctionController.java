package com.qiu.controllers;

import com.qiu.dto.SellRequest;
import com.qiu.entities.Auction;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @GetMapping("/auctions")
    public String showAuctions(Model model, Principal principal) {
        String username = principal.getName();
        List<Auction> auctions = auctionRepository.findAvailableAuctions(LocalDateTime.now(), username);

        Map<Long, String> leadingBidders = auctionService.getLeadingBiddersForAuctions(auctions);

        model.addAttribute("auctions", auctions);
        model.addAttribute("auctionsCount", auctions.size());
        model.addAttribute("leadingBidders", leadingBidders);
        return "auctions";
    }


    @PostMapping("/sell")
    public String sellItem(SellRequest data, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            String username = principal.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika"));

            auctionService.createAuctionFromItem(data.getItemId(), data.getPrice(), data.getHours());
            statsService.updateAuctionsCount();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }


        return "redirect:/equipment";
    }



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
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/auctions/{auctionId}/watch")
    @ResponseBody
    public ResponseEntity<?> toggleWatch(
            @PathVariable Long auctionId,
            @RequestParam boolean watching,
            Principal principal) {


        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika"));

        auctionService.saveWatchEvent(auctionId, user, watching);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/auctions/my-active-bids")
    @ResponseBody
    public List<Long> getMyActiveBids(Principal principal) {
        String username = principal.getName();

        List<Long> fromBids = auctionHistoryRepository
                .findByOwnerUsernameAndEventType(username, "BID")
                .stream()
                .map(h -> h.getAuction().getId())
                .toList();


        List<Long> watchedAuctionIds = auctionHistoryRepository
                .findByOwnerUsernameAndEventType(username, "WATCH")
                .stream()
                .map(h -> h.getAuction().getId())
                .distinct()
                .toList();


        List<Long> fromWatch = watchedAuctionIds.stream()
                .filter(id -> auctionHistoryRepository.isActivelyWatching(username, id))
                .toList();


        return Stream.concat(fromBids.stream(), fromWatch.stream())
                .distinct()
                .filter(id -> auctionRepository.findById(id)
                        .map(a -> a.getEndDate().isAfter(LocalDateTime.now()))
                        .orElse(false))
                .toList();
    }
}