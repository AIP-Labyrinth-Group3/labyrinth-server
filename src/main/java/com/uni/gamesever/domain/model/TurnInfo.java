package com.uni.gamesever.domain.model;

import java.time.Instant;
import java.time.OffsetDateTime;

public class TurnInfo {
    private String currentPlayerId;
    private String turnEndTime;
    private TurnState turnState;

    public TurnInfo(String currentPlayerId, TurnState turnState) {
        this.currentPlayerId = currentPlayerId;
        this.turnEndTime = null;
        this.turnState = turnState;
    }

    public String getCurrentPlayerId() {
        return currentPlayerId;
    }

    public String getTurnEndTime() {
        return turnEndTime;
    }

    public TurnState getTurnState() {
        return turnState;
    }

    public void setCurrentPlayerId(String currentPlayerId) {
        this.currentPlayerId = currentPlayerId;
    }

    public void updateTurnEndTime() {
        this.turnEndTime = OffsetDateTime.now().plusSeconds(60).toString();
    }

    public void setTurnState(TurnState turnState) {
        this.turnState = turnState;
    }
}
