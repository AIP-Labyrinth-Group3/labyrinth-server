package com.uni.gamesever.models;

public class BoardSize {
    private int rows;
    private int cols;

    public BoardSize() {
        this.rows = 7;
        this.cols = 7;
    }


    public int getRows() {
        return rows;
    }
    public int getCols() {
        return cols;
    }

    public void setRows(int rows) throws IllegalArgumentException {
        if(rows < 3 || rows > 11) {
            throw new IllegalArgumentException("Rows must be between 3 and 11.");
        }
        this.rows = rows;
    }

    public void setCols(int cols) throws IllegalArgumentException {
        if(cols < 3 || cols > 11) {
            throw new IllegalArgumentException("Columns must be between 3 and 11.");
        }
        this.cols = cols;
    }
}
