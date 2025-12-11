package com.uni.gamesever.classes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.List;

import com.uni.gamesever.exceptions.GameNotValidException;
import com.uni.gamesever.exceptions.NoValidActionException;
import com.uni.gamesever.exceptions.NotPlayersTurnException;
import com.uni.gamesever.exceptions.PushNotValidException;
import com.uni.gamesever.models.BoardSize;
import com.uni.gamesever.models.Coordinates;
import com.uni.gamesever.models.GameBoard;
import com.uni.gamesever.models.PushActionInfo;
import com.uni.gamesever.models.PlayerInfo;
import com.uni.gamesever.models.PlayerState;
import com.uni.gamesever.models.Tile;
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

        state1 = new PlayerState(player1, null, null, null, null, 0);
        state2 = new PlayerState(player2, null, null, null, null, 0);

        when(playerManager.getCurrentPlayer()).thenReturn(player1);
        when(playerManager.getNonNullPlayerStates()).thenReturn(new PlayerState[] { state1, state2 });

        board = GameBoard.generateBoard(new BoardSize());
        gameManager.setCurrentBoard(board);
    }

    @Test
    void GameManagerTest_handlePushTile_shouldUpdateBoardAndSetLastPush() throws Exception {
        int rowOrColIndex = 1;
        String direction = "UP";
        gameManager.setTurnState(TurnState.WAITING_FOR_PUSH);
        when(playerManager.getCurrentPlayer()).thenReturn(player1);
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
            gameManager.handlePushTile(1, "UP", player1.getId());
        });
    }

    @Test
    void GameManagerTest_handlePushTile_shouldThrowIfNotPlayersTurn() {
        gameManager.setTurnState(TurnState.WAITING_FOR_PUSH);
        assertThrows(NotPlayersTurnException.class, () -> {
            gameManager.handlePushTile(1, "UP", "otherPlayer");
        });
    }

    @Test
    void GameManagerTest_handlePushTile_shouldThrowIfOppositeDirectionRepeated() throws Exception {
        gameManager.setTurnState(TurnState.WAITING_FOR_PUSH);
        int index = 1;
        gameManager.getCurrentBoard().setLastPush(new PushActionInfo(index));
        gameManager.getCurrentBoard().getLastPush().setDirections("UP");

        assertThrows(PushNotValidException.class, () -> {
            gameManager.handlePushTile(index, "DOWN", player1.getId());
        });
    }

    @Test
    void GameManagerTest_isOppositeDirection_shouldReturnTrueForOppositePairs() {
        assertTrue(gameManager.isOppositeDirection("UP", "DOWN"));
        assertTrue(gameManager.isOppositeDirection("DOWN", "UP"));
        assertTrue(gameManager.isOppositeDirection("LEFT", "RIGHT"));
        assertTrue(gameManager.isOppositeDirection("RIGHT", "LEFT"));
    }

    @Test
    void GameManagerTest_isOppositeDirection_shouldReturnFalseForNonOppositePairs() {
        assertFalse(gameManager.isOppositeDirection("UP", "LEFT"));
        assertFalse(gameManager.isOppositeDirection("DOWN", "RIGHT"));
        assertFalse(gameManager.isOppositeDirection("LEFT", "UP"));
        assertFalse(gameManager.isOppositeDirection("RIGHT", "DOWN"));
    }

    @Test
    void GameManagerTest_updateBoard_shouldShiftColumnUpCorrectly() throws Exception {
        board.setExtraTile(new Tile(List.of("UP", "RIGHT"), "CORNER"));
        Tile topBefore = board.getTiles()[0][1];
        Tile extraBefore = board.getExtraTile();

        board.updateBoard(1, "UP");

        assertEquals(topBefore, board.getExtraTile(), "Top tile becomes extra");
        assertEquals(extraBefore, board.getTiles()[board.getRows() - 1][1], "Extra tile inserted at bottom");
    }

    @Test
    void GameManagerTest_updateBoard_shouldThrowIfExtraTileNull() {
        board.setExtraTile(null);

        assertThrows(Exception.class, () -> board.updateBoard(1, "UP"));
    }

    @Test
    void GameManagerTest_updateBoard_shouldThrowIfFixedTile() {
        board.getTiles()[0][1].setisFixed(true);
        board.setExtraTile(new Tile(List.of("UP", "RIGHT"), "CORNER"));

        assertThrows(IllegalArgumentException.class, () -> board.updateBoard(1, "UP"));
    }

    @Test
    void GameManagerTest_updateBoard_shouldThrowIfInvalidDirection() {
        board.setExtraTile(new Tile(List.of("UP", "RIGHT"), "CORNER"));

        assertThrows(IllegalArgumentException.class, () -> board.updateBoard(1, "DIAGONAL"));
    }

    @Test
    void GameManagerTest_updateBoard_shouldThrowIfIndexOutOfBounds() {
        board.setExtraTile(new Tile(List.of("UP", "RIGHT"), "CORNER"));

        assertThrows(IllegalArgumentException.class, () -> board.updateBoard(-1, "UP"));
        assertThrows(IllegalArgumentException.class, () -> board.updateBoard(100, "LEFT"));
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
        Tile t0 = new Tile(List.of("RIGHT", "UP"), "CORNER");
        Tile t1 = new Tile(List.of("LEFT", "DOWN"), "CORNER");

        board.setTile(0, 0, t0);
        board.setTile(0, 1, t1);
        Coordinates start = new Coordinates(0, 0);
        Coordinates target = new Coordinates(0, 1);

        when(playerManager.getThePlayerStatesOfAllOtherPlayers()).thenReturn(new PlayerState[] {});

        assertTrue(gameManager.canPlayerMove(board, start, target), "It should return true when a valid path exists");
    }

    @Test
    void GameManagerTest_canPlayerMove_shouldReturnFalseIfPathBlocked() {
        Tile t0 = new Tile(List.of("RIGHT", "LEFT"), "STRAIGHT");
        Tile t1 = new Tile(List.of("UP", "RIGHT"), "CORNER");

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
        Tile t1 = new Tile(List.of("LEFT", "RIGHT"), "STRAIGHT");
        Tile t2 = new Tile(List.of("LEFT", "DOWN"), "CORNER");
        Tile t3 = new Tile(List.of("UP", "DOWN"), "STRAIGHT");

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
}
