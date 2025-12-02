package com.uni.gamesever.classes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.uni.gamesever.exceptions.NotEnoughPlayerException;
import com.uni.gamesever.exceptions.PlayerNotAdminException;
import com.uni.gamesever.models.BoardSize;
import com.uni.gamesever.models.GameBoard;
import com.uni.gamesever.models.PlayerInfo;
import com.uni.gamesever.models.Tile;
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

    @Test
    void gameBoardHandler_shouldStartGameSuccessfully() throws Exception {
        String userId = "adminUser";
        BoardSize size = new BoardSize();
        PlayerInfo[] players = new PlayerInfo[2];
        players[0] = new PlayerInfo("user1");
        players[1] = new PlayerInfo("user2");

        when(playerManager.getAdminID()).thenReturn(userId);
        when(playerManager.getAmountOfPlayers()).thenReturn(2);
        when(playerManager.getPlayers()).thenReturn(players);

        boolean result = gameInitialitionController.handleStartGameMessage(userId, size);

        assertEquals(true, result);
        verify(playerManager).initializePlayerStates(any());
        verify(socketBroadcastService, times(2)).broadcastMessage(anyString());
    }

    @Test
    void generateBoard_shouldHaveCorrectSize() {
        BoardSize size = new BoardSize();
        size.setRows(10);
        size.setCols(10);
        GameBoard board = GameBoard.generateBoard(size);

        assertEquals(10, board.getTiles().length);
        assertEquals(10, board.getTiles()[0].length);
    }

    @Test
    void generateBoard_shouldSetAllTilesCorrectly() {
        BoardSize size = new BoardSize();
        size.setRows(7);
        size.setCols(7);
        GameBoard board = GameBoard.generateBoard(size);

        int rows = size.getRows();
        int cols = size.getCols();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Tile t = board.getTiles()[i][j];
            
                assertNotNull(t, "Tile at (" + i + "," + j + ") should not be null");

                for (String e : t.getEntrances()) {
                    assertTrue(List.of("UP", "DOWN", "LEFT", "RIGHT").contains(e),
                        "Invalid entrance '" + e + "' at (" + i + "," + j + ")");
                }

                if ((i == 0 && j == 0) || (i == 0 && j == cols - 1) ||
                    (i == rows - 1 && j == 0) || (i == rows - 1 && j == cols - 1)) {
                        assertEquals("CORNER", t.getType(), "Corner type expected at (" + i + "," + j + ")");
                } 
                else if (i % 2 == 0 && j % 2 == 0 && (i == 0 || j == 0 || i == rows - 1 || j == cols - 1)) {
                    assertEquals("CROSS", t.getType(), "Edge cross expected at (" + i + "," + j + ")");
                } 
                else if (i % 2 == 0 && j % 2 == 0) {
                    assertEquals("CROSS", t.getType(), "Inner cross expected at (" + i + "," + j + ")");
                } 
                else {
                    assertTrue(List.of("STRAIGHT","CROSS","CORNER").contains(t.getType()), 
                        "Valid type expected at (" + i + "," + j + ")");
                }
            }
        }
    }
}