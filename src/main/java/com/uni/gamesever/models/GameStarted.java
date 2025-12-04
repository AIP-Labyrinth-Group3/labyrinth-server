package com.uni.gamesever.models;

public class GameStarted {
    private String type;
    private GameBoard initialBoard;
    private PlayerInfo[] players;

    public GameStarted(GameBoard initialBoard, PlayerInfo[] players) {
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
    public PlayerInfo[] getPlayers() {
        return players;
    }
}
