package com.uni.gamesever.interfaces.Websocket;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.gamesever.domain.events.GameTimeoutEvent;
import com.uni.gamesever.domain.exceptions.GameAlreadyStartedException;
import com.uni.gamesever.domain.exceptions.NoExtraTileException;
import com.uni.gamesever.domain.exceptions.NotEnoughPlayerException;
import com.uni.gamesever.domain.exceptions.PlayerNotAdminException;
import com.uni.gamesever.domain.game.BoardItemPlacementService;
import com.uni.gamesever.domain.game.GameManager;
import com.uni.gamesever.domain.game.GameStatsManager;
import com.uni.gamesever.domain.game.PlayerManager;
import com.uni.gamesever.domain.model.BoardSize;
import com.uni.gamesever.domain.model.GameBoard;
import com.uni.gamesever.domain.model.PlayerState;
import com.uni.gamesever.domain.model.Treasure;
import com.uni.gamesever.domain.model.TurnInfo;
import com.uni.gamesever.domain.model.TurnState;
import com.uni.gamesever.infrastructure.GameTimerManager;
import com.uni.gamesever.interfaces.Websocket.messages.server.GameStarted;
import com.uni.gamesever.interfaces.Websocket.messages.server.GameStateUpdate;
import com.uni.gamesever.interfaces.Websocket.messages.server.PlayerTurnEvent;
import com.uni.gamesever.services.SocketMessageService;
import org.springframework.context.ApplicationEventPublisher;

@Service
public class GameInitializationController {
    PlayerManager playerManager;
    GameManager gameManager;
    SocketMessageService socketBroadcastService;
    ObjectMapper objectMapper = ObjectMapperSingleton.getInstance();
    GameStatsManager gameStatsManager;
    BoardItemPlacementService boardItemPlacementService;
    GameTimerManager gameTimerManager;
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

        if (gameManager.getTurnState() != TurnState.NOT_STARTED) {
            throw new GameAlreadyStartedException("Game has already been started.");
        }

        if (playerManager.getAdminID() == null || !playerManager.getAdminID().equals(userID)) {
            throw new PlayerNotAdminException("Only the admin can start the game.");
        }

        if (playerManager.getAmountOfPlayers() < 2) {
            throw new NotEnoughPlayerException("Not enough players to start the game.");
        }
        if (amountOfTreasures < 1 || amountOfTreasures > 24) {
            throw new IllegalArgumentException("Amount of treasures must be between 1 and 24.");
        }

        System.out.println("Starting game with board size: " + size.getRows() + "x" + size.getCols());

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
        gameManager.setTurnState(TurnState.WAITING_FOR_PUSH);

        gameStatsManager.initAllRankingStats(playerManager);

        gameManager.getTurnInfo().setCurrentPlayerId(playerManager.getCurrentPlayer().getId());
        gameManager.getTurnInfo().setTurnState(TurnState.WAITING_FOR_PUSH);
        gameManager.getTurnInfo().updateTurnEndTime();

        gameManager
                .setGameEndTime(OffsetDateTime.now().plusSeconds(gameDuration).toString());

        gameTimerManager.start(gameDuration, () -> {
            eventPublisher.publishEvent(new GameTimeoutEvent());
        });

        GameStarted startedEvent = new GameStarted(board, playerManager.getNonNullPlayerStates(),
                gameManager.getTurnInfo(),
                gameManager.getGameEndTime());
        socketBroadcastService.broadcastMessage(objectMapper.writeValueAsString(startedEvent));

        GameStateUpdate gameStateUpdate = new GameStateUpdate(board, playerManager.getNonNullPlayerStates(),
                playerManager.getCurrentPlayer().getId(), gameManager.getTurnState().name());
        socketBroadcastService.broadcastMessage(objectMapper.writeValueAsString(gameStateUpdate));

        PlayerTurnEvent turn = new PlayerTurnEvent(playerManager.getCurrentPlayer().getId(), board.getSpareTile(), 60);
        socketBroadcastService.broadcastMessage(objectMapper.writeValueAsString(turn));

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