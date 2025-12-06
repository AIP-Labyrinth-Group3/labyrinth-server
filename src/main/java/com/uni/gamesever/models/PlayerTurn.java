package com.uni.gamesever.models;


public class PlayerTurn {
    private String type = "PLAYER_TURN";
    private String playerId;
    private Tile extraTile;
    private int turnTimeLimitSeconds;

    public PlayerTurn(String playerId, Tile extraTile, int turnTimeLimitSeconds) {
        this.playerId = playerId;
        this.extraTile = extraTile;
        this.turnTimeLimitSeconds = turnTimeLimitSeconds;
    }

    public String getType() {
        return type;
    }
    public String getPlayerId() {
        return playerId;
    }
    public Tile getExtraTile() {
        return extraTile;
    }
    public int getTurnTimeLimitSeconds() {
        return turnTimeLimitSeconds;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }
    public void setExtraTile(Tile extraTile) {
        this.extraTile = extraTile;
    }
    public void setTurnTimeLimitSeconds(int turnTimeLimitSeconds) {
        this.turnTimeLimitSeconds = turnTimeLimitSeconds;
    }
}
