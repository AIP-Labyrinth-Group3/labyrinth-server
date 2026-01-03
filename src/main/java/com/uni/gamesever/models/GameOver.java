package com.uni.gamesever.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.uni.gamesever.classes.GameStatsManager;

public class GameOver {
    private String type = "GAME_OVER";
    private String winnerId;
    private List<RankingEntry> ranking;
    @JsonIgnore
    GameStatsManager gameStatsManager;

    public GameOver(List<RankingEntry> ranking) {
        this.ranking = ranking;
        if (!ranking.isEmpty()) {
            this.winnerId = ranking.get(0).getPlayerId();
        }
    }

    public String getType() {
        return type;
    }

    public String getWinnerId() {
        return winnerId;
    }

    public List<RankingEntry> getRanking() {
        return ranking;
    }
}
