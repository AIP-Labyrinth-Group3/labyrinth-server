package com.uni.gamesever.interfaces.Websocket.messages.server;

import com.uni.gamesever.domain.model.Treasure;
import com.uni.gamesever.interfaces.Websocket.messages.client.Message;

public class NextTreasureCardEvent extends Message {
    private Treasure treasure;

    public NextTreasureCardEvent(Treasure treasure) {
        super("NEXT_TREASURE");
        this.treasure = treasure;
    }

    public Treasure getTreasure() {
        return treasure;
    }

    public void setTreasure(Treasure treasure) {
        this.treasure = treasure;
    }
}
