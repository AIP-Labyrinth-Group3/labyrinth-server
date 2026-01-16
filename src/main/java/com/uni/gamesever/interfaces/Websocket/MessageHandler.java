package com.uni.gamesever.interfaces.Websocket;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.gamesever.domain.exceptions.*;
import com.uni.gamesever.domain.game.GameInitializationController;
import com.uni.gamesever.domain.game.GameManager;
import com.uni.gamesever.domain.model.ErrorCode;
import com.uni.gamesever.domain.model.TurnState;
import com.uni.gamesever.interfaces.Websocket.messages.client.ConnectRequest;
import com.uni.gamesever.interfaces.Websocket.messages.client.Message;
import com.uni.gamesever.interfaces.Websocket.messages.client.MovePawnRequest;
import com.uni.gamesever.interfaces.Websocket.messages.client.UsePushFixedTileRequest;
import com.uni.gamesever.interfaces.Websocket.messages.client.PushTileRequest;
import com.uni.gamesever.interfaces.Websocket.messages.client.StartGameRequest;
import com.uni.gamesever.interfaces.Websocket.messages.client.UseBeamRequest;
import com.uni.gamesever.interfaces.Websocket.messages.client.UseSwapRequest;
import com.uni.gamesever.interfaces.Websocket.messages.server.ActionErrorEvent;
import com.uni.gamesever.services.SocketMessageService;

@Service
public class MessageHandler {

    private final ObjectMapper objectMapper = ObjectMapperSingleton.getInstance();
    private final ConnectionHandler connectionHandler;
    private final GameInitializationController gameInitialitionController;
    private final GameManager gameManager;
    private final SocketMessageService socketMessageService;

    public MessageHandler(SocketMessageService socketMessageService,
            GameInitializationController gameInitialitionController, ConnectionHandler connectionHandler,
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
            ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.INVALID_COMMAND,
                    "Ungültiges Nachrichtenformat");
            socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
            return false;
        }

        if (request.getType() == null || request.getType().isBlank()) {
            ActionErrorEvent errorEvent = new ActionErrorEvent(
                    ErrorCode.INVALID_COMMAND,
                    "Missing message type");
            socketMessageService.sendMessageToSession(
                    userId,
                    objectMapper.writeValueAsString(errorEvent));
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
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.LOBBY_FULL, "Spiel ist bereits voll!");
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (UsernameNullOrEmptyException e) {
                    System.err.println(e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.INVALID_COMMAND,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (IllegalArgumentException e) {
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
                } catch (GameAlreadyStartedException e) {
                    System.err.println(e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.GAME_ALREADY_STARTED,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (UserNotFoundException e) {
                    System.err.println(e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.PLAYER_NOT_FOUND,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (JsonProcessingException e) {
                    System.err.println(
                            "Failed to process connect request from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.INVALID_COMMAND,
                            "Ungültiges Nachrichtenformat");
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                }
            case "DISCONNECT":
                try {
                    return connectionHandler.handleIntentionalDisconnectOrAfterTimeOut(userId);
                } catch (UserNotFoundException e) {
                    System.err.println(e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.PLAYER_NOT_FOUND,
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
                            "Ungültiges Nachrichtenformat");
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                }
            case "START_GAME":
                try {
                    StartGameRequest startGameReq = objectMapper.readValue(message, StartGameRequest.class);
                    if (startGameReq.getTreasureCardCount() < 2 || startGameReq.getTreasureCardCount() > 24) {
                        throw new IllegalArgumentException(
                                "Die Anzahl der Schatzkarten muss zwischen 2 und 24 liegen.");
                    }
                    if (startGameReq.getGameDurationInSeconds() < 0) {
                        throw new IllegalArgumentException("Die Spieldauer muss positiv sein.");
                    }
                    if (startGameReq.getTotalBonusCount() < 0 || startGameReq.getTotalBonusCount() > 20) {
                        throw new IllegalArgumentException(
                                "Die Anzahl der Bonusse darf nicht negativ oder größer als 20 sein.");
                    }
                    System.out.println("Start game request: " + startGameReq.getBoardSize().getRows() + "x"
                            + startGameReq.getBoardSize().getCols() + ", Treasures: "
                            + startGameReq.getTreasureCardCount() + ", Duration: "
                            + startGameReq.getGameDurationInSeconds() + "s, Bonuses: "
                            + startGameReq.getTotalBonusCount());
                    return gameInitialitionController.handleStartGameMessage(userId, startGameReq.getBoardSize(),
                            startGameReq.getTreasureCardCount(), startGameReq.getGameDurationInSeconds(),
                            startGameReq.getTotalBonusCount());
                } catch (GameAlreadyStartedException e) {
                    System.err.println(e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.GAME_ALREADY_STARTED,
                            "Das Spiel hat bereits begonnen.");
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (PlayerNotAdminException e) {
                    System.err.println(e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.NOT_ADMIN,
                            "Nur der Admin-Spieler kann das Spiel starten.");
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (NotEnoughPlayerException e) {
                    System.err.println(e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.GENERAL,
                            "Nicht genügend Spieler, um das Spiel zu starten.");
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (NoExtraTileException e) {
                    System.err.println(e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.GENERAL,
                            "Es gab ein Problem mit der Extra-Kachel");
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid start game request from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.INVALID_COMMAND,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (JsonProcessingException e) {
                    System.err.println(
                            "Failed to process start game request from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.INVALID_COMMAND,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (Exception e) {
                    System.err.println(
                            "Unexpected error processing start game request from user " + userId + ": "
                                    + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.GENERAL,
                            "Ein unerwarteter Fehler ist aufgetreten.");
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                }

            case "PUSH_TILE":
                if (gameManager.getTurnInfo().getTurnState() == null
                        || gameManager.getTurnInfo().getTurnState() == TurnState.NOT_STARTED) {
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.GENERAL,
                            "Das Spiel hat noch nicht begonnen.");
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                }
                try {
                    PushTileRequest pushTileCommand = objectMapper.readValue(message, PushTileRequest.class);
                    if (pushTileCommand.getRowOrColIndex() < 0) {
                        throw new IllegalArgumentException(
                                "Keine gültige Zeilen- oder Spaltenindex für Push angegeben");
                    }
                    if (pushTileCommand.getDirection() == null) {
                        throw new IllegalArgumentException("Keine gültige Richtung für Push angegeben");
                    }
                    return gameManager.handlePushTile(pushTileCommand.getRowOrColIndex(),
                            pushTileCommand.getDirection(), userId, false);
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
                            "Ungültiges Nachrichtenformat");
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                }
            case "ROTATE_TILE":
                if (gameManager.getTurnInfo().getTurnState() == null
                        || gameManager.getTurnInfo().getTurnState() == TurnState.NOT_STARTED) {
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.GENERAL,
                            "Das Spiel hat noch nicht begonnen.");
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                }
                try {
                    return gameManager.handleRotateTile(userId);
                } catch (NotPlayersRotateTileExeption e) {
                    System.err.println("Invalid rotate tile command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.INVALID_COMMAND,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (NotPlayersTurnException e) {
                    System.err.println("Invalid rotate tile command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.NOT_YOUR_TURN,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (GameNotValidException e) {
                    System.err.println("Invalid rotate tile command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.GENERAL,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (NoValidActionException e) {
                    System.err.println("Invalid rotate tile command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.INVALID_COMMAND,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid rotate tile command from user " + userId + ": " + e.getMessage());
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
            case "MOVE_PAWN":
                if (gameManager.getTurnInfo().getTurnState() == null
                        || gameManager.getTurnInfo().getTurnState() == TurnState.NOT_STARTED) {
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.GENERAL,
                            "Das Spiel hat noch nicht begonnen.");
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                }
                try {
                    MovePawnRequest movePawnRequest = objectMapper.readValue(message, MovePawnRequest.class);
                    if (movePawnRequest.getTargetCoordinates().getColumn() < 0) {
                        throw new IllegalArgumentException("Die Anzahl der Spalten darf nicht negativ sein");
                    }
                    if (movePawnRequest.getTargetCoordinates().getRow() < 0) {
                        throw new IllegalArgumentException("Die Anzahl der Reihen darf nicht negativ sein");
                    }
                    return gameManager.handleMovePawn(movePawnRequest.getTargetCoordinates(), userId, false);
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
            case "USE_BEAM":
                if (gameManager.getTurnInfo().getTurnState() == null
                        || gameManager.getTurnInfo().getTurnState() == TurnState.NOT_STARTED) {
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.GENERAL,
                            "Das Spiel hat noch nicht begonnen.");
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                }
                try {
                    UseBeamRequest useBeamCommand = objectMapper.readValue(message, UseBeamRequest.class);
                    if (useBeamCommand.getTargetCoordinates().getColumn() < 0) {
                        throw new IllegalArgumentException("Die Anzahl der Spalten darf nicht negativ sein");
                    }
                    if (useBeamCommand.getTargetCoordinates().getRow() < 0) {
                        throw new IllegalArgumentException("Die Anzahl der Reihen darf nicht negativ sein");
                    }
                    return gameManager.handleUseBeam(useBeamCommand.getTargetCoordinates(), userId);
                } catch (TargetCoordinateNullException e) {
                    System.err.println("Invalid use beam command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.INVALID_COMMAND,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (NotPlayersTurnException e) {
                    System.err.println("Invalid use beam command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.NOT_YOUR_TURN,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (GameNotValidException e) {
                    System.err.println("Invalid use beam command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.GENERAL,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (NoValidActionException e) {
                    System.err.println("Invalid use beam command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.INVALID_COMMAND,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (BonusNotAvailable e) {
                    System.err.println("Invalid use beam command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.BONUS_NOT_AVAILABLE,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid use beam command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.INVALID_COMMAND,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (JsonMappingException e) {
                    System.err.println("Failed to map use beam command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.INVALID_COMMAND,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                }
            case "USE_SWAP":
                if (gameManager.getTurnInfo().getTurnState() == null
                        || gameManager.getTurnInfo().getTurnState() == TurnState.NOT_STARTED) {
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.GENERAL,
                            "Das Spiel hat noch nicht begonnen.");
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                }
                try {
                    UseSwapRequest useSwapCommand = objectMapper.readValue(message, UseSwapRequest.class);
                    if (useSwapCommand.getTargetPlayerId() == null || useSwapCommand.getTargetPlayerId().isBlank()) {
                        throw new IllegalArgumentException("Keine gültige Zielspieler-ID für Swap angegeben");
                    }
                    return gameManager.handleUseSwap(useSwapCommand.getTargetPlayerId(), userId);
                } catch (NotPlayersTurnException e) {
                    System.err.println("Invalid use swap command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.NOT_YOUR_TURN,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (NoValidActionException e) {
                    System.err.println("Invalid use swap command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.INVALID_COMMAND,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (BonusNotAvailable e) {
                    System.err.println("Invalid use swap command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.BONUS_NOT_AVAILABLE,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (GameNotValidException e) {
                    System.err.println("Invalid use swap command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.GENERAL,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid use swap command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.INVALID_COMMAND,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (JsonMappingException e) {
                    System.err.println("Failed to map use swap command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.INVALID_COMMAND,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                }
            case "USE_PUSH_FIXED":
                if (gameManager.getTurnInfo().getTurnState() == null
                        || gameManager.getTurnInfo().getTurnState() == TurnState.NOT_STARTED) {
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.GENERAL,
                            "Das Spiel hat noch nicht begonnen.");
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                }
                try {
                    UsePushFixedTileRequest pushFixedTileCommand = objectMapper.readValue(message,
                            UsePushFixedTileRequest.class);
                    if (pushFixedTileCommand.getRowOrColIndex() < 0) {
                        throw new IllegalArgumentException(
                                "Keine gültige Zeilen- oder Spaltenindex für Push angegeben");
                    }
                    if (pushFixedTileCommand.getDirection() == null) {
                        throw new IllegalArgumentException("Keine gültige Richtung für Push angegeben");
                    }
                    return gameManager.handleUsePushFixedTile(pushFixedTileCommand.getDirection(),
                            pushFixedTileCommand.getRowOrColIndex(), userId);
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
                } catch (GameNotValidException e) {
                    System.err.println("Invalid push tile command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.GENERAL,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (NoValidActionException e) {
                    System.err.println("Invalid push tile command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.INVALID_COMMAND,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (BonusNotAvailable e) {
                    System.err.println("Invalid push tile command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.BONUS_NOT_AVAILABLE,
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
                            "Ungültiges Nachrichtenformat");
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                }
            case "USE_PUSH_TWICE":
                if (gameManager.getTurnInfo().getTurnState() == null
                        || gameManager.getTurnInfo().getTurnState() == TurnState.NOT_STARTED) {
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.GENERAL,
                            "Das Spiel hat noch nicht begonnen.");
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                }
                try {
                    return gameManager.handleUsePushTwice(userId);
                } catch (NotPlayersTurnException e) {
                    System.err.println("Invalid use push twice command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.NOT_YOUR_TURN,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (BonusNotAvailable e) {
                    System.err.println("Invalid use push twice command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.BONUS_NOT_AVAILABLE,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                } catch (GameNotValidException e) {
                    System.err.println("Invalid use push twice command from user " + userId + ": " + e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.GENERAL,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return false;
                }
            default:
                return false;
        }
    }

}
