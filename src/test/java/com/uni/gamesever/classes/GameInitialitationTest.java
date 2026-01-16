package com.uni.gamesever.classes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.uni.gamesever.domain.enums.AchievementType;
import com.uni.gamesever.domain.enums.BonusType;
import com.uni.gamesever.domain.enums.DirectionType;
import com.uni.gamesever.domain.enums.TileType;
import com.uni.gamesever.domain.exceptions.NoExtraTileException;
import com.uni.gamesever.domain.exceptions.NotEnoughPlayerException;
import com.uni.gamesever.domain.exceptions.PlayerNotAdminException;
import com.uni.gamesever.domain.game.BoardItemPlacementService;
import com.uni.gamesever.domain.game.GameInitializationController;
import com.uni.gamesever.domain.game.GameManager;
import com.uni.gamesever.domain.game.GameStatsManager;
import com.uni.gamesever.domain.game.PlayerManager;
import com.uni.gamesever.domain.model.BoardSize;
import com.uni.gamesever.domain.model.GameBoard;
import com.uni.gamesever.domain.model.PlayerInfo;
import com.uni.gamesever.domain.model.PlayerState;
import com.uni.gamesever.domain.model.Tile;
import com.uni.gamesever.domain.model.Treasure;
import com.uni.gamesever.domain.model.TurnInfo;
import com.uni.gamesever.domain.model.TurnState;
import com.uni.gamesever.infrastructure.GameTimerManager;
import com.uni.gamesever.interfaces.Websocket.messages.client.StartGameRequest;
import com.uni.gamesever.services.SocketMessageService;

public class GameInitialitationTest {

    @Mock
    PlayerManager playerManager;
    @Mock
    SocketMessageService socketBroadcastService;
    @Mock
    GameManager gameManager;
    @Mock
    GameStatsManager gameStatsManager;
    @InjectMocks
    BoardItemPlacementService boardItemPlacementService;
    @Mock
    GameTimerManager gameTimerManager;
    @Mock
    TurnInfo turnInfo;

    @InjectMocks
    GameInitializationController gameInitialitionController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void startGameAction_shouldHaveDefaultValues() {
        BoardSize size = new BoardSize();
        StartGameRequest action = new StartGameRequest(500, size, 24, 0);

        assertEquals(500, action.getGameDurationInSeconds(), "Game duration should be 500 seconds");
        assertEquals(7, action.getBoardSize().getRows(), "Board rows should be 7");
        assertEquals(7, action.getBoardSize().getCols(), "Board columns should be 7");
        assertEquals(24, action.getTreasureCardCount(), "Treasure card count should be 24");
        assertEquals(0, action.getTotalBonusCount(), "Total bonus count should be 0");
    }

    @Test
    void gameBoardHandler_shouldThrowExceptionIfPlayerIsNotAdmin() {
        String userId = "user1";
        BoardSize size = new BoardSize();

        when(playerManager.getAdminID()).thenReturn("adminUser");
        when(gameManager.getTurnInfo()).thenReturn(turnInfo);
        when(turnInfo.getTurnState()).thenReturn(TurnState.NOT_STARTED);

        assertThrows(PlayerNotAdminException.class, () -> {
            gameInitialitionController.handleStartGameMessage(userId, size, 24, 0, 1);
        });
    }

    @Test
    void gameBoardHandler_shouldThrowExceptionIfNotEnoughPlayers() {
        String userId = "adminUser";
        BoardSize size = new BoardSize();

        when(playerManager.getAdminID()).thenReturn("adminUser");
        when(playerManager.getAmountOfPlayers()).thenReturn(1);
        when(gameManager.getTurnInfo()).thenReturn(turnInfo);
        when(turnInfo.getTurnState()).thenReturn(TurnState.NOT_STARTED);

        assertThrows(NotEnoughPlayerException.class, () -> {
            gameInitialitionController.handleStartGameMessage(userId, size, 24, 0, 1);
        });
    }

    @Test
    void generateBoard_shouldHaveCorrectSize() throws NoExtraTileException {
        BoardSize size = new BoardSize();
        size.setRows(10);
        size.setCols(10);
        GameBoard board = GameBoard.generateBoard(size);

        assertEquals(10, board.getTiles().length);
        assertEquals(10, board.getTiles()[0].length);
    }

    @Test
    void generateBoard_shouldSetAllTilesCorrectly() throws NoExtraTileException {
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

                for (DirectionType e : t.getEntrances()) {
                    assertTrue(
                            List.of(DirectionType.UP, DirectionType.DOWN, DirectionType.LEFT, DirectionType.RIGHT)
                                    .contains(e),
                            "Invalid entrance '" + e + "' at (" + i + "," + j + ")");
                }

                if ((i == 0 && j == 0) || (i == 0 && j == cols - 1) ||
                        (i == rows - 1 && j == 0) || (i == rows - 1 && j == cols - 1)) {
                    assertEquals(TileType.CORNER, t.getType(), "Corner type expected at (" + i + "," + j + ")");
                } else if (i % 2 == 0 && j % 2 == 0 && (i == 0 || j == 0 || i == rows - 1 || j == cols - 1)) {
                    assertEquals(TileType.CROSS, t.getType(), "Edge cross expected at (" + i + "," + j + ")");
                } else if (i % 2 == 0 && j % 2 == 0) {
                    assertEquals(TileType.CROSS, t.getType(), "Inner cross expected at (" + i + "," + j + ")");
                } else {
                    assertTrue(List.of(TileType.STRAIGHT, TileType.CROSS, TileType.CORNER).contains(t.getType()),
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
    void generateBoard_shouldSetFixedTilesCorrectly() throws NoExtraTileException {
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

                boolean isCorner = (i == 0 && j == 0) ||
                        (i == 0 && j == cols - 1) ||
                        (i == rows - 1 && j == 0) ||
                        (i == rows - 1 && j == cols - 1);

                boolean isEvenEven = (i % 2 == 0 && j % 2 == 0);

                if (isCorner || isEvenEven) {
                    assertTrue(t.getIsFixed(),
                            "Tile at (" + i + "," + j + ") should be fixed");
                } else {
                    assertFalse(t.getIsFixed(),
                            "Tile at (" + i + "," + j + ") should not be fixed");
                }

            }
        }
    }

    @Test
    void generateBoard_shouldHaveAllTilesNonNull() throws NoExtraTileException {
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
    void generateBoard_shouldAssignEntrancesForEachTile() throws NoExtraTileException {
        BoardSize size = new BoardSize();
        size.setRows(7);
        size.setCols(7);

        GameBoard board = GameBoard.generateBoard(size);
        Tile[][] tiles = board.getTiles();

        for (int i = 0; i < size.getRows(); i++) {
            for (int j = 0; j < size.getCols(); j++) {
                Tile t = tiles[i][j];
                List<DirectionType> entrances = t.getEntrances();
                assertNotNull(entrances, "Entrances must not be null at (" + i + "," + j + ")");
                assertTrue(entrances.size() >= 2, "Tile at (" + i + "," + j + ") should have at least 2 entrances");
            }
        }
    }

    @Test
    void distributeTreasuresOnPlayers_shouldAssignTreasuresEqually() {
        PlayerInfo playerInfo1 = new PlayerInfo("player1");
        PlayerInfo playerInfo2 = new PlayerInfo("player2");

        PlayerState player1 = new PlayerState(playerInfo1, null, null, null, 0);
        PlayerState player2 = new PlayerState(playerInfo2, null, null, null, 0);
        PlayerState players[] = new PlayerState[] { player1, player2 };

        PlayerManager mockManager = mock(PlayerManager.class);
        when(mockManager.getAmountOfPlayers()).thenReturn(players.length);
        when(mockManager.getNonNullPlayerStates()).thenReturn(players);

        GameInitializationController controller = new GameInitializationController(mockManager, null, null,
                gameStatsManager, boardItemPlacementService, gameTimerManager, null);

        List<Treasure> treasures = boardItemPlacementService.createTreasures(24);
        controller.distributeTreasuresOnPlayers(treasures);

        for (PlayerState p : players) {
            assertNotNull(p.getCurrentTreasure(), "Current treasure should not be null");
            assertEquals(treasures.size() / players.length, p.getRemainingTreasureCount(),
                    "Each player should have equal number of treasures");
            assertEquals(treasures.size() / players.length, p.getAssignedTreasures().size(),
                    "Each player should have correct number of remaining treasure cards");
        }
    }

    @Test
    void generateBoard_shouldHaveAllTilesAndOneExtraTile() throws NoExtraTileException {
        BoardSize size = new BoardSize();
        size.setRows(7);
        size.setCols(7);

        GameBoard board = GameBoard.generateBoard(size);

        Tile[][] tiles = board.getTiles();
        int nonNullCount = 0;
        for (int i = 0; i < size.getRows(); i++) {
            for (int j = 0; j < size.getCols(); j++) {
                Tile t = tiles[i][j];
                assertNotNull(t, "Tile must not be null at (" + i + "," + j + ")");
                nonNullCount++;
            }
        }

        assertEquals(size.getRows() * size.getCols(), nonNullCount, "All board tiles should be placed");

        Tile spareTile = board.getSpareTile();
        assertNotNull(spareTile, "Spare tile must not be null");
        boolean isOnBoard = false;
        for (Tile[] row : tiles) {
            for (Tile t : row) {
                if (t == spareTile) {
                    isOnBoard = true;
                    break;
                }
            }
        }
        assertFalse(isOnBoard, "Extra tile should not be on the board");
    }

}