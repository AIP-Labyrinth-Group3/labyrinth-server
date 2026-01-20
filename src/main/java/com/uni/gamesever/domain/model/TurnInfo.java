package com.uni.gamesever.domain.model;

import java.time.OffsetDateTime;

public class TurnInfo {
    private String currentPlayerId;
    private String turnEndTime;
    private TurnState state;

    public TurnInfo(String currentPlayerId, TurnState state) {
        this.currentPlayerId = currentPlayerId;
        this.turnEndTime = null;
        this.state = state;
    }

    public String getCurrentPlayerId() {
        return currentPlayerId;
    }

    public String getTurnEndTime() {
        return turnEndTime;
    }

    public TurnState getState() {
        return state;
    }

    public void setCurrentPlayerId(String currentPlayerId) {
        this.currentPlayerId = currentPlayerId;
    }

    public void updateTurnEndTime() {
        this.turnEndTime = OffsetDateTime.now().plusSeconds(60).toString();
    }

    public void setState(TurnState state) {
        this.state = state;
    }
}
