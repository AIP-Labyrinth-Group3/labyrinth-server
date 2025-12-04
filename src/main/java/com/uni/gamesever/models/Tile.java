package com.uni.gamesever.models;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;


public class Tile {
    private List<String> entrances;
    @JsonIgnore
    private String type;
    private Treasure treasure;
    private Bonus bonus;
    private boolean isFixed;

    public Tile(List<String> entrances, String type, boolean isFixed) {
        this.entrances = entrances;
        this.type = type;
        this.isFixed = isFixed;
    }
    public Tile(List<String> entrances, String type) {
        this.entrances = entrances;
        this.type = type;
        this.isFixed = false;
    }

    public List<String> getEntrances() {
        return entrances;
    }

    public void setEntrances(List<String> entrances) {
        this.entrances = entrances;
    }

    public Treasure getTreasure() {
        return treasure;
    }

    public void setTreasure(Treasure treasure) {
        this.treasure = treasure;
    }

    public Bonus getBonus() {
        return bonus;
    }

    public void setBonus(Bonus bonus) {
        this.bonus = bonus;
    }

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    public void setFixed(boolean isFixed) {
        this.isFixed = isFixed;
    }
    public boolean isFixed() {
        return isFixed;
    }
}
