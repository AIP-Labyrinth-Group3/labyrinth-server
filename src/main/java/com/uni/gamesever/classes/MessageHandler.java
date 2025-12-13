package com.uni.gamesever.classes;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.gamesever.exceptions.GameAlreadyStartedException;
import com.uni.gamesever.exceptions.GameFullException;
import com.uni.gamesever.exceptions.GameNotStartedException;
import com.uni.gamesever.exceptions.GameNotValidException;
import com.uni.gamesever.exceptions.NoDirectionForPush;
import com.uni.gamesever.exceptions.NoExtraTileException;
import com.uni.gamesever.exceptions.NoValidActionException;
import com.uni.gamesever.exceptions.NotEnoughPlayerException;
import com.uni.gamesever.exceptions.NotPlayersTurnException;
import com.uni.gamesever.exceptions.PlayerNotAdminException;
import com.uni.gamesever.exceptions.PushNotValidException;
import com.uni.gamesever.exceptions.TargetCoordinateNullException;
import com.uni.gamesever.exceptions.UserNotFoundException;
import com.uni.gamesever.exceptions.UsernameAlreadyTakenException;
import com.uni.gamesever.exceptions.UsernameNullOrEmptyException;
import com.uni.gamesever.models.ActionErrorEvent;
import com.uni.gamesever.models.ErrorCode;
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
    private final SocketMessageService socketMessageService;

    public MessageHandler(SocketMessageService socketMessageService,
            GameInitialitionController gameInitialitionController, ConnectionHandler connectionHandler,
            GameManager gameManager) {
        this.socketMessageService = socketMessageService;
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
        } catch (JsonProcessingException e) {
            System.err.println("Failed to parse message from user " + userId + ": " + e.getMessage());
            ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.INVALID_COMMAND, "Invalid command format");
            socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
            return false;
        }
        switch (request.getType()) {
            // connect action from client
            case "CONNECT":
                try {
                    ConnectRequest connectReq = objectMapper.readValue(message, ConnectRequest.class);
                    return connectionHandler.handleConnectMessage(connectReq, userId);
                } catch (GameFullException e) {
                    System.err.println(e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.LOBBY_FULL, "Game is full");
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (UsernameNullOrEmptyException e) {
                    System.err.println(e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.INVALID_COMMAND,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (UsernameAlreadyTakenException e) {
                    System.err.println(e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.USERNAME_TAKEN,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (JsonProcessingException e) {
                    System.err.println(
                            "Failed to process connect request from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.INVALID_COMMAND,
                            "Invalid command format");
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                }
            case "DISCONNECT":
                try {
                    ConnectRequest disconnectRequest = objectMapper.readValue(message, ConnectRequest.class);
                    return connectionHandler.handleDisconnectRequest(disconnectRequest, userId);
                } catch (UserNotFoundException e) {
                    System.err.println(e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.GENERAL,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (IllegalArgumentException e) {
                    System.err.println(e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.GENERAL,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (JsonProcessingException e) {
                    System.err.println(
                            "Failed to process disconnect request from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.INVALID_COMMAND,
                            "Invalid command format");
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                }
            case "START_GAME":
                try {
                    StartGameAction startGameReq = objectMapper.readValue(message, StartGameAction.class);
                    return gameInitialitionController.handleStartGameMessage(userId, startGameReq.getBoardSize(),
                            startGameReq.getTreasureCardCount());
                } catch (GameAlreadyStartedException e) {
                    System.err.println(e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.GAME_ALREADY_STARTED,
                            "The game has already started");
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (PlayerNotAdminException e) {
                    System.err.println(e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.NOT_ADMIN,
                            "Only the admin player can start the game");
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (NotEnoughPlayerException e) {
                    System.err.println(e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.GENERAL,
                            "Not enough players to start the game");
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (NoExtraTileException e) {
                    System.err.println(e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.GENERAL,
                            "There was a problem with the extra tile");
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (JsonProcessingException e) {
                    System.err.println(
                            "Failed to process start game request from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.INVALID_COMMAND,
                            "Invalid command format");
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                }

            case "PUSH_TILE":
                try {
                    PushTileCommand pushTileCommand = objectMapper.readValue(message, PushTileCommand.class);
                    return gameManager.handlePushTile(pushTileCommand.getRowOrColIndex(),
                            pushTileCommand.getDirection(), userId);
                } catch (PushNotValidException e) {
                    System.err.println("Invalid push tile command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.INVALID_PUSH,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (NoDirectionForPush e) {
                    System.err.println("Invalid push tile command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.INVALID_COMMAND,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (NotPlayersTurnException e) {
                    System.err.println("Invalid push tile command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.NOT_YOUR_TURN,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (NoExtraTileException e) {
                    System.err.println("Invalid push tile command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.GENERAL,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (GameNotStartedException e) {
                    System.err.println("Invalid push tile command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.GENERAL,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid push tile command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.INVALID_COMMAND,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (JsonMappingException e) {
                    System.err.println("Failed to map push tile command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.INVALID_COMMAND,
                            "Invalid command format");
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                }
            case "MOVE_PAWN":
                try {
                    MovePawnRequest movePawnRequest = objectMapper.readValue(message, MovePawnRequest.class);
                    return gameManager.handleMovePawn(movePawnRequest.getTargetCoordinates(), userId);
                } catch (TargetCoordinateNullException e) {
                    System.err.println("Invalid move pawn command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.INVALID_COMMAND,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (NotPlayersTurnException e) {
                    System.err.println("Invalid move pawn command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.NOT_YOUR_TURN,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (GameNotValidException e) {
                    System.err.println("Invalid move pawn command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.GENERAL,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (NoValidActionException e) {
                    System.err.println("Invalid move pawn command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.INVALID_MOVE,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid move pawn command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.INVALID_COMMAND,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (JsonMappingException e) {
                    System.err.println("Failed to map move pawn command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.INVALID_COMMAND,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                }
            default:
                return false;
        }
    }

}
