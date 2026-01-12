package com.uni.gamesever.interfaces.Websocket;

import java.io.Console;
import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.gamesever.domain.events.GameTimeoutEvent;
import com.uni.gamesever.domain.exceptions.UserNotFoundException;
import com.uni.gamesever.domain.game.GameManager;
import com.uni.gamesever.domain.game.PlayerManager;
import com.uni.gamesever.domain.model.TurnState;
import com.uni.gamesever.infrastructure.GameTimerManager;
import com.uni.gamesever.interfaces.Websocket.messages.server.LobbyState;
import com.uni.gamesever.interfaces.Websocket.messages.server.ServerInfoEvent;
import com.uni.gamesever.services.SocketMessageService;

// Socket-Connection Configuration class
public class SocketConnectionHandler extends TextWebSocketHandler {
    private final SocketMessageService socketBroadcastService;
    private final MessageHandler messageHandler;
    @Value("${server.version}")
    private String serverVersion;
    @Value("${protocol.version}")
    private String protocolVersion;
    @Value("${server.motd}")
    private String serverMotd;
    private final ObjectMapper objectmapper = new ObjectMapper();
    private final GameManager gameManager;
    private final PlayerManager playerManager;
    private final GameTimerManager gameTimerManager;
    private final ApplicationEventPublisher eventPublisher;
    private final long playerReconnectionTimeout = 30;

    public SocketConnectionHandler(SocketMessageService socketBroadcastService, MessageHandler messageHandler,
            GameManager gameManager, PlayerManager playerManager, GameTimerManager gameTimerManager,
            ApplicationEventPublisher eventPublisher) {
        this.socketBroadcastService = socketBroadcastService;
        this.messageHandler = messageHandler;
        this.gameManager = gameManager;
        this.playerManager = playerManager;
        this.gameTimerManager = gameTimerManager;
        this.eventPublisher = eventPublisher;
    }

    // This method is executed when client tries to connect
    // to the sockets
    @Override
    public void afterConnectionEstablished(WebSocketSession session)
            throws Exception {

        super.afterConnectionEstablished(session);
        try {
            socketBroadcastService.addIncomingSession(session);
            System.out.println(session.getId() + " Connected");
            ServerInfoEvent serverInfoEvent = new ServerInfoEvent(OffsetDateTime.now().toString(), serverVersion,
                    protocolVersion, serverMotd);
            socketBroadcastService.sendMessageToSession(session.getId(),
                    objectmapper.writeValueAsString(serverInfoEvent));
        } catch (Exception e) {
            throw e;
        }
    }

    // When client disconnect from WebSocket then this
    // method is called
    @Override
    public void afterConnectionClosed(WebSocketSession session,
            CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        try {
            socketBroadcastService.removeDisconnectedSession(session);
            if (gameManager.getTurnInfo().getTurnState() != TurnState.NOT_STARTED) {
                playerManager.disconnectPlayer(session.getId());

                gameTimerManager.start(playerReconnectionTimeout, () -> {
                    try {
                        System.out.println("Player " + session.getId() + " failed to reconnect in time.");
                        eventPublisher.publishEvent(playerManager.removePlayer(session.getId()));
                    } catch (UserNotFoundException e) {
                        e.printStackTrace();
                    }
                });
            } else {
                playerManager.removePlayer(session.getId());
                LobbyState lobbyState = new LobbyState(playerManager.getNonNullPlayers());
                socketBroadcastService.broadcastMessage(
                        objectmapper.writeValueAsString(lobbyState));
            }
        } catch (Exception e) {
            throw e;
        }
    }

    // It will handle exchanging of message in the network
    // It will have a session info who is sending the
    // message Also the Message object passes as parameter
    @Override
    public void handleMessage(WebSocketSession session,
            WebSocketMessage<?> message)
            throws Exception {

        super.handleMessage(session, message);

        System.out.println("Message Received from user " + session.getId() + ": " + message.getPayload());

        // the whole game logic goes here
        if (!messageHandler.handleClientMessage(message.getPayload().toString(), session.getId())) {
            // we return a error message to the client

        }
    }
}
