package com.uni.gamesever.classes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import com.uni.gamesever.exceptions.GameNotValidException;
import com.uni.gamesever.exceptions.NotPlayersTurnException;
import com.uni.gamesever.exceptions.PushNotValidException;
import com.uni.gamesever.models.BoardSize;
import com.uni.gamesever.models.Coordinates;
import com.uni.gamesever.models.DirectionType;
import com.uni.gamesever.models.GameBoard;
import com.uni.gamesever.models.PushActionInfo;
import com.uni.gamesever.models.PlayerInfo;
import com.uni.gamesever.models.PlayerState;
import com.uni.gamesever.models.Tile;
import com.uni.gamesever.models.TileType;
import com.uni.gamesever.models.Treasure;
import com.uni.gamesever.models.TurnState;
import com.uni.gamesever.services.SocketMessageService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GameManagerTest {

    @Mock
    PlayerManager playerManager;

    @Mock
    SocketMessageService socketBroadcastService;

    @InjectMocks
    GameManager gameManager;

    PlayerInfo player1;
    PlayerInfo player2;
    PlayerState state1;
    PlayerState state2;
    GameBoard board;

    @BeforeEach
    void GameManagerTest_setup() throws Exception {
        MockitoAnnotations.openMocks(this);

        player1 = new PlayerInfo("player1");
        player2 = new PlayerInfo("player2");

        state1 = new PlayerState(player1, null, null, null, 0);
        state2 = new PlayerState(player2, null, null, null, 0);

        when(playerManager.getCurrentPlayer()).thenReturn(player1);
        when(playerManager.getNonNullPlayerStates()).thenReturn(new PlayerState[] { state1, state2 });

        board = GameBoard.generateBoard(new BoardSize());
        gameManager.setCurrentBoard(board);
    }

    @Test
    void GameManagerTest_handlePushTile_shouldUpdateBoardAndSetLastPush() throws Exception {
        int rowOrColIndex = 1;
        DirectionType direction = DirectionType.UP;
        gameManager.setTurnState(TurnState.WAITING_FOR_PUSH);
        when(playerManager.getCurrentPlayer()).thenReturn(player1);
        state1.setCurrentPosition(new Coordinates(0, 0));
        state2.setCurrentPosition(new Coordinates(1, 1));
        when(playerManager.getPlayerStates()).thenReturn(new PlayerState[] { state1, state2 });

        boolean result = gameManager.handlePushTile(rowOrColIndex, direction, player1.getId());

        PushActionInfo lastPush = gameManager.getCurrentBoard().getLastPush();

        assertTrue(result, "Push should return true");
        assertNotNull(lastPush, "Last push info should be set");
        assertEquals(rowOrColIndex, lastPush.getRowOrColIndex(), "Row/Col index should match pushed index");
        assertEquals(direction, lastPush.getDirection(), "Direction should match pushed direction");
        verify(socketBroadcastService, times(1)).broadcastMessage(anyString());
    }

    @Test
    void GameManagerTest_handlePushTile_shouldThrowIfGameInactive() {
        gameManager.setTurnState(TurnState.NOT_STARTED);

        assertThrows(GameNotValidException.class, () -> {
            gameManager.handlePushTile(1, DirectionType.UP, player1.getId());
        });
    }

    @Test
    void GameManagerTest_handlePushTile_shouldThrowIfNotPlayersTurn() {
        gameManager.setTurnState(TurnState.WAITING_FOR_PUSH);
        assertThrows(NotPlayersTurnException.class, () -> {
            gameManager.handlePushTile(1, DirectionType.UP, "otherPlayer");
        });
    }

    @Test
    void GameManagerTest_handlePushTile_shouldThrowIfOppositeDirectionRepeated() throws Exception {
        gameManager.setTurnState(TurnState.WAITING_FOR_PUSH);
        int index = 1;
        gameManager.getCurrentBoard().setLastPush(new PushActionInfo(index));
        gameManager.getCurrentBoard().getLastPush().setDirections("UP");

        assertThrows(PushNotValidException.class, () -> {
            gameManager.handlePushTile(index, DirectionType.DOWN, player1.getId());
        });
    }

    @Test
    void GameManagerTest_isOppositeDirection_shouldReturnTrueForOppositePairs() {
        assertTrue(gameManager.isOppositeDirection(DirectionType.UP, DirectionType.DOWN));
        assertTrue(gameManager.isOppositeDirection(DirectionType.DOWN, DirectionType.UP));
        assertTrue(gameManager.isOppositeDirection(DirectionType.LEFT, DirectionType.RIGHT));
        assertTrue(gameManager.isOppositeDirection(DirectionType.RIGHT, DirectionType.LEFT));
    }

    @Test
    void GameManagerTest_isOppositeDirection_shouldReturnFalseForNonOppositePairs() {
        assertFalse(gameManager.isOppositeDirection(DirectionType.UP, DirectionType.LEFT));
        assertFalse(gameManager.isOppositeDirection(DirectionType.DOWN, DirectionType.RIGHT));
        assertFalse(gameManager.isOppositeDirection(DirectionType.LEFT, DirectionType.UP));
        assertFalse(gameManager.isOppositeDirection(DirectionType.RIGHT, DirectionType.DOWN));
    }

    @Test
    void GameManagerTest_updateBoard_shouldShiftColumnUpCorrectly() throws Exception {
        board.setExtraTile(new Tile(List.of(DirectionType.UP, DirectionType.RIGHT), TileType.CORNER));
        Tile topBefore = board.getTiles()[0][1];
        Tile extraBefore = board.getExtraTile();

        board.updateBoard(1, DirectionType.UP);

        assertEquals(topBefore, board.getExtraTile(), "Top tile becomes extra");
        assertEquals(extraBefore, board.getTiles()[board.getRows() - 1][1], "Extra tile inserted at bottom");
    }

    @Test
    void GameManagerTest_updateBoard_shouldThrowIfExtraTileNull() {
        board.setExtraTile(null);

        assertThrows(Exception.class, () -> board.updateBoard(1, DirectionType.UP));
    }

    @Test
    void GameManagerTest_updateBoard_shouldThrowIfFixedTile() {
        board.getTiles()[0][1].setisFixed(true);
        board.setExtraTile(new Tile(List.of(DirectionType.UP, DirectionType.RIGHT), TileType.CORNER));

        assertThrows(IllegalArgumentException.class, () -> board.updateBoard(1, DirectionType.UP));
    }

    @Test
    void GameManagerTest_updateBoard_shouldThrowIfIndexOutOfBounds() {
        board.setExtraTile(new Tile(List.of(DirectionType.UP, DirectionType.RIGHT), TileType.CORNER));

        assertThrows(IllegalArgumentException.class, () -> board.updateBoard(-1, DirectionType.UP));
        assertThrows(IllegalArgumentException.class, () -> board.updateBoard(100, DirectionType.LEFT));
    }

    @Test
    void GameManagerTest_canPlayerMove_shouldReturnTrueIfStartEqualsTarget() {
        Coordinates pos = new Coordinates(0, 0);

        when(playerManager.getThePlayerStatesOfAllOtherPlayers()).thenReturn(new PlayerState[] {});

        assertTrue(gameManager.canPlayerMove(board, pos, pos),
                "It should return true when start and target are the same");
    }

    @Test
    void GameManagerTest_canPlayerMove_shouldReturnTrueIfPathExists() {
        Tile t0 = new Tile(List.of(DirectionType.UP, DirectionType.RIGHT), TileType.CORNER);
        Tile t1 = new Tile(List.of(DirectionType.LEFT, DirectionType.DOWN), TileType.CORNER);

        board.setTile(0, 0, t0);
        board.setTile(0, 1, t1);
        Coordinates start = new Coordinates(0, 0);
        Coordinates target = new Coordinates(0, 1);

        when(playerManager.getThePlayerStatesOfAllOtherPlayers()).thenReturn(new PlayerState[] {});

        assertTrue(gameManager.canPlayerMove(board, start, target), "It should return true when a valid path exists");
    }

    @Test
    void GameManagerTest_canPlayerMove_shouldReturnFalseIfPathBlocked() {
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                board.setTile(r, c, null);
            }
        }
        Tile t0 = new Tile(List.of(DirectionType.RIGHT, DirectionType.LEFT), TileType.STRAIGHT);
        Tile t1 = new Tile(List.of(DirectionType.UP, DirectionType.RIGHT), TileType.CORNER);

        board.setTile(0, 0, t1);
        board.setTile(1, 0, t0);

        Coordinates start = new Coordinates(0, 0);
        Coordinates target = new Coordinates(1, 0);

        when(playerManager.getThePlayerStatesOfAllOtherPlayers()).thenReturn(new PlayerState[] {});

        assertFalse(gameManager.canPlayerMove(board, start, target),
                "It should return false when no valid path exists");
    }

    @Test
    void GameManagerTest_canPlayerMove_shouldHandleMultipleSteps() {
        Tile t1 = new Tile(List.of(DirectionType.LEFT, DirectionType.RIGHT), TileType.STRAIGHT);
        Tile t2 = new Tile(List.of(DirectionType.LEFT, DirectionType.DOWN), TileType.CORNER);
        Tile t3 = new Tile(List.of(DirectionType.UP, DirectionType.DOWN), TileType.STRAIGHT);

        board.setTile(0, 0, t1);
        board.setTile(0, 1, t2);
        board.setTile(1, 1, t3);

        Coordinates start = new Coordinates(0, 0);
        Coordinates target = new Coordinates(1, 1);

        when(playerManager.getThePlayerStatesOfAllOtherPlayers()).thenReturn(new PlayerState[] {});

        assertTrue(gameManager.canPlayerMove(board, start, target), "It should return true when a valid path exists");
    }

    @Test
    void GameManagerTest_canPlayerMove_shouldReturnFalseIfTileNull() {

        board.setTile(0, 0, null);

        Coordinates start = new Coordinates(0, 0);
        Coordinates target = new Coordinates(0, 1);

        when(playerManager.getThePlayerStatesOfAllOtherPlayers()).thenReturn(new PlayerState[] {});

        assertFalse(gameManager.canPlayerMove(board, start, target),
                "It should return false when no valid path exists");
    }

    @Test
    void GameManagerTest_ExceptionShouldBeThrownIfTargetIsOccupiedByAnotherPlayer() {
        Coordinates start = new Coordinates(0, 0);
        Coordinates target = new Coordinates(1, 1);

        when(playerManager.getThePlayerStatesOfAllOtherPlayers()).thenReturn(new PlayerState[] { state2 });
        state2.setCurrentPosition(new Coordinates(1, 1));

        assertThrows(IllegalArgumentException.class, () -> {
            gameManager.canPlayerMove(board, start, target);
        });
    }

    @Test
    void GameManagerTest_updatePlayerPositionsAfterPush_shouldMovePlayerUp() throws Exception {
        PlayerState p = new PlayerState(player1, null, null, null, 0);
        p.setCurrentPosition(new Coordinates(1, 1));

        when(playerManager.getNonNullPlayerStates()).thenReturn(new PlayerState[] { p });

        gameManager.setCurrentBoard(board);
        board.setExtraTile(new Tile(List.of(DirectionType.UP), TileType.STRAIGHT));
        gameManager.setTurnState(TurnState.WAITING_FOR_PUSH);

        gameManager.handlePushTile(1, DirectionType.UP, player1.getId());

        assertEquals(0, p.getCurrentPosition().getX(),
                "Player should move up by 1");
        assertEquals(1, p.getCurrentPosition().getY());
    }

    @Test
    void GameManagerTest_updatePlayerPositionsAfterPush_shouldWrapWhenPushedUp() throws Exception {
        int rows = board.getRows();

        PlayerState p = new PlayerState(player1, null, null, null, 0);
        p.setCurrentPosition(new Coordinates(0, 1));

        when(playerManager.getNonNullPlayerStates()).thenReturn(new PlayerState[] { p });

        board.setExtraTile(new Tile(List.of(DirectionType.UP), TileType.STRAIGHT));
        gameManager.setCurrentBoard(board);
        gameManager.setTurnState(TurnState.WAITING_FOR_PUSH);

        gameManager.handlePushTile(1, DirectionType.UP, player1.getId());

        assertEquals(rows - 1, p.getCurrentPosition().getX(),
                "Player pushed off top should reappear at bottom");
        assertEquals(1, p.getCurrentPosition().getY());
    }

    @Test
    void GameManagerTest_updatePlayerPositionsAfterPush_shouldMovePlayerDown() throws Exception {
        PlayerState p = new PlayerState(player1, null, null, null, 0);
        p.setCurrentPosition(new Coordinates(1, 3));

        when(playerManager.getNonNullPlayerStates()).thenReturn(new PlayerState[] { p });

        board.setExtraTile(new Tile(List.of(DirectionType.DOWN), TileType.STRAIGHT));
        gameManager.setCurrentBoard(board);
        gameManager.setTurnState(TurnState.WAITING_FOR_PUSH);

        gameManager.handlePushTile(3, DirectionType.DOWN, player1.getId());

        assertEquals(2, p.getCurrentPosition().getX());
        assertEquals(3, p.getCurrentPosition().getY());
    }

    @Test
    void GameManagerTest_updatePlayerPositionsAfterPush_shouldWrapWhenPushedRight() throws Exception {

        PlayerState p = new PlayerState(player1, null, null, null, 0);
        p.setCurrentPosition(new Coordinates(1, 6));

        when(playerManager.getNonNullPlayerStates()).thenReturn(new PlayerState[] { p });

        board.setExtraTile(new Tile(List.of(DirectionType.LEFT), TileType.STRAIGHT));
        gameManager.setCurrentBoard(board);
        gameManager.setTurnState(TurnState.WAITING_FOR_PUSH);

        gameManager.handlePushTile(1, DirectionType.RIGHT, player1.getId());

        assertEquals(1, p.getCurrentPosition().getX());
        assertEquals(0, p.getCurrentPosition().getY(),
                "Player pushed off right should reappear at left edge");
    }

    @Test
    void GameManagerTest_updatePlayerPositionsAfterPush_shouldNotMoveUnrelatedPlayer() throws Exception {
        PlayerState p = new PlayerState(player1, null, null, null, 0);
        p.setCurrentPosition(new Coordinates(4, 4));

        when(playerManager.getNonNullPlayerStates()).thenReturn(new PlayerState[] { p });

        board.setExtraTile(new Tile(List.of(DirectionType.UP), TileType.STRAIGHT));
        gameManager.setCurrentBoard(board);
        gameManager.setTurnState(TurnState.WAITING_FOR_PUSH);

        gameManager.handlePushTile(1, DirectionType.UP, player1.getId());

        assertEquals(4, p.getCurrentPosition().getX());
        assertEquals(4, p.getCurrentPosition().getY());
    }

    @Test
    void PlayerStateTest_collectCurrentTreasure_shouldCollectTreasureAndUpdateState() {
        Treasure t1 = new Treasure(0, "T1");
        Treasure t2 = new Treasure(0, "T2");

        PlayerState p = new PlayerState(player1, null, null, t1, 2);
        p.setAssignedTreasures(new ArrayList<>(List.of(t1, t2)));

        p.collectCurrentTreasure();

        assertEquals(1, p.getTreasuresFound().size(), "Treasure should be added");
        assertEquals(t1, p.getTreasuresFound().get(0));
        assertEquals(1, p.getRemainingTreasureCount(), "Remaining count should decrease");
        assertEquals(t2, p.getCurrentTreasure(), "Next treasure should be assigned");
    }

    @Test
    void PlayerStateTest_collectCurrentTreasure_shouldThrowExceptionIfTreasureIsNull() {
        PlayerState p = new PlayerState(player1, null, null, null, 1);

        assertThrows(IllegalStateException.class, () -> p.collectCurrentTreasure());
    }

    @Test
    void PlayerStateTest_collectCurrentTreasure_shouldSetCurrentTreasureNullIfLastTreasure() {
        Treasure t1 = new Treasure(0, "T1");

        PlayerState p = new PlayerState(player1, null, null, t1, 1);
        p.setAssignedTreasures(new ArrayList<>(List.of(t1)));

        p.collectCurrentTreasure();

        assertEquals(1, p.getTreasuresFound().size());
        assertEquals(0, p.getRemainingTreasureCount());
        assertNull(p.getCurrentTreasure(), "No treasure should remain");
    }

    @Test
    void PlayerStateTest_collectCurrentTreasure_shouldHandleEmptyAssignedTreasures() {
        Treasure t1 = new Treasure(0, "T1");

        PlayerState p = new PlayerState(player1, null, null, t1, 1);
        p.setAssignedTreasures(new ArrayList<>());

        p.collectCurrentTreasure();

        assertEquals(1, p.getTreasuresFound().size());
        assertNull(p.getCurrentTreasure());
    }

    @Test
    void PlayerStateTest_collectCurrentTreasure_shouldCollectMultipleTreasuresInOrder() {
        Treasure t1 = new Treasure(0, "T1");
        Treasure t2 = new Treasure(0, "T2");
        Treasure t3 = new Treasure(0, "T3");

        PlayerState p = new PlayerState(player1, null, null, t1, 3);
        p.setAssignedTreasures(new ArrayList<>(List.of(t1, t2, t3)));

        p.collectCurrentTreasure();
        p.collectCurrentTreasure();
        p.collectCurrentTreasure();

        assertEquals(3, p.getTreasuresFound().size());
        assertEquals(0, p.getRemainingTreasureCount());
        assertNull(p.getCurrentTreasure());
    }

}