package com.uni.gamesever.models;

public class AchievementEvent {
    private String type = "ACHIEVEMENT_UNLOCKED";
    private String playerId;
    private AchievementType achievement;

    public AchievementEvent(String playerId, AchievementType achievement) {
        this.playerId = playerId;
        this.achievement = achievement;
    }

    public String getType() {
        return type;
    }

    public String getPlayerId() {
        return playerId;
    }

    public AchievementType getAchievement() {
        return achievement;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public void setAchievement(AchievementType achievement) {
        this.achievement = achievement;
    }
}
