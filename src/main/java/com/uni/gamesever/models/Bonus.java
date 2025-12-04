package com.uni.gamesever.models;

public class Bonus {
    private String id;
    private String type;

    public Bonus(String id) {
        this.id = id;
        this.type = BonusType.SWAP.name();
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        try {
            BonusType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid bonus type: " + type + ". Valid types are: BEAM, PUSH_FIXED, SWAP, PUSH_TWICE");
        }
        this.type = type;
    }
}
