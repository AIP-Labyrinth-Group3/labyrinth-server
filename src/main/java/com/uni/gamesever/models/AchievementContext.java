package com.uni.gamesever.models;

public class AchievementContext {

    private final int amountOfTilesPlayerMovedOverThisTurn;
    private final boolean wasPushedOutLastRound;
    private final boolean collectedTreasure;

    public AchievementContext(
            int amountOfTilesPlayerMovedOverThisTurn,
            boolean wasPushedOutLastRound,
            boolean collectedTreasure) {
        this.amountOfTilesPlayerMovedOverThisTurn = amountOfTilesPlayerMovedOverThisTurn;
        this.wasPushedOutLastRound = wasPushedOutLastRound;
        this.collectedTreasure = collectedTreasure;
    }

    public int amountOfTilesPlayerMovedOverThisTurn() {
        return amountOfTilesPlayerMovedOverThisTurn;
    }

    public boolean wasPushedOutLastRound() {
        return wasPushedOutLastRound;
    }

    public boolean collectedTreasure() {
        return collectedTreasure;
    }
}
