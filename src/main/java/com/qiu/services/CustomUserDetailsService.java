package com.qiu.services;

import com.qiu.repositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        com.qiu.entities.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Gracz nie istnieje: " + username));

        // Zabezpieczenie przed null w bazie danych
        String userRole = user.getRole();
        if (userRole == null || userRole.trim().isEmpty()) {
            userRole = "USER"; // Domyślna rola awaryjna, jeśli pole w bazie jest puste
        }

        // Ujednolicenie przedrostka
        if (!userRole.startsWith("ROLE_")) {
            userRole = "ROLE_" + userRole;
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(userRole.replace("ROLE_", "")) // Wycinamy, bo .roles() sam doda "ROLE_"
                .build();
    }
}
