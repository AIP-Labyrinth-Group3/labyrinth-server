package com.uni.gamesever.models;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class PlayerState {
    private PlayerInfo player;
    private Coordinates currentPosition;
    private Coordinates homePosition;
    private List<Treasure> treasuresFound;
    private Treasure currentTreasure;
    private int remainingTreasureCount;
    private String[] achievements;
    private String[] availableBonuses;
    @JsonIgnore
    private List<Treasure> assignedTreasures;

    public PlayerState(PlayerInfo player, Coordinates currentPosition, Coordinates homePosition,
            Treasure currentTreasure, int remainingTreasureCount) {
        this.player = player;
        this.currentPosition = currentPosition;
        this.homePosition = homePosition;
        this.treasuresFound = new ArrayList<>();
        this.currentTreasure = currentTreasure;
        this.remainingTreasureCount = remainingTreasureCount;
        this.achievements = new String[0];
        this.availableBonuses = new String[0];
    }

    public Coordinates getCurrentPosition() {
        return currentPosition;
    }

    public List<Treasure> getTreasuresFound() {
        return treasuresFound;
    }

    public String[] getAchievements() {
        return achievements;
    }

    public PlayerInfo getPlayer() {
        return player;
    }

    public Coordinates getHomePosition() {
        return homePosition;
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

    public List<Treasure> getAssignedTreasures() {
        return assignedTreasures;
    }

    public void setCurrentPosition(Coordinates currentPosition) {
        this.currentPosition = currentPosition;
    }

    public void setTreasuresFound(List<Treasure> treasuresFound) {
        this.treasuresFound = treasuresFound;
    }

    public void setAchievements(String[] achievements) {
        if (achievements == null) {
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

    public void setHomePosition(Coordinates homePosition) {
        this.homePosition = homePosition;
    }

    public void setCurrentTreasure(Treasure currentTreasure) {
        this.currentTreasure = currentTreasure;
    }

    public void setRemainingTreasureCount(int remainingTreasureCount) {
        this.remainingTreasureCount = remainingTreasureCount;
    }

    public void setAssignedTreasures(List<Treasure> assignedTreasures) {
        this.assignedTreasures = assignedTreasures;
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
                throw new IllegalArgumentException(
                        "Invalid bonus type: " + bonus + ". Valid types are: BEAM, PUSH_FIXED, SWAP, PUSH_TWICE");
            }
        }
        this.availableBonuses = availableBonuses;
    }

    public void collectCurrentTreasure() throws IllegalStateException {
        if (currentTreasure == null) {
            throw new IllegalStateException("No current treasure to collect.");
        }
        treasuresFound.add(currentTreasure);
        remainingTreasureCount--;

        if (!assignedTreasures.isEmpty()) {
            assignedTreasures.remove(currentTreasure);
        }

        if (remainingTreasureCount > 0 && assignedTreasures != null && !assignedTreasures.isEmpty()) {
            currentTreasure = assignedTreasures.get(0);
        } else {
            currentTreasure = null;
        }
    }

}
