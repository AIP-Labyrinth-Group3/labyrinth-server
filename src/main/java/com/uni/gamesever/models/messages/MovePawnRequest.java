package com.uni.gamesever.models.messages;

import com.uni.gamesever.models.Coordinates;

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
