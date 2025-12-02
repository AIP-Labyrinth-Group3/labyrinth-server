package com.uni.gamesever.classes;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.gamesever.exceptions.NotEnoughPlayerException;
import com.uni.gamesever.exceptions.PlayerNotAdminException;
import com.uni.gamesever.models.BoardSize;
import com.uni.gamesever.models.GameBoard;
import com.uni.gamesever.models.GameStarted;
import com.uni.gamesever.models.GameStateUpdate;
import com.uni.gamesever.services.SocketMessageService;

@Service
public class GameInitialitionController {
    //hier kommt die ganze Gameboard generierung hin
    PlayerManager playerManager;
    SocketMessageService socketBroadcastService;
    ObjectMapper objectMapper = new ObjectMapper();

    public GameInitialitionController(PlayerManager playerManager, SocketMessageService socketBroadcastService) {
        this.playerManager = playerManager;
        this.socketBroadcastService = socketBroadcastService;
    }

    public boolean handleStartGameMessage(String userID, BoardSize size) throws JsonProcessingException, PlayerNotAdminException, NotEnoughPlayerException {

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

        GameStarted startedEvent = new GameStarted(board, playerManager.getPlayers());
        socketBroadcastService.broadcastMessage(objectMapper.writeValueAsString(startedEvent));

        //gameStateUpdate senden
        GameStateUpdate gameState = new GameStateUpdate(board, playerManager.getPlayerStates());
        String gameStateMessageToBroadcast = objectMapper.writeValueAsString(gameState);
        socketBroadcastService.broadcastMessage(gameStateMessageToBroadcast);

        return true;
    }

    
    
}
