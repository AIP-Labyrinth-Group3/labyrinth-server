package com.uni.gamesever.interfaces.Websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger("GAME_LOG");

    public MessageHandler(SocketMessageService socketMessageService,
            GameInitializationController gameInitialitionController, ConnectionHandler connectionHandler,
            GameManager gameManager) {
        this.socketMessageService = socketMessageService;
        this.connectionHandler = connectionHandler;
        this.gameInitialitionController = gameInitialitionController;
        this.gameManager = gameManager;
    }

    public void handleClientMessage(String message, String userId)
            throws ConnectionRejectedException, JsonProcessingException {
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
            return;
        }

        if (request.getType() == null || request.getType().isBlank()) {
            ActionErrorEvent errorEvent = new ActionErrorEvent(
                    ErrorCode.INVALID_COMMAND,
                    "Missing message type");
            socketMessageService.sendMessageToSession(
                    userId,
                    objectMapper.writeValueAsString(errorEvent));
            return;
        }

        switch (request.getType()) {
            case "CONNECT":
                try {
                    ConnectRequest connectReq = objectMapper.readValue(message, ConnectRequest.class);
                    connectionHandler.handleConnectMessage(connectReq, userId);
                    return;
                } catch (GameFullException e) {
                    System.err.println(e.getMessage());
                    sendError(userId, ErrorCode.LOBBY_FULL,
                            "Das Spiel ist bereits voll. Es kann keine weitere Person mehr beitreten!");
                    log.error("Spiel ist bereits voll, Verbindung von Benutzer {} abgelehnt", userId);
                    throw new ConnectionRejectedException("Lobby voll");
                } catch (UsernameNullOrEmptyException e) {
                    System.err.println(e.getMessage());
                    sendError(userId, ErrorCode.INVALID_COMMAND,
                            e.getMessage());
                    log.error("Ungültiger Benutzername von Benutzer {}: {}", userId, e.getMessage());
                    throw new ConnectionRejectedException("Ungültiger Benutzername");
                } catch (IllegalArgumentException e) {
                    System.err.println(e.getMessage());
                    sendError(userId, ErrorCode.INVALID_COMMAND,
                            e.getMessage());
                    log.error("Der Benutzername von Benutzer {} ist ungültig: {}", userId, e.getMessage());
                    throw new ConnectionRejectedException("Ungültiger Benutzername");
                } catch (UsernameAlreadyTakenException e) {
                    System.err.println(e.getMessage());
                    sendError(userId, ErrorCode.USERNAME_TAKEN,
                            e.getMessage());
                    log.error("Der Benutzername von Benutzer {} ist bereits vergeben: {}", userId, e.getMessage());
                    throw new ConnectionRejectedException("Benutzername bereits vergeben");
                } catch (GameAlreadyStartedException e) {
                    System.err.println(e.getMessage());
                    sendError(userId, ErrorCode.GAME_ALREADY_STARTED,
                            e.getMessage());
                    log.error("Das Spiel wurde bereits gestartet, Benutzer {} kann nicht beitreten: {}", userId,
                            e.getMessage());
                    throw new ConnectionRejectedException("Spiel bereits gestartet");
                } catch (UserNotFoundException e) {
                    System.err.println(e.getMessage());
                    sendError(userId, ErrorCode.PLAYER_NOT_FOUND,
                            e.getMessage());
                    log.error("Benutzer {} nicht gefunden: {}", userId, e.getMessage());
                    throw new ConnectionRejectedException("Benutzer nicht gefunden");
                } catch (JsonProcessingException e) {
                    System.err.println(
                            "Failed to process connect request from user " + userId + ": " + e.getMessage());
                    sendError(userId, ErrorCode.INVALID_COMMAND,
                            "Ungültiges Nachrichtenformat");
                    log.error("Fehler beim Verarbeiten der Verbindungsanfrage von Benutzer {}: {}", userId,
                            e.getMessage());
                    throw new ConnectionRejectedException("Ungültiges Nachrichtenformat");
                }
            case "DISCONNECT":
                try {
                    connectionHandler.handleIntentionalDisconnectOrAfterTimeOut(userId);
                    return;
                } catch (UserNotFoundException e) {
                    System.err.println(e.getMessage());
                    sendError(userId, ErrorCode.PLAYER_NOT_FOUND, e.getMessage());
                    log.error("Benutzer {} nicht gefunden: {}", userId, e.getMessage());
                    return;
                } catch (JsonProcessingException e) {
                    System.err.println(
                            "Failed to process disconnect request from user " + userId + ": " + e.getMessage());
                    sendError(userId, ErrorCode.INVALID_COMMAND,
                            "Ungültiges Nachrichtenformat");
                    log.error("Fehler beim Verarbeiten der Trennungsanfrage von Benutzer {}: {}", userId,
                            e.getMessage());
                    return;
                }
            case "START_GAME":
                try {
                    StartGameRequest startGameReq = objectMapper.readValue(message, StartGameRequest.class);
                    if (startGameReq.getTreasureCardCount() < 2 || startGameReq.getTreasureCardCount() > 24) {
                        log.error("Die Anzahl der Schatzkarten ist nicht korrekt.");
                        throw new IllegalArgumentException(
                                "Die Anzahl der Schatzkarten muss zwischen 2 und 24 liegen.");
                    }
                    if (startGameReq.getGameDurationInSeconds() < 0) {
                        log.error("Die Spieldauer ist nicht korrekt.");
                        throw new IllegalArgumentException("Die Spieldauer muss positiv sein.");
                    }
                    if (startGameReq.getTotalBonusCount() < 0 || startGameReq.getTotalBonusCount() > 20) {
                        log.error("Die Anzahl der Bonusse ist nicht korrekt.");
                        throw new IllegalArgumentException(
                                "Die Anzahl der Bonusse darf nicht negativ oder größer als 20 sein.");
                    }
                    gameInitialitionController.handleStartGameMessage(userId, startGameReq.getBoardSize(),
                            startGameReq.getTreasureCardCount(), startGameReq.getGameDurationInSeconds(),
                            startGameReq.getTotalBonusCount());
                    return;
                } catch (GameAlreadyStartedException e) {
                    System.err.println(e.getMessage());
                    sendError(userId, ErrorCode.GAME_ALREADY_STARTED, "Das Spiel hat bereits begonnen.");
                    log.error(userId, "Das Spiel hat bereits begonnen: {}", e.getMessage());
                    return;
                } catch (PlayerNotAdminException e) {
                    System.err.println(e.getMessage());
                    sendError(userId, ErrorCode.NOT_ADMIN,
                            "Nur der Admin-Spieler kann das Spiel starten. Bitte warte bis zum Spielstart.");
                    log.error(userId, "Nur der Admin-Spieler kann das Spiel starten: {}", e.getMessage());
                    return;
                } catch (NotEnoughPlayerException e) {
                    System.err.println(e.getMessage());
                    sendError(userId, ErrorCode.GENERAL, "Nicht genügend Spieler, um das Spiel zu starten.");
                    log.error(userId, "Nicht genügend Spieler, um das Spiel zu starten: {}", e.getMessage());
                    return;
                } catch (NoExtraTileException e) {
                    System.err.println(e.getMessage());
                    sendError(userId, ErrorCode.GENERAL, "Es gab ein Problem mit der Extra-Kachel");
                    log.error(userId, "Es gab ein Problem mit der Extra-Kachel: {}", e.getMessage());
                    return;
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid start game request from user " + userId + ": " + e.getMessage());
                    sendError(userId, ErrorCode.INVALID_COMMAND, e.getMessage());
                    log.error(userId, "Ungültige Startspiel-Anfrage: {}", e.getMessage());
                    return;
                } catch (JsonProcessingException e) {
                    System.err.println(
                            "Failed to process start game request from user " + userId + ": " + e.getMessage());
                    sendError(userId, ErrorCode.INVALID_COMMAND, e.getMessage());
                    log.error(userId, "Fehler beim Verarbeiten der Startspiel-Anfrage von Benutzer {}: {}", userId,
                            e.getMessage());
                    return;
                } catch (Exception e) {
                    System.err.println(
                            "Unexpected error processing start game request from user " + userId + ": "
                                    + e.getMessage());
                    sendError(userId, ErrorCode.GENERAL, "Ein unerwarteter Fehler ist aufgetreten.");
                    log.error(userId,
                            "Unerwarteter Fehler bei der Verarbeitung der Startspiel-Anfrage von Benutzer {}: {}",
                            userId,
                            e.getMessage());
                    return;
                }

            case "PUSH_TILE":
                if (gameManager.getTurnInfo().getState() == null
                        || gameManager.getTurnInfo().getState() == TurnState.NOT_STARTED) {
                    sendError(userId, ErrorCode.GENERAL, "Das Spiel hat noch nicht begonnen.");
                    log.error(userId, "Das Spiel hat noch nicht begonnen. Es kann keine Kachel geschoben werden.");
                    return;
                }
                try {
                    PushTileRequest pushTileCommand = objectMapper.readValue(message, PushTileRequest.class);
                    if (pushTileCommand.getRowOrColIndex() < 0) {
                        log.error(userId, "Keine gültige Zeilen- oder Spaltenindex für Push angegeben");
                        throw new IllegalArgumentException(
                                "Keine gültige Zeilen- oder Spaltenindex für Push angegeben");
                    }
                    if (pushTileCommand.getDirection() == null) {
                        log.error(userId, "Keine gültige Richtung für Push angegeben");
                        throw new IllegalArgumentException("Keine gültige Richtung für Push angegeben");
                    }
                    gameManager.handlePushTile(pushTileCommand.getRowOrColIndex(),
                            pushTileCommand.getDirection(), userId, false);
                    return;
                } catch (PushNotValidException e) {
                    System.err.println("Invalid push tile command from user " + userId + ": " + e.getMessage());
                    log.error(userId, "Ungültiger Push-Kachel-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.INVALID_PUSH, e.getMessage());
                    return;
                } catch (NoDirectionForPush e) {
                    System.err.println("Invalid push tile command from user " + userId + ": " + e.getMessage());
                    log.error(userId, "Ungültiger Push-Kachel-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.INVALID_COMMAND, e.getMessage());
                    return;
                } catch (NotPlayersTurnException e) {
                    System.err.println("Invalid push tile command from user " + userId + ": " + e.getMessage());
                    log.error(userId, "Ungültiger Push-Kachel-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.NOT_YOUR_TURN, e.getMessage());
                    return;
                } catch (NoExtraTileException e) {
                    System.err.println("Invalid push tile command from user " + userId + ": " + e.getMessage());
                    log.error(userId, "Ungültiger Push-Kachel-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.GENERAL, e.getMessage());
                    return;
                } catch (GameNotStartedException e) {
                    System.err.println("Invalid push tile command from user " + userId + ": " + e.getMessage());
                    log.error(userId, "Ungültiger Push-Kachel-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.GENERAL, e.getMessage());
                    return;
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid push tile command from user " + userId + ": " + e.getMessage());
                    log.error(userId, "Ungültiger Push-Kachel-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.INVALID_COMMAND, e.getMessage());
                    return;
                } catch (JsonMappingException e) {
                    System.err.println("Failed to map push tile command from user " + userId + ": " + e.getMessage());
                    log.error(userId, "Fehler beim Zuordnen des Push-Kachel-Befehls von Benutzer {}: {}", userId,
                            e.getMessage());
                    sendError(userId, ErrorCode.INVALID_COMMAND,
                            "Ungültiges Nachrichtenformat");
                    return;
                }
            case "ROTATE_TILE":
                if (gameManager.getTurnInfo().getState() == null
                        || gameManager.getTurnInfo().getState() == TurnState.NOT_STARTED) {
                    sendError(userId, ErrorCode.GENERAL, "Das Spiel hat noch nicht begonnen.");
                    return;
                }
                try {
                    gameManager.handleRotateTile(userId);
                    return;
                } catch (NotPlayersRotateTileExeption e) {
                    log.error(userId, "Ungültiger Rotate-Kachel-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.INVALID_COMMAND, e.getMessage());
                    return;
                } catch (NotPlayersTurnException e) {
                    log.error(userId, "Ungültiger Rotate-Kachel-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.NOT_YOUR_TURN, e.getMessage());
                    return;
                } catch (GameNotValidException e) {
                    log.error(userId, "Ungültiger Rotate-Kachel-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.GENERAL, e.getMessage());
                    return;
                } catch (NoValidActionException e) {
                    log.error(userId, "Ungültiger Rotate-Kachel-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.INVALID_COMMAND, e.getMessage());
                    return;
                } catch (IllegalArgumentException e) {
                    log.error(userId, "Ungültiger Rotate-Kachel-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.INVALID_COMMAND, e.getMessage());
                    return;
                } catch (JsonMappingException e) {
                    log.error(userId, "Ungültiger Move-Pawn-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.INVALID_COMMAND, e.getMessage());
                    return;
                }
            case "MOVE_PAWN":
                if (gameManager.getTurnInfo().getState() == null
                        || gameManager.getTurnInfo().getState() == TurnState.NOT_STARTED) {
                    sendError(userId, ErrorCode.GENERAL, "Das Spiel hat noch nicht begonnen.");
                    return;
                }
                try {
                    MovePawnRequest movePawnRequest = objectMapper.readValue(message, MovePawnRequest.class);
                    if (movePawnRequest.getTargetCoordinates().getColumn() < 0) {
                        log.error(userId, "Die Anzahl der Spalten darf nicht negativ sein");
                        throw new IllegalArgumentException("Die Anzahl der Spalten darf nicht negativ sein");
                    }
                    if (movePawnRequest.getTargetCoordinates().getRow() < 0) {
                        log.error(userId, "Die Anzahl der Reihen darf nicht negativ sein");
                        throw new IllegalArgumentException("Die Anzahl der Reihen darf nicht negativ sein");
                    }
                    gameManager.handleMovePawn(movePawnRequest.getTargetCoordinates(), userId, false);
                    return;
                } catch (TargetCoordinateNullException e) {
                    log.error(userId, "Ungültiger Move-Pawn-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.INVALID_COMMAND, e.getMessage());
                    return;
                } catch (NotPlayersTurnException e) {
                    log.error(userId, "Ungültiger Move-Pawn-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.NOT_YOUR_TURN, e.getMessage());
                    return;
                } catch (GameNotValidException e) {
                    log.error(userId, "Ungültiger Move-Pawn-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.GENERAL, e.getMessage());
                    return;
                } catch (NoValidActionException e) {
                    log.error(userId, "Ungültiger Move-Pawn-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.INVALID_MOVE, e.getMessage());
                    return;
                } catch (IllegalArgumentException e) {
                    log.error(userId, "Ungültiger Move-Pawn-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.INVALID_COMMAND, e.getMessage());
                    return;
                } catch (JsonMappingException e) {
                    System.err.println("Failed to map move pawn command from user " + userId + ": " + e.getMessage());
                    log.error(userId, "Fehler beim Zuordnen des Move-Pawn-Befehls von Benutzer {}: {}", userId,
                            e.getMessage());
                    sendError(userId, ErrorCode.INVALID_COMMAND, e.getMessage());
                    return;
                }
            case "USE_BEAM":
                if (gameManager.getTurnInfo().getState() == null
                        || gameManager.getTurnInfo().getState() == TurnState.NOT_STARTED) {
                    sendError(userId, ErrorCode.GENERAL, "Das Spiel hat noch nicht begonnen.");
                    return;
                }
                try {
                    UseBeamRequest useBeamCommand = objectMapper.readValue(message, UseBeamRequest.class);
                    if (useBeamCommand.getTargetCoordinates().getColumn() < 0) {
                        log.error(userId, "Die Anzahl der Spalten darf nicht negativ sein.");
                        throw new IllegalArgumentException("Die Anzahl der Spalten darf nicht negativ sein");
                    }
                    if (useBeamCommand.getTargetCoordinates().getRow() < 0) {
                        log.error(userId, "Die Anzahl der Reihen darf nicht negativ sein.");
                        throw new IllegalArgumentException("Die Anzahl der Reihen darf nicht negativ sein");
                    }
                    gameManager.handleUseBeam(useBeamCommand.getTargetCoordinates(), userId);
                    return;
                } catch (TargetCoordinateNullException e) {
                    log.error(userId, "Ungültiger Use-Beam-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.INVALID_COMMAND,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return;
                } catch (NotPlayersTurnException e) {
                    log.error(userId, "Ungültiger Use-Beam-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    ActionErrorEvent errorEvent = new ActionErrorEvent(ErrorCode.NOT_YOUR_TURN,
                            e.getMessage());
                    socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
                    return;
                } catch (GameNotValidException e) {
                    log.error(userId, "Ungültiger Use-Beam-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.GENERAL, e.getMessage());
                    return;
                } catch (NoValidActionException e) {
                    log.error(userId, "Ungültiger Use-Beam-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.INVALID_COMMAND, e.getMessage());
                    return;
                } catch (BonusNotAvailable e) {
                    log.error(userId, "Ungültiger Use-Beam-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.BONUS_NOT_AVAILABLE, e.getMessage());
                    e.getMessage();
                    return;
                } catch (IllegalArgumentException e) {
                    log.error(userId, "Ungültiger Use-Beam-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.INVALID_COMMAND, e.getMessage());
                    return;
                } catch (JsonMappingException e) {
                    log.error(userId, "Ungültiger Use-Beam-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.INVALID_COMMAND, e.getMessage());
                    e.getMessage();
                    return;
                }
            case "USE_SWAP":
                if (gameManager.getTurnInfo().getState() == null
                        || gameManager.getTurnInfo().getState() == TurnState.NOT_STARTED) {
                    sendError(userId, ErrorCode.GENERAL, "Das Spiel hat noch nicht begonnen.");
                    return;
                }
                try {
                    UseSwapRequest useSwapCommand = objectMapper.readValue(message, UseSwapRequest.class);
                    if (useSwapCommand.getTargetPlayerId() == null || useSwapCommand.getTargetPlayerId().isBlank()) {
                        log.error(userId, "Keine gültige Zielspieler-ID für Swap angegeben");
                        throw new IllegalArgumentException("Keine gültige Zielspieler-ID für Swap angegeben");
                    }
                    gameManager.handleUseSwap(useSwapCommand.getTargetPlayerId(), userId);
                    return;
                } catch (NotPlayersTurnException e) {
                    System.err.println("Invalid use swap command from user " + userId + ": " + e.getMessage());
                    log.error(userId, "Ungültiger Use-Swap-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.NOT_YOUR_TURN, e.getMessage());
                    return;
                } catch (NoValidActionException e) {
                    log.error(userId, "Ungültiger Use-Swap-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.INVALID_COMMAND, e.getMessage());
                    return;
                } catch (BonusNotAvailable e) {
                    log.error(userId, "Ungültiger Use-Swap-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.BONUS_NOT_AVAILABLE, e.getMessage());
                    return;
                } catch (GameNotValidException e) {
                    log.error(userId, "Ungültiger Use-Swap-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.GENERAL, e.getMessage());
                    return;
                } catch (IllegalArgumentException e) {
                    log.error(userId, "Ungültiger Use-Swap-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.INVALID_COMMAND, e.getMessage());
                    return;
                } catch (JsonMappingException e) {
                    log.error(userId, "Ungültiger Use-Swap-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.INVALID_COMMAND, e.getMessage());
                    return;
                }
            case "USE_PUSH_FIXED":
                if (gameManager.getTurnInfo().getState() == null
                        || gameManager.getTurnInfo().getState() == TurnState.NOT_STARTED) {
                    sendError(userId, ErrorCode.GENERAL, "Das Spiel hat noch nicht begonnen.");
                    log.error(userId, "Das Spiel hat noch nicht begonnen. Es kann kein fester Push verwendet werden.");
                    return;
                }
                try {
                    UsePushFixedTileRequest pushFixedTileCommand = objectMapper.readValue(message,
                            UsePushFixedTileRequest.class);
                    if (pushFixedTileCommand.getRowOrColIndex() < 0) {
                        log.error(userId, "Keine gültige Zeilen- oder Spaltenindex für Push angegeben");
                        throw new IllegalArgumentException(
                                "Keine gültige Zeilen- oder Spaltenindex für Push angegeben");
                    }
                    if (pushFixedTileCommand.getDirection() == null) {
                        log.error(userId, "Keine gültige Richtung für Push angegeben");
                        throw new IllegalArgumentException("Keine gültige Richtung für Push angegeben");
                    }
                    gameManager.handleUsePushFixedTile(pushFixedTileCommand.getDirection(),
                            pushFixedTileCommand.getRowOrColIndex(), userId);
                    return;
                } catch (PushNotValidException e) {
                    log.error(userId, "Ungültiger Use-Push-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.INVALID_PUSH, e.getMessage());
                    return;
                } catch (NoDirectionForPush e) {
                    log.error(userId, "Ungültiger Use-Push-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.INVALID_COMMAND, e.getMessage());
                    return;
                } catch (NotPlayersTurnException e) {
                    log.error(userId, "Ungültiger Use-Push-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.NOT_YOUR_TURN, e.getMessage());
                    return;
                } catch (NoExtraTileException e) {
                    log.error(userId, "Ungültiger Use-Push-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.GENERAL, e.getMessage());
                    return;
                } catch (GameNotStartedException e) {
                    log.error(userId, "Ungültiger Use-Push-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.GENERAL, e.getMessage());
                    return;
                } catch (GameNotValidException e) {
                    log.error(userId, "Ungültiger Use-Push-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.GENERAL, e.getMessage());
                    return;
                } catch (NoValidActionException e) {
                    log.error(userId, "Ungültiger Use-Push-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.INVALID_COMMAND, e.getMessage());
                    return;
                } catch (BonusNotAvailable e) {
                    log.error(userId, "Ungültiger Use-Push-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.BONUS_NOT_AVAILABLE, e.getMessage());
                    return;
                } catch (IllegalArgumentException e) {
                    log.error(userId, "Ungültiger Use-Push-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.INVALID_COMMAND, e.getMessage());
                    return;
                } catch (JsonMappingException e) {
                    log.error(userId, "Ungültiger Use-Push-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.INVALID_COMMAND, "Ungültiges Nachrichtenformat");
                    return;
                }
            case "USE_PUSH_TWICE":
                if (gameManager.getTurnInfo().getState() == null
                        || gameManager.getTurnInfo().getState() == TurnState.NOT_STARTED) {
                    sendError(userId, ErrorCode.GENERAL, "Das Spiel hat noch nicht begonnen.");
                    return;
                }
                try {
                    gameManager.handleUsePushTwice(userId);
                    return;
                } catch (NotPlayersTurnException e) {
                    log.error(userId, "Ungültiger Use-Push-Twice-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.NOT_YOUR_TURN, e.getMessage());
                    return;
                } catch (BonusNotAvailable e) {
                    log.error(userId, "Ungültiger Use-Push-Twice-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.BONUS_NOT_AVAILABLE, e.getMessage());
                    return;
                } catch (GameNotValidException e) {
                    log.error(userId, "Ungültiger Use-Push-Twice-Befehl von Benutzer {}: {}", userId, e.getMessage());
                    sendError(userId, ErrorCode.GENERAL,
                            e.getMessage());
                    return;
                }
            default:
                return;
        }
    }

    private void sendError(String userId, ErrorCode code, String msg) throws JsonProcessingException {
        ActionErrorEvent errorEvent = new ActionErrorEvent(code, msg);
        socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(errorEvent));
    }

}
