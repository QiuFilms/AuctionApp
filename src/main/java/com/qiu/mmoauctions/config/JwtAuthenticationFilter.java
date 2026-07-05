package com.qiu.mmoauctions.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    private JwtService jwtService;

    @Autowired
    @Lazy
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String jwt = null;
        String username = null; // Zmiana z 'final String' na zwykłego 'String'

        // 1. Szukamy ciasteczka z tokenem
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt_token".equals(cookie.getName())) {
                    jwt = cookie.getValue();
                    break;
                }
            }
        }

        // 2. BARDZO WAŻNE: Cała logika JWT odpala się TYLKO wtedy, gdy mamy token!
        if (jwt != null && !jwt.isEmpty()) {

            // Wyciągamy login dopiero, gdy mamy pewność, że jwt nie jest nullem
            username = jwtService.extractUsername(jwt);

            // Sprawdzamy, czy token zawiera nazwę użytkownika i czy nie jest on już zautoryzowany w aktualnym wątku
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Pobieramy użytkownika z bazy danych
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                // Sprawdzamy, czy token należy do tego użytkownika i czy nie wygasł
                if (jwtService.isTokenValid(jwt, userDetails)) {

                    // Tworzymy obiekt autentykacji, z którym Spring Security umie pracować
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

                    // Dorzucamy detale dotyczące samego żądania HTTP
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // Wrzucamy użytkownika do SecurityContextHolder – od teraz Spring wie, że jest zalogowany
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }

        // 3. Puszczamy żądanie dalej w łańcuchu filtrów.
        // Jeśli ktoś wchodzi na /login i jwt=null, kod po prostu przeskoczy tutaj i go wpuści jako gościa.
        filterChain.doFilter(request, response);
    }

}
