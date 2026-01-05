package com.uni.gamesever.classes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.gamesever.exceptions.GameAlreadyStartedException;
import com.uni.gamesever.exceptions.NoExtraTileException;
import com.uni.gamesever.exceptions.NotEnoughPlayerException;
import com.uni.gamesever.exceptions.PlayerNotAdminException;
import com.uni.gamesever.models.BoardSize;
import com.uni.gamesever.models.GameBoard;
import com.uni.gamesever.models.GameStarted;
import com.uni.gamesever.models.GameStateUpdate;
import com.uni.gamesever.models.PlayerState;
import com.uni.gamesever.models.PlayerTurn;
import com.uni.gamesever.models.Treasure;
import com.uni.gamesever.models.TurnState;
import com.uni.gamesever.services.BoardItemPlacementService;
import com.uni.gamesever.services.SocketMessageService;

@Service
public class GameInitializationController {
    // hier kommt die ganze Gameboard generierung hin
    PlayerManager playerManager;
    GameManager gameManager;
    SocketMessageService socketBroadcastService;
    ObjectMapper objectMapper = ObjectMapperSingleton.getInstance();
    GameStatsManager gameStatsManager;
    BoardItemPlacementService boardItemPlacementService;

    public GameInitializationController(PlayerManager playerManager, SocketMessageService socketBroadcastService,
            GameManager gameManager, GameStatsManager gameStatsManager,
            BoardItemPlacementService boardItemPlacementService) {
        this.playerManager = playerManager;
        this.socketBroadcastService = socketBroadcastService;
        this.gameManager = gameManager;
        this.gameStatsManager = gameStatsManager;
        this.boardItemPlacementService = boardItemPlacementService;
    }

    public boolean handleStartGameMessage(String userID, BoardSize size, int amountOfTreasures)
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

        GameBoard board = GameBoard.generateBoard(size);

        playerManager.initializePlayerStates(board);

        List<Treasure> treasures = boardItemPlacementService.createTreasures(amountOfTreasures);
        boardItemPlacementService.placeTreasures(board, treasures);
        distributeTreasuresOnPlayers(treasures);

        for (int i = 0; i < 4; i++) {
            boardItemPlacementService.trySpawnBonus(board);
        }

        if (board.getExtraTile() == null) {
            throw new NoExtraTileException("Extra tile was not set on the game board.");
        }

        GameStarted startedEvent = new GameStarted(board, playerManager.getNonNullPlayerStates());
        socketBroadcastService.broadcastMessage(objectMapper.writeValueAsString(startedEvent));

        playerManager.setNextPlayerAsCurrent();
        gameManager.setCurrentBoard(board);
        gameManager.setTurnState(TurnState.WAITING_FOR_PUSH);

        gameStatsManager.initAllRankingStats(playerManager);

        GameStateUpdate gameStateUpdate = new GameStateUpdate(board, playerManager.getNonNullPlayerStates());
        socketBroadcastService.broadcastMessage(objectMapper.writeValueAsString(gameStateUpdate));

        PlayerTurn turn = new PlayerTurn(playerManager.getCurrentPlayer().getId(), board.getExtraTile(), 60);
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