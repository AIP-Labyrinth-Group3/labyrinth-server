package com.uni.gamesever.models;

public class GameStarted {
    private String eventType;

    public GameStarted() {
        this.eventType = "GAME_STARTED";
    }
    public String getEventType() {
        return eventType;
    }
    
}
