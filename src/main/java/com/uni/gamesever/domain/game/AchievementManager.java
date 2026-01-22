package com.uni.gamesever.domain.game;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.gamesever.domain.enums.AchievementType;
import com.uni.gamesever.domain.model.AchievementContext;
import com.uni.gamesever.domain.model.PlayerState;
import com.uni.gamesever.interfaces.Websocket.ObjectMapperSingleton;
import com.uni.gamesever.interfaces.Websocket.messages.server.AchievementEvent;
import com.uni.gamesever.services.SocketMessageService;

@Service
public class AchievementManager {

    SocketMessageService socketMessageService;
    private final ObjectMapper objectMapper = ObjectMapperSingleton.getInstance();
    private final PlayerManager playerManager;
    private boolean runnerAchievementUnlocked = false;
    private boolean pusherAchievementUnlocked = false;

    public AchievementManager(SocketMessageService socketMessageService, PlayerManager playerManager) {
        this.socketMessageService = socketMessageService;
        this.playerManager = playerManager;
    }

    public void check(PlayerState player, AchievementContext ctx) {
        checkRunner(player, ctx);
        checkPusher(player, ctx);
    }

    private void checkRunner(PlayerState player, AchievementContext ctx) {
        if (runnerAchievementUnlocked)
            return;

        if (ctx.amountOfTilesPlayerMovedOverThisTurn() >= 7) {
            player.countRunner(ctx.amountOfTilesPlayerMovedOverThisTurn());

            if (player.getRunnerCounter() >= 3) {
                try {
                    unlock(player, AchievementType.RUNNER);
                } catch (JsonProcessingException e) {
                    System.err.println("Failed to unlock achievement: " + e.getMessage());
                }
            }
        }
    }

    private void checkPusher(PlayerState player, AchievementContext ctx) {
        if (pusherAchievementUnlocked)
            return;

        if (ctx.wasPushedOutLastRound()
                && ctx.collectedTreasure()) {
            try {
                unlock(player, AchievementType.PUSHER);
            } catch (JsonProcessingException e) {
                System.err.println("Failed to unlock achievement: " + e.getMessage());
            }
        }
    }

    private void unlock(PlayerState player, AchievementType achievement) throws JsonProcessingException {
        player.unlockAchievement(achievement);
        if (achievement == AchievementType.RUNNER) {
            runnerAchievementUnlocked = true;
        } else if (achievement == AchievementType.PUSHER) {
            pusherAchievementUnlocked = true;
        }
    }

    public void broadCastAchievementsFromPlayerWithId(String playerId) throws JsonProcessingException {
        PlayerState playerState = playerManager.getPlayerStateById(playerId);

        for (String achievement : playerState.getAchievements()) {
            AchievementEvent achievementEvent = new AchievementEvent(playerId, AchievementType.valueOf(achievement));
            socketMessageService.broadcastMessage(objectMapper.writeValueAsString(achievementEvent));
        }
    }
}
