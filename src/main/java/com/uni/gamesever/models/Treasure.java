package com.uni.gamesever.models;

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
