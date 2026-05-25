package com.qiu.mmoauctions.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Bezpieczne hashowanie haseł algorytmem BCrypt (wymóg z Twojego rejestru)
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    // Ten Bean obsługuje zdarzenia sesji (wymagany dla SessionRegistry)
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Zabezpieczenie przed atakami CSRF (wyłączamy tymczasowo, jeśli testujesz czyste API, ale dla Thymelafa warto zostawić)
                .csrf().disable()

                // Definiowanie uprawnień do ścieżek URL
                .authorizeRequests()
                .antMatchers("/login", "/register", "/css/**", "/js/**").permitAll() // Publiczne zasoby
                .antMatchers("/auctions", "/equipment").authenticated() // Panel gracza wymaga JAKIEGOKOLWIEK zalogowania
                .anyRequest().authenticated()
                .and()

                // Konfiguracja formularza logowania
                .formLogin()
                .loginPage("/login") // Wskazuje na Twój szablon login.html
                .loginProcessingUrl("/login") // URL na który formularz wysyła żądanie POST
                .defaultSuccessUrl("/auctions", true) // Gdzie przekierować po sukcesie
                .failureUrl("/login?error=true") // Gdzie odesłać przy błędnych danych
                .permitAll()
                .and()

                // Konfiguracja wylogowania
                .logout()
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll();
        http.sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // Tutaj to umieszczamy
                .maximumSessions(1)
                .sessionRegistry(sessionRegistry());

        return http.build();
    }
}