package com.uni.gamesever.models;

import java.util.List;

public class PushActionInfo {
    private int rowOrColIndex;
    private List<String> directions;

    public PushActionInfo(int rowOrColIndex) {
        this.rowOrColIndex = rowOrColIndex;
    }

    public int getRowOrColIndex() {
        return rowOrColIndex;
    }
    public void setRowOrColIndex(int rowOrColIndex) {
        this.rowOrColIndex = rowOrColIndex;
    }
    public List<String> getDirections() {
        return directions;
    }
    public void setDirections(List<String> directions) {
            for (String dir : directions) {
                try {
                    DirectionType.valueOf(dir);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid direction type: " + dir + ". Valid types are: UP, DOWN, LEFT, RIGHT");
                }
            }
            this.directions = directions;
    }
}
