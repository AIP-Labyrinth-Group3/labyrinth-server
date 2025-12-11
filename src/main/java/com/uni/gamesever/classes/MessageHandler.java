package com.uni.gamesever.classes;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.gamesever.exceptions.GameAlreadyStartedException;
import com.uni.gamesever.exceptions.GameNotValidException;
import com.uni.gamesever.exceptions.NoExtraTileException;
import com.uni.gamesever.exceptions.NoValidActionException;
import com.uni.gamesever.exceptions.NotEnoughPlayerException;
import com.uni.gamesever.exceptions.NotPlayersTurnException;
import com.uni.gamesever.exceptions.PlayerNotAdminException;
import com.uni.gamesever.exceptions.PushNotValidException;
import com.uni.gamesever.models.messages.ConnectRequest;
import com.uni.gamesever.models.messages.Message;
import com.uni.gamesever.models.messages.MovePawnRequest;
import com.uni.gamesever.models.messages.PushTileCommand;
import com.uni.gamesever.models.messages.StartGameAction;
import com.uni.gamesever.services.SocketMessageService;

@Service
public class MessageHandler {

    private final ObjectMapper objectMapper = ObjectMapperSingleton.getInstance();
    private final ConnectionHandler connectionHandler;
    private final GameInitialitionController gameInitialitionController;
    private final GameManager gameManager;

    public MessageHandler(SocketMessageService socketBroadcastService,
            GameInitialitionController gameInitialitionController, ConnectionHandler connectionHandler,
            GameManager gameManager) {
        this.connectionHandler = connectionHandler;
        this.gameInitialitionController = gameInitialitionController;
        this.gameManager = gameManager;
    }

    public boolean handleClientMessage(String message, String userId)
            throws JsonMappingException, JsonProcessingException {
        // parsing the client message into a connectRequest object
        System.out.println("Received message from user " + userId + ": " + message);

        Message request;
        try {
            request = objectMapper.readValue(message, Message.class);
        } catch (JsonMappingException e) {
            System.err.println("Failed to parse message from user " + userId + ": " + e.getMessage());
            return false;
        } catch (JsonProcessingException e) {
            System.err.println("Failed to process message from user " + userId + ": " + e.getMessage());
            return false;
        }

        switch (request.getType()) {
            // connect action from client
            case "CONNECT":
                try {
                    ConnectRequest connectReq = objectMapper.readValue(message, ConnectRequest.class);
                    return connectionHandler.handleConnectMessage(connectReq, userId);
                } catch (JsonProcessingException e) {
                    System.err.println(
                            "Failed to process connect request from user " + userId + ": " + e.getMessage());
                    return false;
                }
            case "DISCONNECT":
                try {
                    ConnectRequest disconnectRequest = objectMapper.readValue(message, ConnectRequest.class);
                    return connectionHandler.handleDisconnectRequest(disconnectRequest, userId);
                } catch (JsonProcessingException e) {
                    System.err.println(
                            "Failed to process disconnect request from user " + userId + ": " + e.getMessage());
                    return false;
                }
            case "START_GAME":
                try {
                    StartGameAction startGameReq = objectMapper.readValue(message, StartGameAction.class);
                    return gameInitialitionController.handleStartGameMessage(userId, startGameReq.getBoardSize());
                } catch (PlayerNotAdminException e) {
                    System.err.println(e.getMessage());
                    return false;
                } catch (NotEnoughPlayerException e) {
                    System.err.println(e.getMessage());
                    return false;
                } catch (NoExtraTileException e) {
                    System.err.println(e.getMessage());
                    return false;
                } catch (GameAlreadyStartedException e) {
                    System.err.println(e.getMessage());
                    return false;
                } catch (JsonProcessingException e) {
                    System.err.println(
                            "Failed to process start game request from user " + userId + ": " + e.getMessage());
                    return false;
                }

            case "PUSH_TILE":
                try {
                    PushTileCommand pushTileCommand = objectMapper.readValue(message, PushTileCommand.class);
                    return gameManager.handlePushTile(pushTileCommand.getRowOrColIndex(),
                            pushTileCommand.getDirection(), userId);
                } catch (PushNotValidException e) {
                    System.err.println("Invalid push tile command from user " + userId + ": " + e.getMessage());
                    return false;
                } catch (NotPlayersTurnException e) {
                    System.err.println("Invalid push tile command from user " + userId + ": " + e.getMessage());
                    return false;
                } catch (GameNotValidException e) {
                    System.err.println("Invalid push tile command from user " + userId + ": " + e.getMessage());
                    return false;
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid push tile command from user " + userId + ": " + e.getMessage());
                    return false;
                } catch (JsonMappingException e) {
                    System.err.println("Failed to map push tile command from user " + userId + ": " + e.getMessage());
                    return false;
                }
            case "MOVE_PAWN":
                try {
                    MovePawnRequest movePawnRequest = objectMapper.readValue(message, MovePawnRequest.class);
                    return gameManager.handleMovePawn(movePawnRequest.getTargetCoordinates(), userId);
                } catch (NotPlayersTurnException e) {
                    System.err.println("Invalid move pawn command from user " + userId + ": " + e.getMessage());
                    return false;
                } catch (GameNotValidException e) {
                    System.err.println("Invalid move pawn command from user " + userId + ": " + e.getMessage());
                    return false;
                } catch (NoValidActionException e) {
                    System.err.println("Invalid move pawn command from user " + userId + ": " + e.getMessage());
                    return false;
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid move pawn command from user " + userId + ": " + e.getMessage());
                    return false;
                } catch (JsonMappingException e) {
                    System.err.println("Failed to map move pawn command from user " + userId + ": " + e.getMessage());
                    return false;
                }
            default:
                return false;
        }
    }

}
