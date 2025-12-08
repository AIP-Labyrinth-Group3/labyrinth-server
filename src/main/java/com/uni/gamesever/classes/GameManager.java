package com.uni.gamesever.classes;

import org.springframework.stereotype.Service;

import com.uni.gamesever.exceptions.GameNotValidException;
import com.uni.gamesever.exceptions.NotPlayersTurnException;
import com.uni.gamesever.models.GameBoard;
import com.uni.gamesever.models.PlayerInfo;

@Service
public class GameManager {
    PlayerManager playerManager;
    private PlayerInfo currentPlayer;
    private GameBoard currentBoard;
    private boolean isGameActive = false;

    public GameManager(PlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    public PlayerInfo getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(PlayerInfo currentPlayer) {
        this.currentPlayer = currentPlayer;
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

    public boolean handlePushTile(int rowOrColIndex, String direction, String playerIdWhoPushed) throws GameNotValidException, NotPlayersTurnException {
        if(!isGameActive) {
            throw new GameNotValidException("Game is not active. Cannot push tile.");
        }
        if(!playerIdWhoPushed.equals(currentPlayer.getId())) {
            throw new NotPlayersTurnException("It's not the turn of player " + playerIdWhoPushed + ". Cannot push tile.");
        }
        return isGameActive;
    }
}
