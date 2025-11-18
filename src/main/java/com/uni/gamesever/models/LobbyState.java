package com.uni.gamesever.models;

public class LobbyState {
    private String type;
    private PlayerInfo[] players;

    public LobbyState(String type, PlayerInfo[] players) {
        this.type = type;
        this.players = players;
    }

    public String getType() {
        return type;
    }

    public PlayerInfo[] getPlayers() {
        return players;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setPlayers(PlayerInfo[] players) {
        this.players = players;
    }
}
