package com.uni.gamesever.classes;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public GameInitialitionController(PlayerManager playerManager, SocketMessageService socketBroadcastService) {
        this.playerManager = playerManager;
        this.socketBroadcastService = socketBroadcastService;
    }

    public int handleStartGameMessage(BoardSize size) throws JsonProcessingException {
        //implementierung der Gameboard generierung
        
        if(playerManager.getAmountOfPlayers() <2) {
            System.out.println("Not enough players to start the game.");
            return -1;
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
        ObjectMapper mapper = new ObjectMapper();
        String gameStartMessageToBroadcast = mapper.writeValueAsString(startedEvent);
        socketBroadcastService.broadcastMessage(gameStartMessageToBroadcast);

        //gameStateUpdate senden
        GameStateUpdate gameState = new GameStateUpdate(board, playerManager.getPlayerStates());
        String gameStateMessageToBroadcast = mapper.writeValueAsString(gameState);
        socketBroadcastService.broadcastMessage(gameStateMessageToBroadcast);

        return 1;
    }

    
    
}
