package com.uni.gamesever.domain.game;

import com.uni.gamesever.domain.enums.AchievementType;
import com.uni.gamesever.domain.model.PlayerInfo;
import com.uni.gamesever.domain.model.PlayerState;
import com.uni.gamesever.services.SocketMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

public class AchievementManagerTest {

    @Mock
    GameStatsManager gameStatsManager;
    private AchievementManager achievementManager;
    @Mock
    private SocketMessageService socketMessageService;
    private PlayerState player;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        achievementManager = new AchievementManager(socketMessageService, gameStatsManager);

        PlayerInfo info = new PlayerInfo("player1");
        player = new PlayerState(info, null, null, null, 0);
    }

    @Test
    void AchievementManagerTest_playerWithMostSteps_shouldUnlockRunnerAchievement() throws Exception {
        when(gameStatsManager.getPlayerWithHighestAmountOfSteps()).thenReturn(player.getPlayerInfo());

        achievementManager.unlockAllAchievements();

        verify(socketMessageService, times(1)).broadcastMessage(contains(AchievementType.RUNNER.name()));
    }

    @Test
    void AchievementManagerTest_playerWithMostTilesPushed_shouldUnlockPusherAchievement() throws Exception {
        when(gameStatsManager.getPlayerWithHighestAmountOfTilesPushed()).thenReturn(player.getPlayerInfo());

        achievementManager.unlockAllAchievements();

        verify(socketMessageService, times(1)).broadcastMessage(contains(AchievementType.PUSHER.name()));
    }

    @Test
    void unlockAllAchievements_twoPlayersWithSameSteps_shouldSendRunnerOnlyOnce() throws Exception {
        PlayerInfo player1 = new PlayerInfo("player1");

        when(gameStatsManager.getPlayerWithHighestAmountOfSteps()).thenReturn(player1);
        when(gameStatsManager.getPlayerWithHighestAmountOfTilesPushed()).thenReturn(null);

        achievementManager.unlockAllAchievements();

        verify(socketMessageService, times(1)).broadcastMessage(contains(AchievementType.RUNNER.name()));
    }

    @Test
    void unlockAllAchievements_twoDifferentPlayers_shouldStillSendOnlyTwoAchievements() throws Exception {
        PlayerInfo runner = new PlayerInfo("runner");
        PlayerInfo pusher = new PlayerInfo("pusher");

        when(gameStatsManager.getPlayerWithHighestAmountOfSteps()).thenReturn(runner);
        when(gameStatsManager.getPlayerWithHighestAmountOfTilesPushed()).thenReturn(pusher);

        achievementManager.unlockAllAchievements();

        verify(socketMessageService, times(2)).broadcastMessage(anyString());
    }

    @Test
    void unlockAllAchievements_noStatsAvailable_shouldSendNothing() throws Exception {
        when(gameStatsManager.getPlayerWithHighestAmountOfSteps()).thenReturn(null);
        when(gameStatsManager.getPlayerWithHighestAmountOfTilesPushed()).thenReturn(null);

        achievementManager.unlockAllAchievements();

        verify(socketMessageService, never()).broadcastMessage(anyString());
    }

    @Test
    void unlockAllAchievements_calledTwice_shouldNotDuplicateAchievements() throws Exception {
        when(gameStatsManager.getPlayerWithHighestAmountOfSteps()).thenReturn(player.getPlayerInfo());

        achievementManager.unlockAllAchievements();
        achievementManager.unlockAllAchievements();

        verify(socketMessageService, times(1)).broadcastMessage(contains(AchievementType.RUNNER.name()));
    }

    @Test
    void unlockAllAchievements_mixedTieSituations_shouldStillSendExactlyTwo() throws Exception {
        PlayerInfo p1 = new PlayerInfo("p1");
        PlayerInfo p2 = new PlayerInfo("p2");

        when(gameStatsManager.getPlayerWithHighestAmountOfSteps()).thenReturn(p1);
        when(gameStatsManager.getPlayerWithHighestAmountOfTilesPushed()).thenReturn(p2);

        achievementManager.unlockAllAchievements();

        verify(socketMessageService, times(1)).broadcastMessage(contains(AchievementType.RUNNER.name()));
        verify(socketMessageService, times(1)).broadcastMessage(contains(AchievementType.PUSHER.name()));
    }
}
