package com.uni.gamesever.models;

public class Coordinates {
    private int x;
    private int y;

    public Coordinates() {
        this.x = -1;
        this.y = -1;
    }

    public Coordinates(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setX(int x) {
        if (x < 0) {
            throw new IllegalArgumentException("X coordinate cannot be negative");
        }
        this.x = x;
    }

    public void setY(int y) {
        if (y < 0) {
            throw new IllegalArgumentException("Y coordinate cannot be negative");
        }
        this.y = y;
    }

}
