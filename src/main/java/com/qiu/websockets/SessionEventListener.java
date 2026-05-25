package com.qiu.websockets;

import com.qiu.controllers.StatsController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent; // Dodaj ten import
import org.springframework.security.core.session.SessionCreationEvent;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

@Component
public class SessionEventListener {
    @Autowired
    private StatsController statsController;

    @Autowired
    private SessionRegistry sessionRegistry; // Wstrzyknij rejestr tutaj
    // To zdarzenie wyłapuje moment udanego zalogowania użytkownika
    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        // Pobierz sesję z kontekstu
        String sessionId = RequestContextHolder.currentRequestAttributes().getSessionId();
        Object principal = event.getAuthentication().getPrincipal();

        // Ręczna rejestracja sesji użytkownika
        sessionRegistry.registerNewSession(sessionId, principal);

        // Wywołaj aktualizację liczby
        statsController.updateOnlineUsersCount();
    }


    @EventListener
    public void handleSessionCreated(SessionCreationEvent event) {
        statsController.updateOnlineUsersCount();
    }

    @EventListener
    public void handleSessionDestroyed(SessionDestroyedEvent event) {
        statsController.updateOnlineUsersCount();
    }
}