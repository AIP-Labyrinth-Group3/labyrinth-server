package com.uni.gamesever.models;

public class PlayerState {
    private PlayerInfo player;
    private Coordinates currentPosition;
    private Coordinates homeCoordinates;
    private Treasure[] treasuresFound;
    private Treasure currentTreasure;
    private int remainingTreasureCount;
    private int points;
    private String[] achievements;
    private String[] availableBonuses;

    public PlayerState(PlayerInfo player, Coordinates currentPosition, Coordinates homeCoordinates, Treasure[] treasuresFound, Treasure currentTreasure, int remainingTreasureCount, int points) {
        this.player = player;
        this.currentPosition = currentPosition;
        this.homeCoordinates = homeCoordinates;
        this.treasuresFound = treasuresFound;
        this.currentTreasure = currentTreasure;
        this.remainingTreasureCount = remainingTreasureCount;
        this.points = points;
    }
    public Coordinates getCurrentPosition() {
        return currentPosition;
    }
    public Treasure[] getTreasuresFound() {
        return treasuresFound;
    }
    public int getPoints() {
        return points;
    }
    public String[] getAchievements() {
        return achievements;
    }

    public PlayerInfo getPlayer() {
        return player;
    }

    public Coordinates getHomeCoordinates() {
        return homeCoordinates;
    }
    public Treasure getCurrentTreasure() {
        return currentTreasure;
    }
    public int getRemainingTreasureCount() {
        return remainingTreasureCount;
    }

    public String[] getAvailableBonuses() {
        return availableBonuses;
    }

    public void setCurrentPosition(Coordinates currentPosition) {
        this.currentPosition = currentPosition;
    }
    public void setTreasuresFound(Treasure[] treasuresFound) {
        this.treasuresFound = treasuresFound;
    }
    public void setPoints(int points) {
        this.points = points;
    }
    public void setAchievements(String[] achievements) {
        if(achievements == null) {
            this.achievements = null;
            return;
        }

        for (String achievement : achievements) {
            try {
                AchievementType.valueOf(achievement);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid achievement type: " + achievement);
            }
        }
        this.achievements = achievements;
    }
    public void setPlayer(PlayerInfo player) {
        this.player = player;
    }
    public void setHomeCoordinates(Coordinates homeCoordinates) {
        this.homeCoordinates = homeCoordinates;
    }
    public void setCurrentTreasure(Treasure currentTreasure) {
        this.currentTreasure = currentTreasure;
    }
    public void setRemainingTreasureCount(int remainingTreasureCount) {
        this.remainingTreasureCount = remainingTreasureCount;
    }

    public void setAvailableBonuses(String[] availableBonuses) {
        if (availableBonuses == null) {
            this.availableBonuses = null;
            return;
        }

        for (String bonus : availableBonuses) {
            try {
                BonusType.valueOf(bonus);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid bonus type: " + bonus + ". Valid types are: BEAM, PUSH_FIXED, SWAP, PUSH_TWICE");
            }
        }
        this.availableBonuses = availableBonuses;
    }
}
