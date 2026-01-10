package com.uni.gamesever.interfaces.Websocket.messages.client;

import com.uni.gamesever.domain.model.Coordinates;

public class MovePawnRequest extends Message {
    private Coordinates targetCoordinates;

    public MovePawnRequest() {
        super("MOVE_PAWN");
        this.targetCoordinates = null;
    }

    public Coordinates getTargetCoordinates() {
        return targetCoordinates;
    }

    public void setTargetCoordinates(Coordinates targetCoordinates) {
        this.targetCoordinates = targetCoordinates;
    }

}
