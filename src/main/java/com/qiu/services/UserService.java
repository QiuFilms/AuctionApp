package com.qiu.services;

import com.qiu.entities.Item;
import com.qiu.entities.ItemUser;
import com.qiu.entities.User;
import com.qiu.repositories.ItemRepository;
import com.qiu.repositories.ItemUserRepository;
import com.qiu.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemUserRepository itemUserRepository;

    @Transactional
    public void registerNewUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("USER");
        user.setWallet(5000.0f);
        user.setCreationDate(LocalDateTime.now());

        // 1. Zapisz użytkownika najpierw, aby uzyskać jego ID
        user = userRepository.save(user);

        List<Item> availableItems = itemRepository.findAll();
        if (availableItems.size() >= 10) {
            List<Item> shuffled = new ArrayList<>(availableItems);
            Collections.shuffle(shuffled);
            List<Item> startingItems = shuffled.subList(0, 10);

            User finalUser = user;
            List<ItemUser> itemUserRelations = startingItems.stream().map(item -> {
                ItemUser iu = new ItemUser();
                iu.setUser(finalUser); // Teraz user ma poprawne ID
                iu.setItem(item);
                iu.setOnAuction(false);
                return iu;
            }).collect(Collectors.toList());

            // 2. Zapisz relacje po zapisaniu użytkownika
            itemUserRepository.saveAll(itemUserRelations);
        }
    }
}