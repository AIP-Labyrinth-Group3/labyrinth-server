package com.uni.gamesever.interfaces.Websocket.messages.client;

import com.uni.gamesever.domain.model.Coordinates;

public class UseBeamRequest extends Message {
    private Coordinates targetCoordinates;

    public UseBeamRequest() {
        super("USE_BEAM");
        this.targetCoordinates = null;
    }

    public UseBeamRequest(Coordinates targetCoordinates) {
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
