package com.uni.gamesever.classes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.uni.gamesever.models.AchievementContext;
import com.uni.gamesever.models.AchievementType;
import com.uni.gamesever.models.PlayerInfo;
import com.uni.gamesever.models.PlayerState;
import com.uni.gamesever.services.SocketMessageService;

public class AchievementManagerTest {

    private AchievementManager achievementManager;

    @Mock
    private SocketMessageService socketMessageService;

    private PlayerState player;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        achievementManager = new AchievementManager(socketMessageService);

        PlayerInfo info = new PlayerInfo("player1");
        player = new PlayerState(info, null, null, null, 0);
    }

    @Test
    void AchievementManagerTest_Runner_shouldNotUnlock_ifMovedLessThan7Tiles() {
        AchievementContext ctx = new AchievementContext(
                6,
                false,
                false);

        achievementManager.check(player, ctx);

        assertFalse(player.hasAchievementOfType(AchievementType.RUNNER));
    }

    @Test
    void AchievementManagerTest_Runner_shouldUnlock_onThirdValidRun() {
        AchievementContext validRun = new AchievementContext(
                7,
                false,
                false);

        achievementManager.check(player, validRun);
        achievementManager.check(player, validRun);
        assertFalse(player.hasAchievementOfType(AchievementType.RUNNER));

        achievementManager.check(player, validRun);
        assertTrue(player.hasAchievementOfType(AchievementType.RUNNER));

        verify(socketMessageService, times(1))
                .broadcastMessage(anyString());
    }

    @Test
    void AchievementManagerTest_Runner_shouldNotUnlockTwice() {
        AchievementContext validRun = new AchievementContext(
                7,
                false,
                false);

        achievementManager.check(player, validRun);
        achievementManager.check(player, validRun);
        achievementManager.check(player, validRun);
        achievementManager.check(player, validRun);

        verify(socketMessageService, times(1))
                .broadcastMessage(anyString());
    }

    @Test
    void AchievementManagerTest_Pusher_shouldNotUnlock_withoutBeingPushedOut() {
        AchievementContext ctx = new AchievementContext(
                0,
                false,
                true);

        achievementManager.check(player, ctx);

        assertFalse(player.hasAchievementOfType(AchievementType.PUSHER));
    }

    @Test
    void AchievementManagerTest_Pusher_shouldNotUnlock_withoutCollectingTreasure() {
        AchievementContext ctx = new AchievementContext(
                0,
                true,
                false);

        achievementManager.check(player, ctx);

        assertFalse(player.hasAchievementOfType(AchievementType.PUSHER));
    }

    @Test
    void AchievementManagerTest_Pusher_shouldUnlock_whenPushedOutAndCollectsTreasure() {
        AchievementContext ctx = new AchievementContext(
                0,
                true,
                true);

        achievementManager.check(player, ctx);

        assertTrue(player.hasAchievementOfType(AchievementType.PUSHER));

        verify(socketMessageService, times(1))
                .broadcastMessage(anyString());
    }

    @Test
    void AchievementManagerTest_Pusher_shouldNotUnlockTwice() {
        AchievementContext ctx = new AchievementContext(
                0,
                true,
                true);

        achievementManager.check(player, ctx);
        achievementManager.check(player, ctx);

        verify(socketMessageService, times(1))
                .broadcastMessage(anyString());
    }
}
