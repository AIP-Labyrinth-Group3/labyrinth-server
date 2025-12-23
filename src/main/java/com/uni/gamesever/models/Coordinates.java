package com.uni.gamesever.models;

public class Coordinates {
    private int column;
    private int row;

    public Coordinates() {
        this.column = -1;
        this.row = -1;
    }

    public Coordinates(int column, int row) {
        this.column = column;
        this.row = row;
    }

    public int getColumn() {
        return column;
    }

    public int getRow() {
        return row;
    }

    public void setColumn(int c) {
        if (c < 0) {
            throw new IllegalArgumentException("Column coordinate cannot be negative");
        }
        this.column = c;
    }

    public void setRow(int r) {
        if (r < 0) {
            throw new IllegalArgumentException("Row coordinate cannot be negative");
        }
        this.row = r;
    }

}
