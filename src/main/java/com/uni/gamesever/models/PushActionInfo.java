package com.uni.gamesever.models;

public class PushActionInfo {
    private int rowOrColIndex;
    private String direction;

    public PushActionInfo(int rowOrColIndex) {
        this.rowOrColIndex = rowOrColIndex;
    }

    public int getRowOrColIndex() {
        return rowOrColIndex;
    }
    public void setRowOrColIndex(int rowOrColIndex) {
        this.rowOrColIndex = rowOrColIndex;
    }
    public String getDirection() {
        return direction;
    }
    public void setDirections(String direction) {
            try {
                DirectionType.valueOf(direction.toUpperCase());
                this.direction = direction;
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid direction type: " + direction + ". Valid types are: UP, DOWN, LEFT, RIGHT");
            }
    }
}
