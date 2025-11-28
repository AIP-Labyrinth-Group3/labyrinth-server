package com.uni.gamesever.classes;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.gamesever.exceptions.GameNotValidException;
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

    public boolean handleStartGameMessage(BoardSize size) throws JsonProcessingException, GameNotValidException {
        //implementierung der Gameboard generierung
        
        if(playerManager.getAmountOfPlayers() <2) {
            throw new GameNotValidException("Not enough players to start the game.");
        }
        System.out.println("Starting game with board size: " + size.getRows() + "x" + size.getCols());

        //Gameboard generieren
        GameBoard board = GameBoard.generateBoard(size);

        GameBoard.printBoard(board);

        //PlayerState initialisieren
        playerManager.initializePlayerStates(board);


        //Spiel starten -> Client mitteilen 
        GameStarted startedEvent = new GameStarted();
        //parse it into json and send to clients
        String gameStartMessageToBroadcast = objectMapper.writeValueAsString(startedEvent);
        socketBroadcastService.broadcastMessage(gameStartMessageToBroadcast);

        //gameStateUpdate senden
        GameStateUpdate gameState = new GameStateUpdate(board, playerManager.getPlayerStates());
        String gameStateMessageToBroadcast = objectMapper.writeValueAsString(gameState);
        socketBroadcastService.broadcastMessage(gameStateMessageToBroadcast);

        return true;
    }

    
    
}
