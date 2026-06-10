package com.qiu.controllers;

import com.qiu.entities.Auction;
import com.qiu.entities.ItemUser;
import com.qiu.entities.User;
import com.qiu.repositories.ItemRepository;
import com.qiu.repositories.UserRepository;
import com.qiu.services.AuctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class EquipmentController {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuctionService auctionService;

    @GetMapping("/equipment")
    public String showEquipment(Model model, Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje"));

        List<ItemUser> items = user.getItems();


        List<Auction> activeAuctions = auctionService.findAll().stream()
                .filter(a -> a.getEndDate().isAfter(LocalDateTime.now()))
                .collect(Collectors.toList());


        Map<Long, Auction> auctionByItemUserId = new HashMap<>();

        for (ItemUser itemUser : items) {
            for (Auction auction : activeAuctions) {
                if (auction.getItem().getId().equals(itemUser.getItem().getId())
                        && auction.getSeller().getId().equals(user.getId())) {

                    auctionByItemUserId.put(itemUser.getId(), auction);
                    activeAuctions.remove(auction);
                    break;
                }
            }
        }

        model.addAttribute("items", items);
        model.addAttribute("itemsCount", items.size());
        model.addAttribute("auctionByItemUserId", auctionByItemUserId);
        return "equipment";
    }
}