package com.uni.gamesever.interfaces.Websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.uni.gamesever.interfaces.Websocket.messages.server.NextTreasureCardEvent;
import com.uni.gamesever.interfaces.Websocket.messages.server.PlayerTurnEvent;
import com.uni.gamesever.interfaces.Websocket.messages.server.PlayerUpdateEvent;
import com.uni.gamesever.services.SocketMessageService;

@Service
public class ConnectionHandler {
    private final PlayerManager playerManager;
    private final GameManager gameManager;
    private final SocketMessageService socketMessageService;
    private final ObjectMapper objectMapper = ObjectMapperSingleton.getInstance();
    private static final Logger log = LoggerFactory.getLogger("GAME_LOG");

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
            throw new UsernameNullOrEmptyException("Der Benutzername darf nicht leer sein.");
        }
        if (request.getIdentifierToken() != null && !request.getIdentifierToken().isEmpty()) {
            if (playerManager.reconnectPlayer(request.getIdentifierToken(), userId)) {
                System.out.println("User " + userId + " reconnected as " + request.getUsername());
                log.info("User {} hat sich als {} wiederverbunden", userId, request.getUsername());
                ConnectAck connectionAck = new ConnectAck(userId, userId);
                socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(connectionAck));
                NextTreasureCardEvent nextTreasureCardEvent = new NextTreasureCardEvent(
                        playerManager.getPlayerStateById(userId).getCurrentTreasure());
                socketMessageService.sendMessageToSession(userId,
                        objectMapper.writeValueAsString(nextTreasureCardEvent));
                GameStateUpdate gameStateUpdate = new GameStateUpdate(gameManager.getCurrentBoard(),
                        playerManager.getNonNullPlayerStates(),
                        gameManager.getTurnInfo(), gameManager.getGameEndTime());
                socketMessageService.broadcastMessage(objectMapper.writeValueAsString(gameStateUpdate));
                return true;
            }
        }
        if (gameManager.getTurnInfo().getTurnState() != TurnState.NOT_STARTED) {
            throw new GameAlreadyStartedException(
                    "Das Spiel hat bereits begonnen. Ein Beitritt ist nicht mehr möglich.");
        }
        PlayerInfo newPlayer = new PlayerInfo(userId);
        newPlayer.setName(request.getUsername());
        if (playerManager.addPlayer(newPlayer)) {
            System.out.println("User " + userId + " connected as " + request.getUsername());
            log.info("User {} hat sich als {} verbunden", userId, request.getUsername());
            ConnectAck connectionAck = new ConnectAck(newPlayer.getId(), newPlayer.getId());
            socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(connectionAck));

            LobbyState lobbyState = new LobbyState(playerManager.getNonNullPlayers());
            socketMessageService.broadcastMessage(objectMapper.writeValueAsString(lobbyState));
        } else {
            System.err.println("Game is full. User " + userId + " cannot join.");
            throw new GameFullException("Das Spiel ist voll. Beitritt nicht möglich.");
        }

        return true;
    }

    public boolean handleIntentionalDisconnectOrAfterTimeOut(String userId)
            throws UserNotFoundException, JsonProcessingException {
        if (userId == null || userId.isEmpty()) {
            throw new UserNotFoundException("Die Benutzer-ID darf nicht null oder leer sein.");
        }
        if (gameManager.getTurnInfo().getTurnState() != TurnState.NOT_STARTED) {
            if (playerManager.getCurrentPlayer().getId().equals(userId)) {
                playerManager.setNextPlayerAsCurrent();
                playerManager.removePlayer(userId);
                if (playerManager.getNonNullPlayers().length == 0) {
                    gameManager.endGameByTimeoutOrAfterCollectingAllTreasures();
                    return true;
                }
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
            throw new UserNotFoundException("Die Benutzer-ID darf nicht null oder leer sein.");
        }
        playerManager.disconnectPlayer(userId);
        PlayerUpdateEvent playerUpdateEvent = new PlayerUpdateEvent(
                playerManager.getPlayerById(userId));
        socketMessageService.broadcastMessage(objectMapper.writeValueAsString(playerUpdateEvent));

        return true;
    }

}
