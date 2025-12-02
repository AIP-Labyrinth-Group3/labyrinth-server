package com.uni.gamesever.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Bonus {
    private String id;
    private String type;

    public Bonus(String id) {
        this.id = id;
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
