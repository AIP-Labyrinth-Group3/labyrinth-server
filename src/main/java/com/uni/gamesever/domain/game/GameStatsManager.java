package com.uni.gamesever.domain.game;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.uni.gamesever.domain.model.PlayerGameStats;
import com.uni.gamesever.domain.model.PlayerInfo;
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
        RankingEntry entry = findRankingEntry(playerId);
        if (entry != null) {
            int newScore = entry.getScore() + amount;
            entry.setScore(newScore);
        } else {
            log.info("⚠️  Could not find ranking entry for player " + playerId);
        }
    }

    public void updateScoreForEndGameForASinglePlayer(String playerId) {
        RankingEntry entry = findRankingEntry(playerId);
        if (entry != null) {
            int score = entry.getStats().getTreasuresCollected() * 10;
            entry.setScore(entry.getScore() + score);
        } else {
            log.info("⚠️  Could not find ranking entry for player " + playerId);
        }
    }

    public void updateScoresForAllPlayersAtTheEndOfTheGame() {
        for (var player : playerManager.getNonNullPlayers()) {
            updateScoreForEndGameForASinglePlayer(player.getId());
        }
    }

    /**
     * Helper method to find RankingEntry by playerId or identifierToken.
     * Tries playerId first, then falls back to identifierToken (for reconnected
     * players).
     */
    private RankingEntry findRankingEntry(String playerId) {
        // First try to find by current playerId
        for (var entry : rankings) {
            if (entry.getPlayerId().equals(playerId)) {
                return entry;
            }
        }

        // If not found, try to find by identifierToken (for reconnected players)
        var player = playerManager.getPlayerById(playerId);
        if (player != null && player.getIdentifierToken() != null) {
            for (var entry : rankings) {
                if (player.getIdentifierToken().equals(entry.getIdentifierToken())) {
                    log.info("📊 Found ranking by identifierToken for reconnected player: " +
                            player.getName() + " (old ID: " + entry.getPlayerId() + " → new ID: " + playerId + ")");
                    // Update the playerId in ranking to current one
                    entry.setPlayerId(playerId);
                    return entry;
                }
            }
        }

        return null;
    }

    public void increaseStepsTaken(int steps, String playerId) {
        RankingEntry entry = findRankingEntry(playerId);
        if (entry != null) {
            entry.getStats().increaseStepsTaken(steps);
            log.info("Schritte für Spieler {} um {} erhöht", playerId, steps);
        } else {
            log.info("⚠️  Could not find ranking entry for player " + playerId);
        }
    }

    public void increaseTilesPushed(int tiles, String playerId) {
        RankingEntry entry = findRankingEntry(playerId);
        if (entry != null) {
            entry.getStats().increaseTilesPushed(tiles);
            log.info("Geschobene Kacheln für Spieler {} um {} erhöht", playerId, tiles);
        } else {
            log.info("⚠️  Could not find ranking entry for player " + playerId);
        }
    }

    public void increaseTreasuresCollected(int treasures, String playerId) {
        RankingEntry entry = findRankingEntry(playerId);
        if (entry != null) {
            entry.getStats().increaseTreasuresCollected(treasures);
        } else {
            log.info("⚠️  Could not find ranking entry for player " + playerId);
        }
    }

    public List<RankingEntry> getSortedRankings() {
        rankings.sort(
                (a, b) -> Integer.compare(b.getStats().getTreasuresCollected(), a.getStats().getTreasuresCollected()));
        return rankings;
    }

    public PlayerInfo getPlayerWithHighestAmountOfSteps() {
        RankingEntry topEntry = null;
        for (var entry : rankings) {
            if (topEntry == null || entry.getStats().getStepsTaken() > topEntry.getStats().getStepsTaken()) {
                topEntry = entry;
            }
        }
        if (topEntry != null) {
            return playerManager.getPlayerById(topEntry.getPlayerId());
        }
        return null;
    }

    public PlayerInfo getPlayerWithHighestAmountOfTilesPushed() {
        RankingEntry topEntry = null;
        for (var entry : rankings) {
            if (topEntry == null || entry.getStats().getTilesPushed() > topEntry.getStats().getTilesPushed()) {
                topEntry = entry;
            }
        }
        if (topEntry != null) {
            return playerManager.getPlayerById(topEntry.getPlayerId());
        }
        return null;
    }
}
