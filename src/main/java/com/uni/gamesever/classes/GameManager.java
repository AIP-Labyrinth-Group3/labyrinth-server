package com.uni.gamesever.classes;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.gamesever.exceptions.GameNotValidException;
import com.uni.gamesever.exceptions.NotPlayersTurnException;
import com.uni.gamesever.exceptions.PushNotValidException;
import com.uni.gamesever.models.GameBoard;
import com.uni.gamesever.models.GameStateUpdate;
import com.uni.gamesever.models.PushActionInfo;
import com.uni.gamesever.services.SocketMessageService;

@Service
public class GameManager {
    PlayerManager playerManager;
    private GameBoard currentBoard;
    private boolean isGameActive = false;
    SocketMessageService socketBroadcastService;
    private final ObjectMapper objectMapper = ObjectMapperSingleton.getInstance();


    public GameManager(PlayerManager playerManager, SocketMessageService socketBroadcastService) {
        this.playerManager = playerManager;
        this.socketBroadcastService = socketBroadcastService;
    }

    public GameBoard getCurrentBoard() {
        return currentBoard;
    }
    public void setCurrentBoard(GameBoard currentBoard) {
        this.currentBoard = currentBoard;
    }
    public boolean isGameActive() {
        return isGameActive;
    }
    public void setGameActive(boolean isGameActive) {
        this.isGameActive = isGameActive;
    }

    public boolean handlePushTile(int rowOrColIndex, String direction, String playerIdWhoPushed) throws GameNotValidException, NotPlayersTurnException, PushNotValidException, JsonProcessingException, IllegalArgumentException {
        if(!isGameActive) {
            throw new GameNotValidException("Game is not active. Cannot push tile.");
        }
        if(!playerIdWhoPushed.equals(playerManager.getCurrentPlayer().getId())) {
            throw new NotPlayersTurnException("It's not the turn of player " + playerIdWhoPushed + ". Cannot push tile.");
        }
        if(currentBoard.getLastPush() != null){
            int lastIndex = currentBoard.getLastPush().getRowOrColIndex();
            String lastDirection = currentBoard.getLastPush().getDirection();

            if(lastIndex == rowOrColIndex && isOppositeDirection(lastDirection, direction)) {
                throw new PushNotValidException("Invalid push: same index and opposite direction");
            }
        }

        currentBoard.updateBoard(rowOrColIndex, direction);
        PushActionInfo pushInfo = new PushActionInfo(rowOrColIndex);
        pushInfo.setDirections(direction);
        currentBoard.setLastPush(pushInfo);

        GameBoard.printBoard(currentBoard);

        GameStateUpdate gameState = new GameStateUpdate(currentBoard, playerManager.getNonNullPlayerStates());
        socketBroadcastService.broadcastMessage(objectMapper.writeValueAsString(gameState));

        return true;
    }

    public boolean isOppositeDirection(String dir1, String dir2) {
        return (dir1.equals("UP") && dir2.equals("DOWN")) ||
               (dir1.equals("DOWN") && dir2.equals("UP")) ||
               (dir1.equals("LEFT") && dir2.equals("RIGHT")) ||
               (dir1.equals("RIGHT") && dir2.equals("LEFT"));
    }
}
