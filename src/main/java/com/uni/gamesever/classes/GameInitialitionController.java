package com.uni.gamesever.classes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.gamesever.exceptions.NoExtraTileException;
import com.uni.gamesever.exceptions.NotEnoughPlayerException;
import com.uni.gamesever.exceptions.PlayerNotAdminException;
import com.uni.gamesever.models.BoardSize;
import com.uni.gamesever.models.Coordinates;
import com.uni.gamesever.models.GameBoard;
import com.uni.gamesever.models.GameStarted;
import com.uni.gamesever.models.GameStateUpdate;
import com.uni.gamesever.models.PlayerState;
import com.uni.gamesever.models.PlayerTurn;
import com.uni.gamesever.models.Tile;
import com.uni.gamesever.models.Treasure;
import com.uni.gamesever.services.SocketMessageService;

@Service
public class GameInitialitionController {
    //hier kommt die ganze Gameboard generierung hin
    PlayerManager playerManager;
    GameManager gameManager;
    SocketMessageService socketBroadcastService;
    ObjectMapper objectMapper = new ObjectMapper();

    public GameInitialitionController(PlayerManager playerManager, SocketMessageService socketBroadcastService, GameManager gameManager) {
        this.playerManager = playerManager;
        this.socketBroadcastService = socketBroadcastService;
        this.gameManager = gameManager;
    }

    public boolean handleStartGameMessage(String userID, BoardSize size) throws JsonProcessingException, PlayerNotAdminException, NotEnoughPlayerException, NoExtraTileException {

        if(playerManager.getAdminID() == null || !playerManager.getAdminID().equals(userID)) {
            throw new PlayerNotAdminException("Only the admin can start the game.");
        }
        
        if(playerManager.getAmountOfPlayers() <2) {
            throw new NotEnoughPlayerException("Not enough players to start the game.");
        }
        System.out.println("Starting game with board size: " + size.getRows() + "x" + size.getCols());

        GameBoard board = GameBoard.generateBoard(size);

        GameBoard.printBoard(board);

        playerManager.initializePlayerStates(board);

        List<Treasure> treasures = createTreasures();
        distributeTreasuresOnPlayers(treasures);
        placeTreasuresOnBoard(board, treasures);

        GameStarted startedEvent = new GameStarted(board, playerManager.getNonNullPlayerStates());
        socketBroadcastService.broadcastMessage(objectMapper.writeValueAsString(startedEvent));

        //gameStateUpdate senden
        GameStateUpdate gameState = new GameStateUpdate(board, playerManager.getNonNullPlayerStates());
        String gameStateMessageToBroadcast = objectMapper.writeValueAsString(gameState);
        socketBroadcastService.broadcastMessage(gameStateMessageToBroadcast);

        gameManager.setCurrentPlayer(playerManager.getNonNullPlayerStates()[0].getPlayer());
        gameManager.setCurrentBoard(board);
        gameManager.setGameActive(true);

        if(board.getExtraTile() == null){
            throw new NoExtraTileException("Extra tile was not set on the game board.");
        }

        PlayerTurn turn = new PlayerTurn(gameManager.getCurrentPlayer().getId(), board.getExtraTile(), 60);
        socketBroadcastService.broadcastMessage(objectMapper.writeValueAsString(turn));

        return true;
    }

    public static List<Treasure> createTreasures() {
        //es werden 24 Schätze erstellt
        List<Treasure> treasures = new ArrayList<>();
        for (int i = 1; i <= 24; i++) {
            treasures.add(new Treasure(i, "Treasure " + i));
        }

        return treasures;
    }

    public void distributeTreasuresOnPlayers(List<Treasure> treasures) {
        int playersCount = playerManager.getAmountOfPlayers();
        int index = 0;
        Collections.shuffle(treasures);

        for (PlayerState state : playerManager.getNonNullPlayerStates()) {
            if (state == null) continue;

            List<Treasure> assigned = new ArrayList<>();

            while (index < treasures.size() && assigned.size() < treasures.size() / playersCount) {
                assigned.add(treasures.get(index++));
        }

            state.setTreasuresFound(new Treasure[0]);
            state.setRemainingTreasureCount(assigned.size());
            state.setCurrentTreasure(assigned.get(0));
            state.setAssignedTreasures(assigned);
        }

    }

    public void placeTreasuresOnBoard(GameBoard board, List<Treasure> treasures) {
        int rows = board.getSize().getRows();
        int cols = board.getSize().getCols();
        Tile[][] tiles = board.getTiles();

        List<Tile> validTiles = new ArrayList<>();

        List<Coordinates> forbiddenStartPositions = new ArrayList<>();
        forbiddenStartPositions.add(new Coordinates(0, 0));
        forbiddenStartPositions.add(new Coordinates(0, cols - 1));
        forbiddenStartPositions.add(new Coordinates(rows - 1, 0));
        forbiddenStartPositions.add(new Coordinates(rows - 1, cols - 1));
        
        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            for (int colIndex = 0; colIndex < cols; colIndex++) {

                boolean isStartField = false;
                for (Coordinates start : forbiddenStartPositions) {
                    if (start.getX() == rowIndex && start.getY() == colIndex) {
                        isStartField = true;
                        break;
                    }
                }

                if (!isStartField) {
                    validTiles.add(tiles[rowIndex][colIndex]);
                }
            }
        }

        Collections.shuffle(validTiles);

        int maxAssignable = Math.min(treasures.size(), validTiles.size());

        for (int i = 0; i < maxAssignable; i++) {
            validTiles.get(i).setTreasure(treasures.get(i));
        }

        if (treasures.size() > validTiles.size()) {
            throw new IllegalArgumentException("Not enough valid tiles to place all treasures.");
        }
    }
}