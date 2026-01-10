package com.uni.gamesever.domain.model;

public class Treasure {
    private int id;
    private String name;

    public Treasure(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
