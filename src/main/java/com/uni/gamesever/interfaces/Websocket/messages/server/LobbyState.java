package com.uni.gamesever.interfaces.Websocket.messages.server;

import com.uni.gamesever.domain.model.PlayerInfo;

public class LobbyState {
    private String type = "LOBBY_STATE";
    private PlayerInfo[] players;

    public LobbyState(PlayerInfo[] players) {
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
