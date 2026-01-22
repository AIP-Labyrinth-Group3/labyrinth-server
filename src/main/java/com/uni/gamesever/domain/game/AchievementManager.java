package com.uni.gamesever.domain.game;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.gamesever.domain.enums.AchievementType;
import com.uni.gamesever.domain.model.PlayerInfo;
import com.uni.gamesever.interfaces.Websocket.ObjectMapperSingleton;
import com.uni.gamesever.interfaces.Websocket.messages.server.AchievementEvent;
import com.uni.gamesever.services.SocketMessageService;

@Service
public class AchievementManager {

    SocketMessageService socketMessageService;
    private final ObjectMapper objectMapper = ObjectMapperSingleton.getInstance();
    GameStatsManager gameStatsManager;
    private boolean hasBroadcastedRunnerAchievement = false;
    private boolean hasBroadcastedPusherAchievement = false;

    public AchievementManager(SocketMessageService socketMessageService,
            GameStatsManager gameStatsManager) {
        this.socketMessageService = socketMessageService;
        this.gameStatsManager = gameStatsManager;
    }

    public void unlockAllAchievements() throws JsonProcessingException {
        PlayerInfo playerWithMostSteps = gameStatsManager.getPlayerWithHighestAmountOfSteps();
        if (playerWithMostSteps != null && !hasBroadcastedRunnerAchievement) {
            AchievementEvent achievementEvent = new AchievementEvent(playerWithMostSteps.getId(),
                    AchievementType.RUNNER);
            socketMessageService.broadcastMessage(objectMapper.writeValueAsString(achievementEvent));
            hasBroadcastedRunnerAchievement = true;
        }

        PlayerInfo playerWithMostPushedTiles = gameStatsManager.getPlayerWithHighestAmountOfTilesPushed();
        if (playerWithMostPushedTiles != null && !hasBroadcastedPusherAchievement) {
            AchievementEvent achievementEvent = new AchievementEvent(playerWithMostPushedTiles.getId(),
                    AchievementType.PUSHER);
            socketMessageService.broadcastMessage(objectMapper.writeValueAsString(achievementEvent));
            hasBroadcastedPusherAchievement = true;
        }
    }
}
