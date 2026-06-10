package com.qiu.websockets;

import com.qiu.controllers.StatsController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
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
    private SessionRegistry sessionRegistry;

    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String sessionId = RequestContextHolder.currentRequestAttributes().getSessionId();
        Object principal = event.getAuthentication().getPrincipal();

        sessionRegistry.registerNewSession(sessionId, principal);

        statsController.updateOnlineUsersCount();
    }

    @EventListener
    public void onSessionChange(SessionDestroyedEvent event) {
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