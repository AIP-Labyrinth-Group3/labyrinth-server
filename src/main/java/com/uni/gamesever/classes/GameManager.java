package com.uni.gamesever.classes;

import org.springframework.stereotype.Service;

import com.uni.gamesever.models.PlayerInfo;

@Service
public class GameManager {
    PlayerManager playerManager;
    private PlayerInfo currentPlayer;

    public GameManager(PlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    public PlayerInfo getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(PlayerInfo currentPlayer) {
        this.currentPlayer = currentPlayer;
    }
}
