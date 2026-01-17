package com.uni.gamesever.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
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
        this.column = c;
    }

    public void setRow(int r) {
        this.row = r;
    }

}
