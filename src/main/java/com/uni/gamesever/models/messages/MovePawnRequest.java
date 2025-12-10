package com.uni.gamesever.models.messages;

import com.uni.gamesever.models.Coordinates;

public class MovePawnRequest extends Message {
    Coordinates targetCoordinates;

    public MovePawnRequest() {
        super("MOVE_PAWN");
    }

    public Coordinates getTargetCoordinates() {
        return targetCoordinates;
    }

    public void setTargetCoordinates(Coordinates targetCoordinates) {
        this.targetCoordinates = targetCoordinates;
    }

}
