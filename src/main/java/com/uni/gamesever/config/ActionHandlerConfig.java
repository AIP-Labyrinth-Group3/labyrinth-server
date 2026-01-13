package com.uni.gamesever.config;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.uni.gamesever.domain.game.GameManager;
import com.uni.gamesever.domain.game.PlayerManager;
import com.uni.gamesever.infrastructure.GameTimerManager;
import com.uni.gamesever.interfaces.Websocket.ConnectionHandler;
import com.uni.gamesever.interfaces.Websocket.MessageHandler;
import com.uni.gamesever.interfaces.Websocket.SocketConnectionHandler;
import com.uni.gamesever.services.SocketMessageService;

@Configuration
public class ActionHandlerConfig {

    private final MessageHandler messageHandler;
    private final SocketMessageService socketBroadcastService;
    private final GameManager gameManager;
    private final PlayerManager playerManager;
    private final GameTimerManager gameTimerManager;
    private final ApplicationEventPublisher eventPublisher;
    private final ConnectionHandler playerConnectionHandler;

    public ActionHandlerConfig(MessageHandler messageHandler, SocketMessageService socketBroadcastService,
            GameManager gameManager,
            PlayerManager playerManager, GameTimerManager gameTimerManager, ApplicationEventPublisher eventPublisher,
            ConnectionHandler playerConnectionHandler) {
        this.messageHandler = messageHandler;
        this.socketBroadcastService = socketBroadcastService;
        this.gameManager = gameManager;
        this.playerManager = playerManager;
        this.gameTimerManager = gameTimerManager;
        this.eventPublisher = eventPublisher;
        this.playerConnectionHandler = playerConnectionHandler;
    }

    @Bean
    public SocketConnectionHandler socketConnectionHandler() {
        return new SocketConnectionHandler(socketBroadcastService, messageHandler, gameManager,
                playerConnectionHandler,
                gameTimerManager,
                eventPublisher, playerManager);
    }
}
