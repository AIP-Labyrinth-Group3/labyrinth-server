package com.uni.gamesever.classes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.uni.gamesever.exceptions.NotEnoughPlayerException;
import com.uni.gamesever.exceptions.PlayerNotAdminException;
import com.uni.gamesever.models.BoardSize;
import com.uni.gamesever.models.messages.StartGameAction;
import com.uni.gamesever.services.SocketMessageService;

public class GameInitialitationTest {

    @Mock PlayerManager playerManager;
    @Mock SocketMessageService socketBroadcastService;

    @InjectMocks
    GameInitialitionController gameInitialitionController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void startGameAction_shouldHaveDefaultValues() {
        BoardSize size = new BoardSize();
        StartGameAction action = new StartGameAction(500, size);

        assertEquals(500, action.getGameDurationInSeconds(), "Game duration should be 500 seconds");
        assertEquals(7, action.getBoardSize().getRows(), "Board rows should be 7");
        assertEquals(7, action.getBoardSize().getCols(), "Board columns should be 7");
        assertEquals(24, action.getTreasureCardCount(), "Treasure card count should be 24");
        assertEquals(0, action.getTotalBonusCount(), "Total bonus count should be 0");
    }

    @Test
    void startGameAction_shouldThrowExceptionOnInvalidTreasureCount() {
        StartGameAction action = new StartGameAction();
        assertThrows(IllegalArgumentException.class,
                () -> action.setTreasureCardCount(1));
    }

    @Test
    void boardSize_shouldThrowExceptionOnInvalidRows() {
        BoardSize size = new BoardSize();
        assertThrows(IllegalArgumentException.class,
                () -> size.setRows(2));
        }

    @Test
    void boardSize_shouldThrowExceptionOnInvalidCols() {
        BoardSize size = new BoardSize();
        assertThrows(IllegalArgumentException.class,
                () -> size.setCols(12));
        }

    @Test
    void gameBoardHandler_shouldThrowExceptionIfPlayerIsNotAdmin() {
        String userId = "user1";
        BoardSize size = new BoardSize();

        when(playerManager.getAdminID()).thenReturn("adminUser");

        assertThrows(PlayerNotAdminException.class, () -> {
            gameInitialitionController.handleStartGameMessage(userId, size);
        });
    }

    @Test
    void gameBoardHandler_shouldThrowExceptionIfNotEnoughPlayers() {
        String userId = "adminUser";
        BoardSize size = new BoardSize();

        when(playerManager.getAdminID()).thenReturn("adminUser");
        when(playerManager.getAmountOfPlayers()).thenReturn(1);

        assertThrows(NotEnoughPlayerException.class, () -> {
            gameInitialitionController.handleStartGameMessage(userId, size);
        });
    }
}