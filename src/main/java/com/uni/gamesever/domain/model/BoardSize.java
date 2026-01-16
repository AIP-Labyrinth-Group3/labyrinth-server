package com.uni.gamesever.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BoardSize {
    private int rows;
    private int cols;

    public BoardSize() {
        this.rows = 7;
        this.cols = 7;
    }

    public BoardSize(int rows, int cols) throws IllegalArgumentException {
        setRows(rows);
        setCols(cols);
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public void setRows(int rows) throws IllegalArgumentException {
        this.rows = rows;
    }

    public void setCols(int cols) throws IllegalArgumentException {
        this.cols = cols;
    }
}
