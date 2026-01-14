package com.uni.gamesever.interfaces.Websocket;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.gamesever.domain.exceptions.GameAlreadyStartedException;
import com.uni.gamesever.domain.exceptions.GameFullException;
import com.uni.gamesever.domain.exceptions.UserNotFoundException;
import com.uni.gamesever.domain.exceptions.UsernameAlreadyTakenException;
import com.uni.gamesever.domain.exceptions.UsernameNullOrEmptyException;
import com.uni.gamesever.domain.game.GameManager;
import com.uni.gamesever.domain.game.PlayerManager;
import com.uni.gamesever.domain.model.PlayerInfo;
import com.uni.gamesever.domain.model.TurnState;
import com.uni.gamesever.interfaces.Websocket.messages.client.ConnectRequest;
import com.uni.gamesever.interfaces.Websocket.messages.server.ConnectAck;
import com.uni.gamesever.interfaces.Websocket.messages.server.GameStateUpdate;
import com.uni.gamesever.interfaces.Websocket.messages.server.LobbyState;
import com.uni.gamesever.interfaces.Websocket.messages.server.PlayerTurnEvent;
import com.uni.gamesever.interfaces.Websocket.messages.server.PlayerUpdateEvent;
import com.uni.gamesever.services.SocketMessageService;

@Service
public class ConnectionHandler {
    private final PlayerManager playerManager;
    private final GameManager gameManager;
    private final SocketMessageService socketMessageService;
    private final ObjectMapper objectMapper = ObjectMapperSingleton.getInstance();

    public ConnectionHandler(PlayerManager playerManager, GameManager gameManager,
            SocketMessageService socketMessageService) {
        this.playerManager = playerManager;
        this.gameManager = gameManager;
        this.socketMessageService = socketMessageService;
    }

    public boolean handleConnectMessage(ConnectRequest request, String userId)
            throws JsonProcessingException, GameFullException, IllegalArgumentException, UsernameNullOrEmptyException,
            UsernameAlreadyTakenException, UserNotFoundException, GameAlreadyStartedException {
        if (request.getUsername() == null || request.getUsername().isEmpty()) {
            throw new UsernameNullOrEmptyException("Username cannot be null or empty.");
        }
        if (request.getIdentifierToken() != null && !request.getIdentifierToken().isEmpty()) {
            if (playerManager.reconnectPlayer(request.getIdentifierToken(), userId)) {
                System.out.println("User " + userId + " reconnected as " + request.getUsername());
                ConnectAck connectionAck = new ConnectAck(userId, userId);
                socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(connectionAck));

                if (playerManager.getCurrentPlayer().getId().equals(userId)) {
                    GameStateUpdate gameStatUpdate = new GameStateUpdate(gameManager.getCurrentBoard(),
                            playerManager.getNonNullPlayerStates(),
                            gameManager.getTurnInfo(), gameManager.getGameEndTime());
                    socketMessageService.broadcastMessage(objectMapper.writeValueAsString(gameStatUpdate));

                    PlayerTurnEvent turn = new PlayerTurnEvent(playerManager.getCurrentPlayer().getId(),
                            gameManager.getCurrentBoard().getSpareTile(), 60);
                    socketMessageService.broadcastMessage(objectMapper.writeValueAsString(turn));
                }
                return true;
            }
        }
        if (gameManager.getTurnInfo().getTurnState() != TurnState.NOT_STARTED) {
            throw new GameAlreadyStartedException("Game already started. Cannot join now.");
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

    public boolean handleIntentionalDisconnectOrAfterTimeOut(String userId)
            throws IllegalArgumentException, UserNotFoundException, JsonProcessingException {
        if (userId == null || userId.isEmpty()) {
            throw new UserNotFoundException("User ID cannot be null or empty.");
        }
        if (gameManager.getTurnInfo().getTurnState() != TurnState.NOT_STARTED) {
            if (playerManager.getCurrentPlayer().getId().equals(userId)) {
                playerManager.setNextPlayerAsCurrent();
                playerManager.removePlayer(userId);
                gameManager.resetAllVariablesForNextTurn();
                GameStateUpdate gameStatUpdate = new GameStateUpdate(gameManager.getCurrentBoard(),
                        playerManager.getNonNullPlayerStates(),
                        gameManager.getTurnInfo(), gameManager.getGameEndTime());
                socketMessageService.broadcastMessage(objectMapper.writeValueAsString(gameStatUpdate));

                PlayerTurnEvent turn = new PlayerTurnEvent(playerManager.getCurrentPlayer().getId(),
                        gameManager.getCurrentBoard().getSpareTile(), 60);
                socketMessageService.broadcastMessage(objectMapper.writeValueAsString(turn));
            } else {
                playerManager.removePlayer(userId);

            }
        } else {
            playerManager.removePlayer(userId);
            LobbyState lobbyState = new LobbyState(playerManager.getNonNullPlayers());
            socketMessageService.broadcastMessage(objectMapper.writeValueAsString(lobbyState));
        }
        return true;
    }

    public boolean handleSituationWhenTheConnectionIsLost(String userId)
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
