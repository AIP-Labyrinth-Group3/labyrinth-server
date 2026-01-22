package com.uni.gamesever.interfaces.Websocket;

import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.gamesever.domain.ai.ServerAIManager;
import com.uni.gamesever.domain.exceptions.ConnectionRejectedException;
import com.uni.gamesever.domain.exceptions.UserNotFoundException;
import com.uni.gamesever.domain.game.GameManager;
import com.uni.gamesever.domain.game.PlayerManager;
import com.uni.gamesever.domain.model.PlayerInfo;
import com.uni.gamesever.domain.model.TurnState;
import com.uni.gamesever.infrastructure.ReconnectTimerManager;
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
    private final ConnectionHandler connectionHandler;
    private final ReconnectTimerManager reconnectTimerManager;
    private final ApplicationEventPublisher eventPublisher;
    private final PlayerManager playerManager;
    private final ServerAIManager serverAIManager;
    private final long playerReconnectionTimeout = 30;
    private final ShutdownState shutdownState;
    private static final Logger log = LoggerFactory.getLogger(SocketConnectionHandler.class);

    public SocketConnectionHandler(SocketMessageService socketBroadcastService, MessageHandler messageHandler,
            GameManager gameManager, ConnectionHandler connectionHandler, ReconnectTimerManager reconnectTimerManager,
            ApplicationEventPublisher eventPublisher, PlayerManager playerManager, ShutdownState shutdownState,
            ServerAIManager serverAIManager) {
        this.socketBroadcastService = socketBroadcastService;
        this.messageHandler = messageHandler;
        this.gameManager = gameManager;
        this.connectionHandler = connectionHandler;
        this.reconnectTimerManager = reconnectTimerManager;
        this.eventPublisher = eventPublisher;
        this.playerManager = playerManager;
        this.shutdownState = shutdownState;
        this.serverAIManager = serverAIManager;
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
            log.info("Session {} verbunden", session.getId());
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

        if (shutdownState.isShuttingDown()) {
            return;
        }
        if (status.getCode() == CloseStatus.GOING_AWAY.getCode()) {
            return;
        }

        super.afterConnectionClosed(session, status);
        try {
            socketBroadcastService.removeDisconnectedSession(session);
            if (gameManager.getTurnInfo().getTurnState() != TurnState.NOT_STARTED) {
                connectionHandler.handleSituationWhenTheConnectionIsLost(session.getId());

                // ACTIVATE AI FOR DISCONNECTED PLAYER
                // Verwende identifierToken (bleibt fix) statt Session ID
                PlayerInfo player = playerManager.getPlayerById(session.getId());
                String identifierToken = player != null && player.getIdentifierToken() != null
                        ? player.getIdentifierToken()
                        : session.getId();

                serverAIManager.activateAI(identifierToken);
                System.out.println("🤖 AI activated for disconnected player: " + identifierToken + " (session: "
                        + session.getId() + ")");

                reconnectTimerManager.start(session.getId(), playerReconnectionTimeout, () -> {
                    if (playerManager.getPlayerById(session.getId()).getIsConnected()) {
                        return;
                    }

                    System.out.println("⏱️  Player " + identifierToken + " failed to reconnect in time.");
                    log.info("Spieler " + identifierToken + " hat sich nicht rechtzeitig wiederverbunden.");

                    // WICHTIG: Wenn AI aktiv ist, NICHT den Spieler entfernen!
                    // Die AI soll weiterspielen bis der Spieler sich wieder verbindet
                    if (serverAIManager.isAIActive(identifierToken)) {
                        System.out.println("🤖 AI bleibt aktiv - Spieler kann sich jederzeit wieder verbinden");
                        log.info("AI bleibt aktiv für Spieler: {}", identifierToken);
                        // Spieler bleibt im Spiel, AI spielt weiter
                        return;
                    }

                    // Nur wenn AI NICHT aktiv ist, entferne den Spieler wie bisher
                    try {
                        System.out.println("❌ Entferne Spieler aus dem Spiel (keine AI aktiv)");
                        eventPublisher.publishEvent(
                                connectionHandler.handleIntentionalDisconnectOrAfterTimeOut(session.getId()));
                    } catch (UserNotFoundException e) {
                        System.err.println(
                                "Fehler beim Verarbeiten der Zeitüberschreitung für die Verbindung: " + e.getMessage());
                    } catch (JsonProcessingException e) {
                        System.err.println(
                                "Fehler beim Verarbeiten der Zeitüberschreitung für die Verbindung: " + e.getMessage());
                    }
                });
            } else {
                eventPublisher
                        .publishEvent(connectionHandler.handleIntentionalDisconnectOrAfterTimeOut(session.getId()));
                LobbyState lobbyState = new LobbyState(playerManager.getNonNullPlayers());
                socketBroadcastService.broadcastMessage(
                        objectmapper.writeValueAsString(lobbyState));
            }
        } catch (UserNotFoundException e) {
            System.err.println(
                    "Der Benutzer mit der ID " + session.getId()
                            + " wurde nicht gefunden und wurde bereites entfernt.");
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
        log.info("Nachricht von Benutzer {} empfangen: {}", session.getId(), message.getPayload());

        try {
            messageHandler.handleClientMessage(message.getPayload().toString(), session.getId());
        } catch (ConnectionRejectedException e) {
            log.warn("Websocket-Verbindung von {} wird wieder geschlossen: {}", session.getId(), e.getMessage());
            closeSession(session);
        } catch (JsonProcessingException e) {
            log.error("Fehler beim Verarbeiten der JSON-Nachricht von Benutzer {}: {}", session.getId(),
                    e.getMessage());
        } catch (Exception e) {
            log.error("Unerwarteter Fehler beim Verarbeiten der Nachricht von Benutzer {}: {}", session.getId(),
                    e.getMessage());
        }
    }

    private void closeSession(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.POLICY_VIOLATION);
            }
        } catch (Exception e) {
            log.error("Failed to close session {}", session.getId(), e);
        }
    }

}
