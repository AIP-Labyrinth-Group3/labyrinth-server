package com.uni.gamesever.domain.game;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.gamesever.domain.enums.LobbyStateEnum;
import com.uni.gamesever.domain.events.GameTimeoutEvent;
import com.uni.gamesever.domain.exceptions.GameAlreadyStartedException;
import com.uni.gamesever.domain.exceptions.NoExtraTileException;
import com.uni.gamesever.domain.exceptions.NotEnoughPlayerException;
import com.uni.gamesever.domain.exceptions.PlayerNotAdminException;
import com.uni.gamesever.domain.model.BoardSize;
import com.uni.gamesever.domain.model.GameBoard;
import com.uni.gamesever.domain.model.PlayerState;
import com.uni.gamesever.domain.model.Treasure;
import com.uni.gamesever.domain.model.TurnState;
import com.uni.gamesever.infrastructure.GameTimerManager;
import com.uni.gamesever.interfaces.Websocket.ObjectMapperSingleton;
import com.uni.gamesever.interfaces.Websocket.SocketConnectionHandler;
import com.uni.gamesever.interfaces.Websocket.messages.server.GameStarted;
import com.uni.gamesever.interfaces.Websocket.messages.server.GameStateUpdate;
import com.uni.gamesever.interfaces.Websocket.messages.server.NextTreasureCardEvent;
import com.uni.gamesever.interfaces.Websocket.messages.server.PlayerTurnEvent;
import com.uni.gamesever.services.SocketMessageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class GameInitializationController {
    PlayerManager playerManager;
    GameManager gameManager;
    SocketMessageService socketBroadcastService;
    ObjectMapper objectMapper = ObjectMapperSingleton.getInstance();
    GameStatsManager gameStatsManager;
    BoardItemPlacementService boardItemPlacementService;
    GameTimerManager gameTimerManager;
    private static final Logger log = LoggerFactory.getLogger("GAME_LOG");

    private final ApplicationEventPublisher eventPublisher;

    public GameInitializationController(PlayerManager playerManager, SocketMessageService socketBroadcastService,
            GameManager gameManager, GameStatsManager gameStatsManager,
            BoardItemPlacementService boardItemPlacementService, GameTimerManager gameTimerManager,
            ApplicationEventPublisher eventPublisher) {
        this.playerManager = playerManager;
        this.socketBroadcastService = socketBroadcastService;
        this.gameManager = gameManager;
        this.gameStatsManager = gameStatsManager;
        this.boardItemPlacementService = boardItemPlacementService;
        this.gameTimerManager = gameTimerManager;
        this.eventPublisher = eventPublisher;

    }

    public boolean handleStartGameMessage(String userID, BoardSize size, int amountOfTreasures, long gameDuration,
            int totalBonusCount)
            throws JsonProcessingException, PlayerNotAdminException, NotEnoughPlayerException, NoExtraTileException,
            GameAlreadyStartedException, IllegalArgumentException {

        if (gameManager.getTurnInfo().getState() != TurnState.NOT_STARTED) {
            throw new GameAlreadyStartedException("Das Spiel hat bereits begonnen.");
        }

        if (playerManager.getAdminID() == null || !playerManager.getAdminID().equals(userID)) {
            throw new PlayerNotAdminException("Nur der Administrator kann das Spiel starten.");
        }

        if (playerManager.getAmountOfPlayers() < 2) {
            throw new NotEnoughPlayerException("Nicht genügend Spieler, um das Spiel zu starten.");
        }
        if (amountOfTreasures < 1 || amountOfTreasures > 24) {
            throw new IllegalArgumentException("Die Anzahl der Schätze muss zwischen 1 und 24 liegen.");
        }
        if (amountOfTreasures < playerManager.getAmountOfPlayers()) {
            throw new IllegalArgumentException(
                    "Die Anzahl der Schätze muss mindestens so groß sein wie die Anzahl der Spieler.");
        }
        log.info("Spiel wurde gestartet mit Brettgröße: {}x{}", size.getRows(), size.getCols());

        gameManager.setTotalBonusCountsOnBoard(totalBonusCount);

        GameBoard board = GameBoard.generateBoard(size);

        playerManager.initializePlayerStates(board);

        List<Treasure> treasures = boardItemPlacementService.createTreasures(amountOfTreasures);
        boardItemPlacementService.placeTreasures(board, treasures);
        distributeTreasuresOnPlayers(treasures);

        for (int i = 0; i < 4; i++) {
            if (boardItemPlacementService.trySpawnBonus(board, gameManager.getTotalBonusCountsOnBoard())) {
                gameManager.reduceTotalBonusCountsOnBoard(1);
            }
        }

        if (board.getSpareTile() == null) {
            throw new NoExtraTileException("Spare tile was not set on the game board.");
        }

        playerManager.setNextPlayerAsCurrent();
        gameManager.setCurrentBoard(board);
        gameManager.setLobbyState(LobbyStateEnum.IN_GAME);

        gameStatsManager.initAllRankingStats(playerManager);

        gameManager.getTurnInfo().setCurrentPlayerId(playerManager.getCurrentPlayer().getId());
        gameManager.getTurnInfo().updateTurnEndTime();

        gameManager
                .setGameEndTime(OffsetDateTime.now().plusSeconds(gameDuration).toString());

        gameManager.resetAllVariablesForNextTurn();

        gameTimerManager.start(gameDuration, () -> {
            eventPublisher.publishEvent(new GameTimeoutEvent());
        });

        GameStarted startedEvent = new GameStarted(board, playerManager.getNonNullPlayerStates(),
                gameManager.getTurnInfo(),
                gameManager.getGameEndTime());
        socketBroadcastService.broadcastMessage(objectMapper.writeValueAsString(startedEvent));

        for (PlayerState state : playerManager.getNonNullPlayerStates()) {
            NextTreasureCardEvent nextTreasureCardEvent = new NextTreasureCardEvent(state.getCurrentTreasure());
            socketBroadcastService.sendMessageToSession(state.getPlayerInfo().getId(),
                    objectMapper.writeValueAsString(nextTreasureCardEvent));
        }

        GameStateUpdate gameStateUpdate = new GameStateUpdate(board, playerManager.getNonNullPlayerStates(),
                gameManager.getTurnInfo(), gameManager.getGameEndTime());
        socketBroadcastService.broadcastMessage(objectMapper.writeValueAsString(gameStateUpdate));

        // Prüfe ob der erste Spieler AI-gesteuert ist (disconnected beim Spielstart)
        gameManager.checkAndExecuteAIIfNeeded();

        return true;
    }

    public void distributeTreasuresOnPlayers(List<Treasure> treasures) {
        int playersCount = playerManager.getAmountOfPlayers();
        int index = 0;
        Collections.shuffle(treasures);

        for (PlayerState state : playerManager.getNonNullPlayerStates()) {
            if (state == null)
                continue;

            List<Treasure> assigned = new ArrayList<>();

            while (index < treasures.size() && assigned.size() < treasures.size() / playersCount) {
                assigned.add(treasures.get(index++));
            }
            state.setRemainingTreasureCount(assigned.size());
            state.setCurrentTreasure(assigned.get(0));
            state.setAssignedTreasures(assigned);
        }
    }
}