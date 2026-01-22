package com.uni.gamesever.interfaces.Websocket.messages.server;

import com.uni.gamesever.domain.model.PlayerInfo;
import com.uni.gamesever.interfaces.Websocket.messages.client.Message;

public class PlayerUpdateEvent extends Message {
    private final PlayerInfo player;

    public PlayerUpdateEvent(PlayerInfo player) {
        super("PLAYER_UPDATED");
        this.player = player;
    }

    public PlayerInfo getPlayer() {
        return player;
    }

}
