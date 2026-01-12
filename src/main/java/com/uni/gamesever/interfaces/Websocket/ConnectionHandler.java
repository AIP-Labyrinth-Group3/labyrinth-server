package com.uni.gamesever.interfaces.Websocket;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.gamesever.domain.exceptions.GameFullException;
import com.uni.gamesever.domain.exceptions.UserNotFoundException;
import com.uni.gamesever.domain.exceptions.UsernameAlreadyTakenException;
import com.uni.gamesever.domain.exceptions.UsernameNullOrEmptyException;
import com.uni.gamesever.domain.game.PlayerManager;
import com.uni.gamesever.domain.model.PlayerInfo;
import com.uni.gamesever.interfaces.Websocket.messages.client.ConnectRequest;
import com.uni.gamesever.interfaces.Websocket.messages.server.ConnectAck;
import com.uni.gamesever.interfaces.Websocket.messages.server.LobbyState;
import com.uni.gamesever.interfaces.Websocket.messages.server.PlayerUpdateEvent;
import com.uni.gamesever.services.SocketMessageService;

@Service
public class ConnectionHandler {
    private final PlayerManager playerManager;
    private final SocketMessageService socketMessageService;
    private final ObjectMapper objectMapper = ObjectMapperSingleton.getInstance();

    public ConnectionHandler(PlayerManager playerManager, SocketMessageService socketMessageService) {
        this.playerManager = playerManager;
        this.socketMessageService = socketMessageService;
    }

    public boolean handleConnectMessage(ConnectRequest request, String userId)
            throws JsonProcessingException, GameFullException, IllegalArgumentException, UsernameNullOrEmptyException,
            UsernameAlreadyTakenException, UserNotFoundException {
        if (request.getUsername() == null || request.getUsername().isEmpty()) {
            throw new UsernameNullOrEmptyException("Username cannot be null or empty.");
        }
        if (request.getIdentifierToken() != null && !request.getIdentifierToken().isEmpty()) {
            if (playerManager.reconnectPlayer(request.getIdentifierToken(), userId)) {
                System.out.println("User " + userId + " reconnected as " + request.getUsername());
                ConnectAck connectionAck = new ConnectAck(userId, userId);
                socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(connectionAck));

                PlayerUpdateEvent playerUpdateEvent = new PlayerUpdateEvent(
                        playerManager.getPlayerById(userId));
                socketMessageService.broadcastMessage(objectMapper.writeValueAsString(playerUpdateEvent));
                return true;
            }
        }
        PlayerInfo newPlayer = new PlayerInfo(userId);
        newPlayer.setName(request.getUsername());
        if (playerManager.addPlayer(newPlayer)) {
            System.out.println("User " + userId + " connected as " + request.getUsername());
            ConnectAck connectionAck = new ConnectAck(newPlayer.getId(), newPlayer.getId());
            socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(connectionAck));

            LobbyState lobbyState = new LobbyState(playerManager.getNonNullPlayers());
            socketMessageService.broadcastMessage(objectMapper.writeValueAsString(lobbyState));
        } else {
            System.err.println("Game is full. User " + userId + " cannot join.");
            throw new GameFullException("Game is full");
        }

        return true;
    }

    public boolean handleDisconnectRequest(ConnectRequest request, String userId)
            throws IllegalArgumentException, UserNotFoundException, JsonProcessingException {
        if (userId == null || userId.isEmpty()) {
            throw new UserNotFoundException("User ID cannot be null or empty.");
        }
        if (playerManager.removePlayer(userId)) {
            System.out.println("User " + userId + " disconnected " + request.getUsername());
            LobbyState lobbyState = new LobbyState(playerManager.getNonNullPlayers());
            socketMessageService.broadcastMessage(objectMapper.writeValueAsString(lobbyState));
        }
        return true;
    }

    public boolean handleSituationWhereTheConnectionIsLost(String userId)
            throws IllegalArgumentException, UserNotFoundException, JsonProcessingException {
        if (userId == null || userId.isEmpty()) {
            throw new UserNotFoundException("User ID cannot be null or empty.");
        }
        playerManager.disconnectPlayer(userId);
        PlayerUpdateEvent playerUpdateEvent = new PlayerUpdateEvent(
                playerManager.getPlayerById(userId));
        socketMessageService.broadcastMessage(objectMapper.writeValueAsString(playerUpdateEvent));

        return true;
    }

}
