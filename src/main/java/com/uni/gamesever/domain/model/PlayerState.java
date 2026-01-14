package com.uni.gamesever.domain.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.uni.gamesever.domain.enums.AchievementType;
import com.uni.gamesever.domain.enums.BonusType;

public class PlayerState {
    private PlayerInfo playerInfo;
    private Coordinates currentPosition;
    private Coordinates homePosition;
    private List<Treasure> treasuresFound;
    @JsonIgnore
    private Treasure currentTreasure;
    private int remainingTreasureCount;
    private String[] achievements;
    private String[] availableBonuses;
    @JsonIgnore
    private List<Treasure> assignedTreasures;
    @JsonIgnore
    private int runnerCounter = 0;
    @JsonIgnore
    private boolean wasPushedOutLastRound = false;
    @JsonIgnore
    private boolean hasCollectedTreasureThisTurn = false;
    @JsonIgnore
    private int stepsTakenThisTurn = 0;

    public PlayerState(PlayerInfo player, Coordinates currentPosition, Coordinates homePosition,
            Treasure currentTreasure, int remainingTreasureCount) {
        this.playerInfo = player;
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

    public PlayerInfo getPlayerInfo() {
        return playerInfo;
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

    public void setPlayerInfo(PlayerInfo player) {
        this.playerInfo = player;
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
                        "Ungültiger Bonus-Typ: " + bonus);
            }
        }
        this.availableBonuses = availableBonuses;
    }

    public void collectCurrentTreasure() throws IllegalStateException {
        if (currentTreasure == null) {
            throw new IllegalStateException("Keine Schätze zum Sammeln vorhanden.");
        }
        treasuresFound.add(currentTreasure);
        remainingTreasureCount--;
        markCollectedTreasureThisTurn();

        if (assignedTreasures != null && !assignedTreasures.isEmpty()) {
            assignedTreasures.remove(currentTreasure);
        }

        if (remainingTreasureCount > 0 && assignedTreasures != null && !assignedTreasures.isEmpty()) {
            currentTreasure = assignedTreasures.get(0);
        } else {
            currentTreasure = null;
        }
    }

    public void collectBonus(Bonus bonus) {
        List<String> bonusesList = new ArrayList<>();
        for (String b : availableBonuses) {
            bonusesList.add(b);
        }
        bonusesList.add(bonus.getType().name());
        this.availableBonuses = bonusesList.toArray(new String[0]);
    }

    public boolean hasBonusOfType(BonusType type) {
        if (availableBonuses == null) {
            return false;
        }
        for (String bonus : availableBonuses) {
            if (bonus.equals(type.name())) {
                return true;
            }
        }
        return false;
    }

    public void useOneBonusOfType(BonusType type) throws IllegalStateException {
        if (availableBonuses == null) {
            throw new IllegalStateException("Keine verfügbaren Boni zum Verwenden.");
        }
        List<String> bonusesList = new ArrayList<>();
        boolean found = false;
        for (String bonus : availableBonuses) {
            if (bonus.equals(type.name()) && !found) {
                found = true;
            } else {
                bonusesList.add(bonus);
            }
        }
        if (!found) {
            throw new IllegalStateException("Kein Bonus vom Typ " + type.name() + " zum Verwenden vorhanden.");
        }
        this.availableBonuses = bonusesList.toArray(new String[0]);
    }

    public void countRunner(int amountOfPlayerMovesThisTurn) {
        if (amountOfPlayerMovesThisTurn >= 7) {
            runnerCounter++;
        }
    }

    public int getRunnerCounter() {
        return runnerCounter;
    }

    public void markPushedOut() {
        this.wasPushedOutLastRound = true;
    }

    public void consumePushedOutFlag() {
        this.wasPushedOutLastRound = false;
    }

    public boolean getWasPushedOutLastRound() {
        return wasPushedOutLastRound;
    }

    public void markCollectedTreasureThisTurn() {
        this.hasCollectedTreasureThisTurn = true;
    }

    public void consumeCollectedTreasureFlag() {
        this.hasCollectedTreasureThisTurn = false;
    }

    public boolean getHasCollectedTreasureThisTurn() {
        return hasCollectedTreasureThisTurn;
    }

    public void setStepsTakenThisTurn(int steps) {
        this.stepsTakenThisTurn = steps;
    }

    public int getStepsTakenThisTurn() {
        return stepsTakenThisTurn;
    }

    public boolean hasAchievementOfType(AchievementType type) {
        if (achievements != null) {
            for (String achievement : achievements) {
                if (achievement.equals(type.name())) {
                    return true;
                }
            }
        }
        return false;
    }

    public void unlockAchievement(AchievementType type) {
        List<String> achievementList = new ArrayList<>();
        if (achievements != null) {
            for (String achievement : achievements) {
                achievementList.add(achievement);
            }
        }
        achievementList.add(type.name());
        this.achievements = achievementList.toArray(new String[0]);
    }

    public int getAmountOfAchievements() {
        if (achievements != null) {
            return achievements.length;
        }
        return 0;
    }
}
