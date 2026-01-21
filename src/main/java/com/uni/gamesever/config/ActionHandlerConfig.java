package com.uni.gamesever.config;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.uni.gamesever.domain.ai.ServerAIManager;
import com.uni.gamesever.domain.game.GameManager;
import com.uni.gamesever.domain.game.PlayerManager;
import com.uni.gamesever.infrastructure.ReconnectTimerManager;
import com.uni.gamesever.interfaces.Websocket.ConnectionHandler;
import com.uni.gamesever.interfaces.Websocket.MessageHandler;
import com.uni.gamesever.interfaces.Websocket.ShutdownState;
import com.uni.gamesever.interfaces.Websocket.SocketConnectionHandler;
import com.uni.gamesever.services.SocketMessageService;

@Configuration
public class ActionHandlerConfig {

    private final MessageHandler messageHandler;
    private final SocketMessageService socketBroadcastService;
    private final GameManager gameManager;
    private final PlayerManager playerManager;
    private final ReconnectTimerManager reconnectTimerManager;
    private final ApplicationEventPublisher eventPublisher;
    private final ConnectionHandler playerConnectionHandler;
    private final ShutdownState shutdownState;
    private final ServerAIManager serverAIManager;

    public ActionHandlerConfig(MessageHandler messageHandler, SocketMessageService socketBroadcastService,
            GameManager gameManager,
            PlayerManager playerManager, ReconnectTimerManager reconnectTimerManager,
            ApplicationEventPublisher eventPublisher,
            ConnectionHandler playerConnectionHandler, ShutdownState shutdownState,
            ServerAIManager serverAIManager) {
        this.messageHandler = messageHandler;
        this.socketBroadcastService = socketBroadcastService;
        this.gameManager = gameManager;
        this.playerManager = playerManager;
        this.reconnectTimerManager = reconnectTimerManager;
        this.eventPublisher = eventPublisher;
        this.playerConnectionHandler = playerConnectionHandler;
        this.shutdownState = shutdownState;
        this.serverAIManager = serverAIManager;
    }

    @Bean
    public SocketConnectionHandler socketConnectionHandler() {
        return new SocketConnectionHandler(socketBroadcastService, messageHandler, gameManager,
                playerConnectionHandler,
                reconnectTimerManager,
                eventPublisher, playerManager, shutdownState, serverAIManager);
    }
}
