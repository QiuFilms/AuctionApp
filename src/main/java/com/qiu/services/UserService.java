package com.qiu.services;

import com.qiu.dto.AuthRequest;
import com.qiu.entities.Item;
import com.qiu.entities.ItemUser;
import com.qiu.entities.User;
import com.qiu.repositories.ItemRepository;
import com.qiu.repositories.ItemUserRepository;
import com.qiu.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import javax.transaction.Transactional;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


@Service
public class UserService {

    @Autowired
    @Qualifier("dataSourceAuth")
    private DataSource dataSourceAuth;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemUserRepository itemUserRepository;


    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void save(User user){
        userRepository.save(user);
    }

    public Optional<User> findByUsername(String username){
        return userRepository.findByUsername(username);
    }

    @Transactional
    public void registerNewUser(AuthRequest registerRequest) {
        long newUserId = -1;

        String insertAuthSql = "INSERT INTO users (username, password, creation_date) VALUES (?, ?, ?)";

        try (Connection conn = dataSourceAuth.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertAuthSql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, registerRequest.getUsername());
            pstmt.setString(2, passwordEncoder.encode(registerRequest.getPassword()));
            pstmt.setObject(3, LocalDateTime.now());

            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    newUserId = rs.getLong(1);
                } else {
                    throw new Exception("Nie udało się pobrać wygenerowanego ID z bazy autoryzacji.");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }



        User finalUser = new User();
        finalUser.setId(newUserId);
        finalUser.setUsername(registerRequest.getUsername());
        finalUser.setWallet(1000.0f);
        finalUser.setAvatarContentType(null);
        finalUser.setAvatar(null);
        userRepository.save(finalUser);

        List<Item> availableItems = itemRepository.findAll();
        if (availableItems.size() >= 10) {
            List<Item> shuffled = new ArrayList<>(availableItems);
            Collections.shuffle(shuffled);
            List<Item> startingItems = shuffled.subList(0, 10);

            List<ItemUser> itemUserRelations = startingItems.stream().map(item -> {
                ItemUser itemUser = new ItemUser();
                itemUser.setUser(finalUser);
                itemUser.setItem(item);
                itemUser.setOnAuction(false);
                return itemUser;
            }).toList();

            itemUserRepository.saveAll(itemUserRelations);
        }
    }
}