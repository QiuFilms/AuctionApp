package com.qiu.controllers;

import com.qiu.dto.AuthRequest;
import com.qiu.dto.AuthResponse;
import com.qiu.mmoauctions.config.JwtService;
import com.qiu.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/auth")
public class AuthController {


    private final UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtService jwtService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request, HttpServletResponse response) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        if(userDetails == null){
            return ResponseEntity.badRequest().build();

        }
        final String jwtToken = jwtService.generateToken(userDetails);
        Cookie cookie = new Cookie("jwt_token", jwtToken);
        cookie.setHttpOnly(true); // Zabezpiecza przed kradzieżą tokena przez złośliwe skrypty JS
        cookie.setPath("/"); // Ciasteczko działa na całej stronie
        cookie.setMaxAge(24 * 60 * 60); // 24 godziny
        response.addCookie(cookie);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request, HttpServletResponse response) {
        userService.registerNewUser(request);

        return login(request, response);
    }
}
