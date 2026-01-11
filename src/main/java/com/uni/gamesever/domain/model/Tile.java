package com.uni.gamesever.domain.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.uni.gamesever.domain.enums.DirectionType;
import com.uni.gamesever.domain.enums.TileType;

public class Tile {
    private List<DirectionType> entrances;
    @JsonIgnore
    private TileType type;
    private Treasure treasure;
    private Bonus bonus;
    private boolean isFixed;

    public Tile(List<DirectionType> entrances, TileType type, boolean isFixed) {
        this.entrances = entrances;
        this.type = type;
        this.isFixed = isFixed;
    }

    public Tile(List<DirectionType> entrances, TileType type) {
        this.entrances = entrances;
        this.type = type;
        this.isFixed = false;
    }

    public List<DirectionType> getEntrances() {
        return entrances;
    }

    public void setEntrances(List<DirectionType> entrances) {
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

    public TileType getType() {
        return type;
    }

    public void setType(TileType type) {
        this.type = type;
    }

    public void setisFixed(boolean isFixed) {
        this.isFixed = isFixed;
    }

    public boolean getIsFixed() {
        return isFixed;
    }

    public void rotateClockwise() {
        for (int i = 0; i < entrances.size(); i++) {
            DirectionType dir = entrances.get(i);
            switch (dir) {
                case UP:
                    entrances.set(i, DirectionType.RIGHT);
                    break;
                case RIGHT:
                    entrances.set(i, DirectionType.DOWN);
                    break;
                case DOWN:
                    entrances.set(i, DirectionType.LEFT);
                    break;
                case LEFT:
                    entrances.set(i, DirectionType.UP);
                    break;
                default:
                    break;
            }
        }
    }
}
