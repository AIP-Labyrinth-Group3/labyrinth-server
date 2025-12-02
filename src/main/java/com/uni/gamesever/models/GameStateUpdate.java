package com.uni.gamesever.models;

public class GameStateUpdate {
    private String eventType = "GAME_STATE_UPDATE";
    private GameBoard gameBoard;
    private PlayerState[] playerState;
    private String currentPlayerId;
    private String turnState;

    public GameStateUpdate(GameBoard gameBoard, PlayerState[] playerState) {
        this.gameBoard = gameBoard;
        this.playerState = playerState;
    }
    public String getEventType() {
        return eventType;
    }
    public GameBoard getGameBoard() {
        return gameBoard;
    }
    public PlayerState[] getPlayerState() {
        return playerState;
    }

    public String getCurrentPlayerId() {
        return currentPlayerId;
    }
    public void setCurrentPlayerId(String currentPlayerId) {
        this.currentPlayerId = currentPlayerId;
    }

    public String getTurnState() {
        return turnState;
    }
    public void setTurnState(String turnState) {
        try {
            TurnState.valueOf(turnState);
            this.turnState = turnState;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid turn state: " + turnState);
        }
    }

    
    public void setGameBoard(GameBoard gameBoard) {
        this.gameBoard = gameBoard;
    }
    public void setPlayerState(PlayerState[] playerState) {
        this.playerState = playerState;
    }

}
