package com.uni.gamesever.models.messages;

import com.uni.gamesever.models.Coordinates;

public class UseBeamCommand extends Message {
    private Coordinates targetCoordinates;

    public UseBeamCommand() {
        super("USE_BEAM");
        this.targetCoordinates = null;
    }

    public UseBeamCommand(Coordinates targetCoordinates) {
        super("USE_BEAM");
        this.targetCoordinates = targetCoordinates;
    }

    public Coordinates getTargetCoordinates() {
        return targetCoordinates;
    }

    public void setTargetCoordinates(Coordinates targetCoordinates) {
        this.targetCoordinates = targetCoordinates;
    }

}
