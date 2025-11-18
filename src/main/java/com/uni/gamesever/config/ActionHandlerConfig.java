package com.uni.gamesever.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.uni.gamesever.classes.MessageHandler;
import com.uni.gamesever.controller.SocketConnectionHandler;
import com.uni.gamesever.services.SocketBroadcastService;

@Configuration
public class ActionHandlerConfig {

    private final MessageHandler messageHandler;
    private final SocketBroadcastService socketBroadcastService;

    public ActionHandlerConfig(MessageHandler messageHandler, SocketBroadcastService socketBroadcastService) {
        this.messageHandler = messageHandler;
        this.socketBroadcastService = socketBroadcastService;
    }
    
    @Bean
    public SocketConnectionHandler socketConnectionHandler() {
        return new SocketConnectionHandler(socketBroadcastService, messageHandler);
    }
}
