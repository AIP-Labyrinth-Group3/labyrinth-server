package com.uni.gamesever.domain.game;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.uni.gamesever.domain.model.PlayerGameStats;
import com.uni.gamesever.domain.model.RankingEntry;

@Service
public class GameStatsManager {
    private List<RankingEntry> rankings;
    PlayerManager playerManager;
    private static final Logger log = LoggerFactory.getLogger("GAME_LOG");

    public GameStatsManager(PlayerManager playerManager) {
        this.playerManager = playerManager;
        this.rankings = new java.util.ArrayList<>();
    }

    public void initAllRankingStats(PlayerManager playerManager) {
        for (var player : playerManager.getNonNullPlayers()) {
            rankings.add(new RankingEntry(player, 0, 0, new PlayerGameStats(0, 0, 0)));
        }
    }

    public void removeAllScoresAndRanks() {
        rankings.clear();
    }

    public void updateRankForAllPlayersBasedOnScore() {
        rankings.sort((a, b) -> {
            int scoreA = a.getScore();
            int scoreB = b.getScore();

            if (scoreA != scoreB) {
                return Integer.compare(scoreB, scoreA);
            }

            int treasuresA = a.getStats().getTreasuresCollected();
            int treasuresB = b.getStats().getTreasuresCollected();
            if (treasuresA != treasuresB) {
                return Integer.compare(treasuresB, treasuresA);
            }

            int stepsA = a.getStats().getStepsTaken();
            int stepsB = b.getStats().getStepsTaken();
            if (stepsA != stepsB) {
                return Integer.compare(stepsA, stepsB);
            }

            int tilesA = a.getStats().getTilesPushed();
            int tilesB = b.getStats().getTilesPushed();

            if (tilesA != tilesB) {
                return Integer.compare(tilesA, tilesB);
            }
            return 0;
        });

        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setRank(i + 1);
        }

    }

    public void addAmountToScoreForASinglePlayer(String playerId, int amount) {
        for (var entry : rankings) {
            if (entry.getPlayerId().equals(playerId)) {
                int newScore = entry.getScore() + amount;
                entry.setScore(newScore);
                return;
            }
        }
    }

    public void updateScoreForEndGameForASinglePlayer(String playerId) {
        for (var entry : rankings) {
            if (entry.getPlayerId().equals(playerId)) {
                int score = entry.getStats().getTreasuresCollected() * 10;

                entry.setScore(entry.getScore() + score);
                return;
            }
        }
    }

    public void updateScoresForAllPlayersAtTheEndOfTheGame() {
        for (var player : playerManager.getNonNullPlayers()) {
            updateScoreForEndGameForASinglePlayer(player.getId());
        }
    }

    public void increaseStepsTaken(int steps, String playerId) {
        for (var entry : rankings) {
            if (entry.getPlayerId().equals(playerId)) {
                entry.getStats().increaseStepsTaken(steps);
                System.out.println("Increased steps for player " + playerId + " by " + steps);
                log.info("Schritte für Spieler {} um {} erhöht", playerId, steps);
                return;
            }
        }
    }

    public void increaseTilesPushed(int tiles, String playerId) {
        for (var entry : rankings) {
            if (entry.getPlayerId().equals(playerId)) {
                entry.getStats().increaseTilesPushed(tiles);
                System.out.println("Increased tiles pushed for player " + playerId + " by " + tiles);
                log.info("Geschobene Kacheln für Spieler {} um {} erhöht", playerId, tiles);
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
