package com.qiu.controllers;

import com.qiu.entities.Auction;
import com.qiu.entities.Item;
import com.qiu.entities.ItemUser;
import com.qiu.entities.User;
import com.qiu.repositories.ItemRepository;
import com.qiu.repositories.UserRepository;
import com.qiu.services.AuctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
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
    public String showEquipment(
            @RequestParam(required = false, defaultValue = "all") String type,
            Model model,
            Principal principal) {

        Optional<User> userOptional = userRepository.findByUsername(principal.getName());

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            List<ItemUser> items = user.getItems();

            if (!"all".equals(type)) {
                items = items.stream()
                        .filter(item -> type.equalsIgnoreCase(item.getItem().getType()))
                        .collect(Collectors.toList());
            }

            // Mapa itemId -> Auction dla przedmiotów będących na aukcji
            Map<Long, Auction> auctionByItemId = auctionService.findAll().stream()
                    .filter(a -> a.getItem() != null)
                    .collect(Collectors.toMap(
                            a -> a.getItem().getId(),
                            a -> a,
                            (a1, a2) -> a1
                    ));

            model.addAttribute("items", items);
            model.addAttribute("itemsCount", items.size());
            model.addAttribute("selectedType", type);
            model.addAttribute("auctionByItemId", auctionByItemId);
        } else {
            return "redirect:/login";
        }

        return "equipment";
    }
}