package com.uni.gamesever.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class RankingEntry {
    @JsonIgnore
    private PlayerInfo playerInfo;

    private String playerId;
    private int rank;
    private int score;
    private PlayerGameStats stats;

    public RankingEntry(PlayerInfo playerInfo, int rank, int score, PlayerGameStats stats) {
        this.playerInfo = playerInfo;
        this.playerId = playerInfo.getId();
        this.rank = rank;
        this.score = score;
        this.stats = stats;
    }

    public String getPlayerId() {
        return playerId;
    }

    public int getRank() {
        return rank;
    }

    public int getScore() {
        return score;
    }

    public PlayerGameStats getStats() {
        return stats;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
