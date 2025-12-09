package com.uni.gamesever.classes;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.uni.gamesever.exceptions.GameNotValidException;
import com.uni.gamesever.exceptions.NotPlayersTurnException;
import com.uni.gamesever.models.PlayerInfo;

public class GameManagerTest {
    private GameManager gameManager;
    private PlayerManager playerManager;

    @BeforeEach
    public void setUp() {
        playerManager = Mockito.mock(PlayerManager.class);
        gameManager = new GameManager(playerManager, null);
    }

    @Test
    void handlePushTile_shouldThrowException_whenGameNotActive() {
        gameManager.setGameActive(false);
        gameManager.setCurrentPlayer(new PlayerInfo("player1"));

        assertThrows(GameNotValidException.class, () ->
            gameManager.handlePushTile(0, "LEFT", "player1")
        );
    }

    @Test
    void handlePushTile_shouldThrowException_whenNotPlayersTurn() {
        PlayerInfo current = new PlayerInfo("currentPlayer");
        gameManager.setCurrentPlayer(current);
        gameManager.setGameActive(true);

        assertThrows(NotPlayersTurnException.class, () ->
            gameManager.handlePushTile(0, "LEFT", "otherPlayer")
        );
    }

    @Test
    void handlePushTile_shouldReturnTrue_whenValidPush() throws Exception {
        PlayerInfo current = new PlayerInfo("player1");
        gameManager.setCurrentPlayer(current);
        gameManager.setGameActive(true);

        boolean result = gameManager.handlePushTile(2, "RIGHT", "player1");

        assertTrue(result);
    }

    @Test
    void handlePushTile_shouldFail_whenCurrentPlayerNull() {
        gameManager.setGameActive(true);
        gameManager.setCurrentPlayer(null);

        assertThrows(NullPointerException.class, () ->
            gameManager.handlePushTile(1, "UP", "playerX")
        );
    }
}
