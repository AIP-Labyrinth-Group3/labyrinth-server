package com.uni.gamesever.models;

public class Bonus {
    private static int counter = 1;
    private int id;
    private BonusType type;

    public Bonus() {
        this.id = counter++;
        this.type = BonusType.SWAP;
    }

    public int getId() {
        return id;
    }

    public BonusType getType() {
        return type;
    }

    public void setType(BonusType type) {
        this.type = type;
    }
}
