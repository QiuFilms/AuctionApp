package com.qiu.websockets.config;

import com.qiu.websockets.WebSocketEndpointJSON;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketEndpointJSON(), "/websocketEndPointJSON").setAllowedOriginPatterns("*");
    }

    @Bean
    public WebSocketEndpointJSON webSocketEndpointJSON() {
        return new WebSocketEndpointJSON();
    }
}
