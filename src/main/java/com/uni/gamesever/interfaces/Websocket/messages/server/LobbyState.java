package com.uni.gamesever.interfaces.Websocket.messages.server;

import com.uni.gamesever.domain.model.PlayerInfo;
import com.uni.gamesever.interfaces.Websocket.messages.client.Message;

public class LobbyState extends Message {
    private PlayerInfo[] players;

    public LobbyState(PlayerInfo[] players) {
        super("LOBBY_STATE");
        this.players = players;
    }

    public PlayerInfo[] getPlayers() {
        return players;
    }

    public void setPlayers(PlayerInfo[] players) {
        this.players = players;
    }
}
