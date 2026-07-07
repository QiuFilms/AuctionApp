package com.qiu.controllers;

import com.qiu.entities.Auction;
import com.qiu.entities.Item;
import com.qiu.entities.ItemUser;
import com.qiu.entities.User;
import com.qiu.services.AuctionService;
import com.qiu.services.ItemService;
import com.qiu.services.ItemUserService;
import com.qiu.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class EquipmentController {

    @Autowired
    public UserService userService;

    @Autowired
    public AuctionService auctionService;

    @Autowired
    public ItemService itemService;

    @Autowired
    public ItemUserService itemUserService;


    @GetMapping("/equipment")
    public String showEquipment(Model model, Principal principal) {
        User user = userService.findByUsername(principal.getName())
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

        List<Item> allAvailableItems = itemService.findAll();

        String username = principal.getName();

        Optional<User> userOpt = userService.findByUsername(username);

        String base64Avatar = "";
        if (user.getAvatar() != null && user.getAvatar().length > 0) {
            base64Avatar = java.util.Base64.getEncoder().encodeToString(user.getAvatar());
        }

        model.addAttribute("base64Avatar", base64Avatar);
        model.addAttribute("items", items);
        model.addAttribute("itemsCount", items.size());
        model.addAttribute("auctionByItemUserId", auctionByItemUserId);
        model.addAttribute("allAvailableItems", allAvailableItems);
        return "equipment";
    }

    @DeleteMapping("/equipment/items/{itemUserId}")
    @ResponseBody
    public ResponseEntity<?> deleteItemFromEquipment(@PathVariable Long itemUserId, Principal principal) {
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika"));


        ItemUser itemToDelete = user.getItems().stream()
                .filter(it -> it.getId().equals(itemUserId))
                .findFirst()
                .orElse(null);

        if (itemToDelete == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Nie masz takiego przedmiotu!"));
        }

        if (itemToDelete.getOnAuction()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Przedmiot jest wystawiony na aukcji!"));
        }

        user.getItems().remove(itemToDelete);
        itemUserService.delete(itemToDelete);
        userService.save(user);

        return ResponseEntity.ok(Map.of("success", true));
    }


    @PostMapping("/equipment/items/add")
    public String addSpecificItem(@RequestParam Long itemId, Principal principal) {
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje"));


        Item selectedItem = itemService.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Przedmiot nie istnieje"));


        ItemUser newItem = ItemUser.builder()
                .item(selectedItem)
                .user(user)
                .onAuction(false)
                .build();

        user.getItems().add(newItem);
        userService.save(user);

        return "redirect:/equipment?added=true";
    }
}