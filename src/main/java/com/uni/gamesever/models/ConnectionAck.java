package com.uni.gamesever.models;

public class ConnectionAck {
    public String type = "CONNECTION_ACK";
    public String playerId;

    public ConnectionAck(String playerId) {
        this.playerId = playerId;
    }

    public String getPlayerId() {
        return playerId;
    }
}
