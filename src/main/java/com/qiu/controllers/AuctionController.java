package com.qiu.controllers;

import com.qiu.dto.SellRequest;
import com.qiu.entities.Auction;
import com.qiu.entities.User;
import com.qiu.services.AuctionHistoryService;
import com.qiu.services.AuctionService;
import com.qiu.services.StatsService;
import com.qiu.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Controller
public class AuctionController {

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private AuctionHistoryService auctionHistoryService;


    @Autowired
    private StatsService statsService;

    @Autowired
    private UserService userService;


    @GetMapping("/auctions")
    public String showAuctions(Model model, Principal principal) {
        String username = principal.getName();

        // 1. Pobierz użytkownika, aby mieć dostęp do awatara
        Optional<User> userOpt = userService.findByUsername(username);

        User user = userOpt.orElse(null);

        String base64Avatar = "";
        if (user != null && user.getAvatar() != null && user.getAvatar().length > 0) {
            base64Avatar = java.util.Base64.getEncoder().encodeToString(user.getAvatar());
        }



        // 3. Pobierz aukcje
        List<Auction> auctions = auctionService.availableAuctions(LocalDateTime.now(), username);
        Map<Long, String> leadingBidders = auctionService.getLeadingBiddersForAuctions(auctions);

        // 4. Dodaj atrybuty do modelu
        model.addAttribute("user", user); // Teraz przesyłasz obiekt User, a nie Optional
        model.addAttribute("base64Avatar", base64Avatar);
        model.addAttribute("auctions", auctions);
        model.addAttribute("auctionsCount", auctions.size());
        model.addAttribute("leadingBidders", leadingBidders);

        return "auctions";
    }



    @PostMapping("/sell")
    public String sellItem(SellRequest data, RedirectAttributes redirectAttributes) {
        try {
            auctionService.createAuctionFromItem(data.getItemId(), data.getPrice(), data.getHours());
            statsService.updateAuctionsCount();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/equipment";
    }



    @PutMapping("/auctions/{auctionId}/bid")
    @ResponseBody
    public ResponseEntity<?> placeBid(
            @PathVariable Long auctionId,
            @RequestParam float amount,
            Principal principal) {


        User user = userService.findByUsername(principal.getName())
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


        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika"));

        auctionService.saveWatchEvent(auctionId, user, watching);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/auctions/my-active-bids")
    @ResponseBody
    public List<Long> getMyActiveBids(Principal principal) {
        String username = principal.getName();

        List<Long> fromBids = auctionHistoryService.findByOwnerUsernameAndEventType(username, "BID");

        List<Long> watchedAuctionIds = auctionHistoryService.findByOwnerUsernameAndEventType(username, "WATCH");

        List<Long> fromWatch = watchedAuctionIds.stream()
                .filter(id -> auctionHistoryService.isActivelyWatching(username, id))
                .toList();


        return Stream.concat(fromBids.stream(), fromWatch.stream())
                .distinct()
                .filter(id -> auctionService.findById(id)
                        .map(a -> a.getEndDate().isAfter(LocalDateTime.now()))
                        .orElse(false))
                .toList();
    }
}