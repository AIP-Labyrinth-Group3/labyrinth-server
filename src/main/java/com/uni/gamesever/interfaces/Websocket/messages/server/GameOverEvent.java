package com.uni.gamesever.interfaces.Websocket.messages.server;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.uni.gamesever.domain.game.GameStatsManager;
import com.uni.gamesever.domain.model.RankingEntry;
import com.uni.gamesever.interfaces.Websocket.messages.client.Message;

public class GameOverEvent extends Message {
    private String winnerId;
    private List<RankingEntry> ranking;
    @JsonIgnore
    GameStatsManager gameStatsManager;

    public GameOverEvent(List<RankingEntry> ranking) {
        super("GAME_OVER");
        this.ranking = ranking;
        if (!ranking.isEmpty()) {
            this.winnerId = ranking.get(0).getPlayerId();
        }
    }

    public String getWinnerId() {
        return winnerId;
    }

    public List<RankingEntry> getRanking() {
        return ranking;
    }
}
