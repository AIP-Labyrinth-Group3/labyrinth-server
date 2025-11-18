package com.uni.gamesever.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.uni.gamesever.controller.SocketConnectionHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig
    implements WebSocketConfigurer {

    private final SocketConnectionHandler socketConnectionHandler;

    public WebSocketConfig(SocketConnectionHandler socketConnectionHandler) {
        this.socketConnectionHandler = socketConnectionHandler;
    }

    @Override
    public void registerWebSocketHandlers(
        WebSocketHandlerRegistry webSocketHandlerRegistry)
    {
        
        webSocketHandlerRegistry
        .addHandler(socketConnectionHandler,"/game")
        .setAllowedOrigins("*");
    }
}
