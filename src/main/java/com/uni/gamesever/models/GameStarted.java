package com.uni.gamesever.models;

public class GameStarted {
    private String type;
    private GameBoard initialBoard;
    private PlayerState[] players;

    public GameStarted(GameBoard initialBoard, PlayerState[] players) {
         this.type = "GAME_STARTED";
         this.initialBoard = initialBoard;
         this.players = players;
    }

    public String getType() {
        return type;
    }
    public GameBoard getInitialBoard() {
        return initialBoard;
    }
    public PlayerState[] getPlayers() {
        return players;
    }
}
