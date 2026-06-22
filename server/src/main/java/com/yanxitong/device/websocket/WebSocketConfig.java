package com.yanxitong.device.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final ConfirmScreenWebSocketHandler confirmScreenWebSocketHandler;

    public WebSocketConfig(ConfirmScreenWebSocketHandler confirmScreenWebSocketHandler) {
        this.confirmScreenWebSocketHandler = confirmScreenWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(confirmScreenWebSocketHandler, "/ws/confirm-screen")
                .setAllowedOrigins("*");
    }
}

