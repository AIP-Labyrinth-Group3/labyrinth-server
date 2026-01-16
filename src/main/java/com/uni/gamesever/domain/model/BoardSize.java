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
        if (rows < 3 || rows > 11) {
            throw new IllegalArgumentException("Die Anzahl der Reihen muss zwischen 3 und 11 liegen.");
        }
        this.rows = rows;
    }

    public void setCols(int cols) throws IllegalArgumentException {
        if (cols < 3 || cols > 11) {
            throw new IllegalArgumentException("Die Anzahl der Spalten muss zwischen 3 und 11 liegen.");
        }
        this.cols = cols;
    }
}
