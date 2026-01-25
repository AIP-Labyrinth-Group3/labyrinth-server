package com.uni.gamesever.domain.game;

import com.uni.gamesever.domain.model.PlayerInfo;
import com.uni.gamesever.domain.model.RankingEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class GameStatsManagerTest {

    @Mock
    private PlayerManager playerManager;

    @InjectMocks
    private GameStatsManager gameStatsManager;

    private PlayerInfo player1;
    private PlayerInfo player2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        player1 = new PlayerInfo("player1");
        player2 = new PlayerInfo("player2");

        when(playerManager.getNonNullPlayers()).thenReturn(new PlayerInfo[]{player1, player2});
        when(playerManager.getPlayers()).thenReturn(new PlayerInfo[]{player1, player2});

        gameStatsManager.initAllRankingStats(playerManager);
    }

    @Test
    void GameStatsTest_IncreaseStepsTaken_shouldCorrectlyUpdateSteps() {
        String p1Id = player1.getId();

        gameStatsManager.increaseStepsTaken(5, p1Id);
        gameStatsManager.increaseStepsTaken(3, p1Id);

        RankingEntry entry = gameStatsManager.getSortedRankings().stream().filter(e -> e.getPlayerId().equals(p1Id)).findFirst().orElseThrow();

        assertEquals(8, entry.getStats().getStepsTaken(), "Steps taken should be 8");
    }

    @Test
    void GameStatsTest_IncreaseTilesPushed_shouldCorrectlyUpdateTiles() {
        String p2Id = player2.getId();

        gameStatsManager.increaseTilesPushed(2, p2Id);

        RankingEntry entry = gameStatsManager.getSortedRankings().stream().filter(e -> e.getPlayerId().equals(p2Id)).findFirst().orElseThrow();

        assertEquals(2, entry.getStats().getTilesPushed(), "Tiles pushed should be 2");
    }

    @Test
    void GameStatsTest_UpdateScoreForEndGameForASinglePlayer_shouldCalculateCorrectScore() {
        String p1Id = player1.getId();

        gameStatsManager.increaseStepsTaken(10, p1Id);
        gameStatsManager.increaseTilesPushed(5, p1Id);
        gameStatsManager.increaseTreasuresCollected(4, p1Id);

        gameStatsManager.updateScoreForEndGameForASinglePlayer(p1Id);

        RankingEntry entry = gameStatsManager.getSortedRankings().stream().filter(e -> e.getPlayerId().equals(p1Id)).findFirst().orElseThrow();

        assertEquals(40, entry.getScore(), "Score should be 40 from treasures collected (4 treasures * 10 points)");
    }

    @Test
    void GameStatsTest_UpdateRankForAllPlayersBasedOnAmountOfTreasures_shouldSortByTreasures() {
        String p1Id = player1.getId();
        String p2Id = player2.getId();

        gameStatsManager.increaseTreasuresCollected(1, p1Id);
        gameStatsManager.increaseTreasuresCollected(5, p2Id);

        gameStatsManager.updateRankForAllPlayersBasedOnScore();

        List<RankingEntry> results = gameStatsManager.getSortedRankings();

        assertEquals(p2Id, results.get(0).getPlayerId());
        assertEquals(1, results.get(0).getRank());

        assertEquals(p1Id, results.get(1).getPlayerId());
        assertEquals(2, results.get(1).getRank());
    }

    @Test
    void GameStatsTest_FullTurnSimulation_shouldUpdateAllMetrics() {
        String p1Id = player1.getId();
        String p2Id = player2.getId();

        gameStatsManager.increaseTilesPushed(1, p1Id);
        gameStatsManager.increaseStepsTaken(3, p1Id);
        gameStatsManager.increaseTreasuresCollected(1, p1Id);

        gameStatsManager.increaseTilesPushed(2, p2Id);
        gameStatsManager.increaseStepsTaken(4, p2Id);
        gameStatsManager.increaseTreasuresCollected(0, p2Id);

        gameStatsManager.updateScoresForAllPlayersAtTheEndOfTheGame();
        gameStatsManager.updateRankForAllPlayersBasedOnScore();

        RankingEntry entry = gameStatsManager.getSortedRankings().get(0);

        assertEquals(p1Id, entry.getPlayerId());
        assertEquals(10, entry.getScore(), "Total score should be 10 from treasures collected (1 treasure * 10 points)");
        assertEquals(1, entry.getStats().getTreasuresCollected());
        assertEquals(1, entry.getRank());
    }
}