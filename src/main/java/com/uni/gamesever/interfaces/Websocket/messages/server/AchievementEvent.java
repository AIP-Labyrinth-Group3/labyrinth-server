package com.uni.gamesever.interfaces.Websocket.messages.server;

import com.uni.gamesever.domain.enums.AchievementType;
import com.uni.gamesever.interfaces.Websocket.messages.client.Message;

public class AchievementEvent extends Message {
    private String playerId;
    private AchievementType achievement;

    public AchievementEvent(String playerId, AchievementType achievement) {
        super("ACHIEVEMENT_UNLOCKED");
        this.playerId = playerId;
        this.achievement = achievement;
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
