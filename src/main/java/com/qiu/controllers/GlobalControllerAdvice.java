package com.qiu.controllers;

import com.qiu.entities.User;
import com.qiu.repositories.UserRepository;
import com.qiu.services.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {
    @Autowired
    private StatsService statsService;

    @ModelAttribute("onlineUsers")
    public int getOnlineUsers() {
        return statsService.getOnlineUsersCount();
    }

    @Autowired
    private UserRepository userRepository;

    @ModelAttribute("user")
    public User addLoggedInUserToModel() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String username = auth.getName();
            return userRepository.findByUsername(username).orElse(null);
        }
        return null;
    }
}
