package com.uni.gamesever.models;

public class NextTreasureCardEvent {
    private String type = "NEXT_TREASURE";
    private Treasure treasure;

    public NextTreasureCardEvent(Treasure treasure) {
        this.treasure = treasure;
    }

    public String getType() {
        return type;
    }

    public Treasure getTreasure() {
        return treasure;
    }

    public void setTreasure(Treasure treasure) {
        this.treasure = treasure;
    }
}
