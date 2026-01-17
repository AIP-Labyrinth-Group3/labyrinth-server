package com.uni.gamesever.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.uni.gamesever.domain.enums.BonusType;

public class Bonus {

    private static int counter = 1;

    @JsonIgnore
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

    @JsonValue
    public String toJson() {
        return type.name();
    }
}
