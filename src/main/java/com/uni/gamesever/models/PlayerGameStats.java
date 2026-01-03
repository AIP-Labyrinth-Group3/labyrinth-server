package com.uni.gamesever.models;

public class PlayerGameStats {
    private int stepsTaken;
    private int tilesPushed;
    private int treasuresCollected;

    public PlayerGameStats(int stepsTaken, int tilesPushed, int treasuresCollected) {
        this.stepsTaken = stepsTaken;
        this.tilesPushed = tilesPushed;
        this.treasuresCollected = treasuresCollected;
    }

    public int getStepsTaken() {
        return stepsTaken;
    }

    public int getTilesPushed() {
        return tilesPushed;
    }

    public int getTreasuresCollected() {
        return treasuresCollected;
    }

    public void increaseStepsTaken(int steps) {
        this.stepsTaken += steps;
    }

    public void increaseTilesPushed(int tiles) {
        this.tilesPushed += tiles;
    }

    public void increaseTreasuresCollected(int treasures) {
        this.treasuresCollected += treasures;
    }
}
