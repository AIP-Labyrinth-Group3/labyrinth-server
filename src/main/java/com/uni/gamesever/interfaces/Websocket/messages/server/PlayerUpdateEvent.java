package com.uni.gamesever.interfaces.Websocket.messages.server;

import com.uni.gamesever.domain.model.PlayerInfo;
import com.uni.gamesever.interfaces.Websocket.messages.client.Message;

public class PlayerUpdateEvent extends Message {
    private final PlayerInfo playerInfo;

    public PlayerUpdateEvent(PlayerInfo playerInfo) {
        super("PLAYER_UPDATED");
        this.playerInfo = playerInfo;
    }

    public PlayerInfo getPlayerInfo() {
        return playerInfo;
    }

}
