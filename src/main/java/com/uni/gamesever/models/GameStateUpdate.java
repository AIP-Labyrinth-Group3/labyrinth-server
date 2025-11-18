package com.uni.gamesever.models;

public class GameStateUpdate {
    private String eventType = "GAME_STATE_UPDATE";
    private GameBoard gameBoard;
    private PlayerState[] playerState;

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

    
    public void setGameBoard(GameBoard gameBoard) {
        this.gameBoard = gameBoard;
    }
    public void setPlayerState(PlayerState[] playerState) {
        this.playerState = playerState;
    }

}
