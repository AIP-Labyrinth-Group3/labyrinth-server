package com.uni.gamesever.classes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.uni.gamesever.exceptions.NotEnoughPlayerException;
import com.uni.gamesever.exceptions.PlayerNotAdminException;
import com.uni.gamesever.models.AchievementType;
import com.uni.gamesever.models.BoardSize;
import com.uni.gamesever.models.BonusType;
import com.uni.gamesever.models.Coordinates;
import com.uni.gamesever.models.DirectionType;
import com.uni.gamesever.models.GameBoard;
import com.uni.gamesever.models.PlayerInfo;
import com.uni.gamesever.models.PlayerState;
import com.uni.gamesever.models.Tile;
import com.uni.gamesever.models.Treasure;
import com.uni.gamesever.models.TurnState;
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

        PlayerState[] states = new PlayerState[2];
        states[0] = new PlayerState(players[0], null, null, null, null, 0, 0);
        states[1] = new PlayerState(players[1], null, null, null, null, 0, 0);

        when(playerManager.getAdminID()).thenReturn(userId);
        when(playerManager.getAmountOfPlayers()).thenReturn(2);
        when(playerManager.getPlayers()).thenReturn(players);

        when(playerManager.getPlayerStates()).thenReturn(states);

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
    @Test
    void bonus_shouldThrowExceptionOnInvalidType() {
        assertThrows(IllegalArgumentException.class, () -> {
            BonusType.valueOf("INVALID_BONUS");
        });
    }
    @Test
    void gameState_shouldThrowExceptionOnInvalidTurnState() {
        assertThrows(IllegalArgumentException.class, () -> {
            TurnState.valueOf("INVALID_STATE");
        });
    }
    @Test
    void playerState_shouldThrowExceptionOnInvalidBonusType() {
        assertThrows(IllegalArgumentException.class, () -> {
            BonusType.valueOf("UNKNOWN_BONUS");
        });
    }
    @Test
    void playerState_shouldThrowExceptionOnInvalidAchievementType() {
        assertThrows(IllegalArgumentException.class, () -> {
            AchievementType.valueOf("INVALID_ACHIEVEMENT");
        });
    }
    @Test
    void pushAction_shouldThrowExceptionOnInvalidDirection() {
        assertThrows(IllegalArgumentException.class, () -> {
            DirectionType.valueOf("SIDEWAYS");
        });
    }   
    @Test
    void generateBoard_shouldSetFixedTilesCorrectly() {
        BoardSize size = new BoardSize();
        size.setRows(7);
        size.setCols(7);

        GameBoard board = GameBoard.generateBoard(size);

        Tile[][] tiles = board.getTiles();
        int rows = size.getRows();
        int cols = size.getCols();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Tile t = tiles[i][j];
                assertNotNull(t, "Tile should not be null at (" + i + "," + j + ")");

                boolean isCorner =
                        (i == 0 && j == 0) ||
                        (i == 0 && j == cols - 1) ||
                        (i == rows - 1 && j == 0) ||
                        (i == rows - 1 && j == cols - 1);

                boolean isEvenEven = (i % 2 == 0 && j % 2 == 0);
                boolean isEdge = (i == 0 || j == 0 || i == rows - 1 || j == cols - 1);

                if (isCorner) {
                    assertTrue(t.isFixed(), "Corner tile at (" + i + "," + j + ") must be fixed");
                } else if (isEvenEven && isEdge) {
                    assertTrue(t.isFixed(), "Edge even-even tile at (" + i + "," + j + ") must be fixed");
                } else {
                    if ("CROSS".equals(t.getType()) || "CORNER".equals(t.getType()) || "STRAIGHT".equals(t.getType())) {
                        assertFalse(t.isFixed(), "Tile at (" + i + "," + j + ") should not be fixed");
                    }
                }
            }
        }
    }
    @Test
    void generateBoard_shouldHaveAllTilesNonNull() {
        BoardSize size = new BoardSize();
        size.setRows(7);
        size.setCols(7);

        GameBoard board = GameBoard.generateBoard(size);

        Tile[][] tiles = board.getTiles();
        for (int i = 0; i < tiles.length; i++) {
            for (int j = 0; j < tiles[i].length; j++) {
                assertNotNull(tiles[i][j], "Tile must not be null at (" + i + "," + j + ")");
            }
        }
    }
    @Test
    void generateBoard_shouldAssignEntrancesForEachTile() {
        BoardSize size = new BoardSize();
        size.setRows(7);
        size.setCols(7);

        GameBoard board = GameBoard.generateBoard(size);
        Tile[][] tiles = board.getTiles();

        for (int i = 0; i < size.getRows(); i++) {
            for (int j = 0; j < size.getCols(); j++) {
                Tile t = tiles[i][j];
                List<String> entrances = t.getEntrances();
                assertNotNull(entrances, "Entrances must not be null at (" + i + "," + j + ")");
                assertTrue(entrances.size() >= 2, "Tile at (" + i + "," + j + ") should have at least 2 entrances");
            }
        }
    }
    @Test
    void createTreasures_shouldReturn24TreasuresWithCorrectIds() {
        List<Treasure> treasures = GameInitialitionController.createTreasures();

        assertEquals(24, treasures.size(), "Should create 24 treasures");

        for (int i = 0; i < treasures.size(); i++) {
            Treasure t = treasures.get(i);
            assertEquals(i + 1, t.getId(), "Treasure ID should match its position");
            assertNotNull(t.getName(), "Treasure " + (i + 1) + " name should not be null");
        
        }
    }
    @Test
    void distributeTreasuresOnPlayers_shouldAssignTreasuresEqually() {
        PlayerInfo playerInfo1 = new PlayerInfo("player1");
        PlayerInfo playerInfo2 = new PlayerInfo("player2");

        PlayerState player1 = new PlayerState(playerInfo1, null, null, null, null, 0, 0);
        PlayerState player2 = new PlayerState(playerInfo2, null, null, null, null, 0, 0);
        PlayerState players[] = new PlayerState[] {player1, player2};
        
        PlayerManager mockManager = mock(PlayerManager.class);
        when(mockManager.getAmountOfPlayers()).thenReturn(players.length);
        when(mockManager.getPlayerStates()).thenReturn(players);

        GameInitialitionController controller = new GameInitialitionController(mockManager, null);

        List<Treasure> treasures = GameInitialitionController.createTreasures();
        controller.distributeTreasuresOnPlayers(treasures);

        for (PlayerState p : players) {
            assertNotNull(p.getCurrentTreasure(), "Current treasure should not be null");
            assertEquals(treasures.size() / players.length, p.getRemainingTreasureCount(),
                "Each player should have equal number of treasures");
            assertEquals(treasures.size() / players.length, p.getRemainingTreasureCards().size(),
                "Each player should have correct number of remaining treasure cards");
        }
    }
    @Test
    void placeTreasuresOnBoard_shouldPlaceTreasuresOnValidTiles() {
        BoardSize size = new BoardSize();
        size.setRows(10);
        size.setCols(10);
        GameBoard board = GameBoard.generateBoard(size);

        List<Treasure> treasures = GameInitialitionController.createTreasures();
        GameInitialitionController controller = new GameInitialitionController(null, null);
        controller.placeTreasuresOnBoard(board, treasures);

        Tile[][] tiles = board.getTiles();
        List<Coordinates> forbiddenStartPositions = List.of(
            new Coordinates(0, 0),
            new Coordinates(0, size.getCols() - 1),
            new Coordinates(size.getRows() - 1, 0),
            new Coordinates(size.getRows() - 1, size.getCols() - 1)
        );

        int placedCount = 0;
        for (int i = 0; i < tiles.length; i++) {
            for (int j = 0; j < tiles[i].length; j++) {
                Tile t = tiles[i][j];
                if (t.getTreasure() != null) {
                    placedCount++;
                    for (Coordinates forbidden : forbiddenStartPositions) {
                        assertFalse(forbidden.getX() == i && forbidden.getY() == j, "No treasure on start tile");
                    }
                }
            }
        }

        assertEquals(treasures.size(), placedCount, "All treasures should be placed");
    }

    @Test
    void placeTreasuresOnBoard_shouldThrowExceptionWhenNotEnoughTiles() {
        BoardSize size = new BoardSize();
        size.setRows(3);
        size.setCols(3);
        GameBoard board = GameBoard.generateBoard(size);

        List<Treasure> treasures = new ArrayList<>();
        for (int i = 1; i <= 10; i++) treasures.add(new Treasure(i, "Treasure " + i));

        GameInitialitionController controller = new GameInitialitionController(null, null);

        assertThrows(IllegalArgumentException.class, () -> controller.placeTreasuresOnBoard(board, treasures));
    }
}