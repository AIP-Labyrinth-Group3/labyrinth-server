package com.uni.gamesever.classes;

import java.util.List;

import org.springframework.stereotype.Service;
import com.uni.gamesever.models.RankingEntry;
import com.uni.gamesever.models.PlayerGameStats;

@Service
public class GameStatsManager {
    private List<RankingEntry> rankings;
    PlayerManager playerManager;

    public GameStatsManager(PlayerManager playerManager) {
        this.playerManager = playerManager;
        this.rankings = new java.util.ArrayList<>();
    }

    public void initAllRankingStats(PlayerManager playerManager) {
        for (var player : playerManager.getNonNullPlayers()) {
            rankings.add(new RankingEntry(player, 0, 0, new PlayerGameStats(0, 0, 0)));
        }
    }

    public void updateRankForAllPlayersBasedOnAmountOfTreasures() {
        rankings.sort(
                (a, b) -> Integer.compare(b.getStats().getTreasuresCollected(), a.getStats().getTreasuresCollected()));
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setRank(i + 1);
        }
    }

    public void updateScoreForASinglePlayer(String playerId) {
        for (var entry : rankings) {
            if (entry.getPlayerId().equals(playerId)) {
                int score = entry.getStats().getTreasuresCollected() + entry.getStats().getStepsTaken()
                        + entry.getStats().getTilesPushed();
                entry.setScore(score);
                return;
            }
        }
    }

    public void updateScoresForAllPlayers() {
        for (var player : playerManager.getNonNullPlayers()) {
            updateScoreForASinglePlayer(player.getId());
        }
    }

    public void increaseStepsTaken(int steps, String playerId) {
        for (var entry : rankings) {
            if (entry.getPlayerId().equals(playerId)) {
                entry.getStats().increaseStepsTaken(steps);
                System.out.println("Increased steps for player " + playerId + " by " + steps);
                return;
            }
        }
    }

    public void increaseTilesPushed(int tiles, String playerId) {
        for (var entry : rankings) {
            if (entry.getPlayerId().equals(playerId)) {
                entry.getStats().increaseTilesPushed(tiles);
                System.out.println("Increased tiles pushed for player " + playerId + " by " + tiles);
                return;
            }
        }
    }

    public void increaseTreasuresCollected(int treasures, String playerId) {
        for (var entry : rankings) {
            if (entry.getPlayerId().equals(playerId)) {
                entry.getStats().increaseTreasuresCollected(treasures);
                return;
            }
        }
    }

    public List<RankingEntry> getSortedRankings() {
        rankings.sort(
                (a, b) -> Integer.compare(b.getStats().getTreasuresCollected(), a.getStats().getTreasuresCollected()));
        return rankings;
    }
}
