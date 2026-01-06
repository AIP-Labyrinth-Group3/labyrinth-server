package com.uni.gamesever.classes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import java.util.List;

import com.uni.gamesever.exceptions.GameNotValidException;
import com.uni.gamesever.exceptions.NoValidActionException;
import com.uni.gamesever.exceptions.NotPlayersTurnException;
import com.uni.gamesever.exceptions.PushNotValidException;
import com.uni.gamesever.models.BoardSize;
import com.uni.gamesever.models.Bonus;
import com.uni.gamesever.models.BonusType;
import com.uni.gamesever.models.Coordinates;
import com.uni.gamesever.models.DirectionType;
import com.uni.gamesever.models.GameBoard;
import com.uni.gamesever.models.PlayerGameStats;
import com.uni.gamesever.models.PushActionInfo;
import com.uni.gamesever.models.RankingEntry;
import com.uni.gamesever.models.PlayerInfo;
import com.uni.gamesever.models.PlayerState;
import com.uni.gamesever.models.Tile;
import com.uni.gamesever.models.TileType;
import com.uni.gamesever.models.Treasure;
import com.uni.gamesever.models.TurnState;
import com.uni.gamesever.services.BoardItemPlacementService;
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

    @Mock
    GameStatsManager gameStatsManager;

    @Mock
    BoardItemPlacementService boardItemPlacementService;

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

        when(playerManager.getCurrentPlayer()).thenReturn(player1);
        when(playerManager.getCurrentPlayerState()).thenReturn(state1);
        when(playerManager.getNonNullPlayerStates()).thenReturn(new PlayerState[] { state1, state2 });

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

        boolean result = gameManager.handlePushTile(rowOrColIndex, direction, player1.getId(), false);

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
            gameManager.handlePushTile(1, DirectionType.UP, player1.getId(), false);
        });
    }

    @Test
    void GameManagerTest_handlePushTile_shouldThrowIfNotPlayersTurn() {
        gameManager.setTurnState(TurnState.WAITING_FOR_PUSH);
        assertThrows(NotPlayersTurnException.class, () -> {
            gameManager.handlePushTile(1, DirectionType.UP, "otherPlayer", false);
        });
    }

    @Test
    void GameManagerTest_handlePushTile_shouldThrowIfOppositeDirectionRepeated() throws Exception {
        gameManager.setTurnState(TurnState.WAITING_FOR_PUSH);
        int index = 1;
        gameManager.getCurrentBoard().setLastPush(new PushActionInfo(index));
        gameManager.getCurrentBoard().getLastPush().setDirections("UP");

        assertThrows(PushNotValidException.class, () -> {
            gameManager.handlePushTile(index, DirectionType.DOWN, player1.getId(), false);
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

        board.pushTile(1, DirectionType.UP, false);

        assertEquals(topBefore, board.getExtraTile(), "Top tile becomes extra");
        assertEquals(extraBefore, board.getTiles()[board.getRows() - 1][1], "Extra tile inserted at bottom");
    }

    @Test
    void GameManagerTest_updateBoard_shouldThrowIfExtraTileNull() {
        board.setExtraTile(null);

        assertThrows(Exception.class, () -> board.pushTile(1, DirectionType.UP, false));
    }

    @Test
    void GameManagerTest_updateBoard_shouldThrowIfFixedTile() {
        board.getTiles()[0][1].setisFixed(true);
        board.setExtraTile(new Tile(List.of(DirectionType.UP, DirectionType.RIGHT), TileType.CORNER));

        assertThrows(IllegalArgumentException.class, () -> board.pushTile(1, DirectionType.UP, false));
    }

    @Test
    void GameManagerTest_updateBoard_shouldThrowIfIndexOutOfBounds() {
        board.setExtraTile(new Tile(List.of(DirectionType.UP, DirectionType.RIGHT), TileType.CORNER));

        assertThrows(IllegalArgumentException.class, () -> board.pushTile(-1, DirectionType.UP, false));
        assertThrows(IllegalArgumentException.class, () -> board.pushTile(100, DirectionType.LEFT, false));
    }

    @Test
    void GameManagerTest_canPlayerMove_shouldReturnTrueIfStartEqualsTarget() {
        Coordinates pos = new Coordinates(0, 0);

        when(playerManager.getPlayerStatesOfPlayersNotOnTurn()).thenReturn(new PlayerState[] {});

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
        Coordinates target = new Coordinates(1, 0);

        when(playerManager.getPlayerStatesOfPlayersNotOnTurn()).thenReturn(new PlayerState[] {});

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

        when(playerManager.getPlayerStatesOfPlayersNotOnTurn()).thenReturn(new PlayerState[] {});

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

        when(playerManager.getPlayerStatesOfPlayersNotOnTurn()).thenReturn(new PlayerState[] {});

        assertTrue(gameManager.canPlayerMove(board, start, target), "It should return true when a valid path exists");
    }

    @Test
    void GameManagerTest_canPlayerMove_shouldReturnFalseIfTileNull() {

        board.setTile(0, 0, null);

        Coordinates start = new Coordinates(0, 0);
        Coordinates target = new Coordinates(0, 1);

        when(playerManager.getPlayerStatesOfPlayersNotOnTurn()).thenReturn(new PlayerState[] {});

        assertFalse(gameManager.canPlayerMove(board, start, target),
                "It should return false when no valid path exists");
    }

    @Test
    void GameManagerTest_ExceptionShouldBeThrownIfTargetIsOccupiedByAnotherPlayer() {
        Coordinates start = new Coordinates(0, 0);
        Coordinates target = new Coordinates(1, 1);

        when(playerManager.getPlayerStatesOfPlayersNotOnTurn()).thenReturn(new PlayerState[] { state2 });
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

        gameManager.handlePushTile(1, DirectionType.UP, player1.getId(), false);

        assertEquals(0, p.getCurrentPosition().getRow(),
                "Player should move up by 1");
        assertEquals(1, p.getCurrentPosition().getColumn());
    }

    @Test
    void GameManagerTest_updatePlayerPositionsAfterPush_shouldWrapWhenPushedUp() throws Exception {
        int rows = board.getRows();

        PlayerState p = new PlayerState(player1, null, null, null, 0);
        p.setCurrentPosition(new Coordinates(1, 0));

        when(playerManager.getNonNullPlayerStates()).thenReturn(new PlayerState[] { p });

        board.setExtraTile(new Tile(List.of(DirectionType.UP), TileType.STRAIGHT));
        gameManager.setCurrentBoard(board);
        gameManager.setTurnState(TurnState.WAITING_FOR_PUSH);

        gameManager.handlePushTile(1, DirectionType.UP, player1.getId(), false);

        assertEquals(rows - 1, p.getCurrentPosition().getRow(),
                "Player pushed off top should reappear at bottom");
        assertEquals(1, p.getCurrentPosition().getColumn());
    }

    @Test
    void GameManagerTest_updatePlayerPositionsAfterPush_shouldMovePlayerDown() throws Exception {
        PlayerState p = new PlayerState(player1, null, null, null, 0);
        p.setCurrentPosition(new Coordinates(3, 1));

        when(playerManager.getNonNullPlayerStates()).thenReturn(new PlayerState[] { p });

        board.setExtraTile(new Tile(List.of(DirectionType.DOWN), TileType.STRAIGHT));
        gameManager.setCurrentBoard(board);
        gameManager.setTurnState(TurnState.WAITING_FOR_PUSH);

        gameManager.handlePushTile(3, DirectionType.DOWN, player1.getId(), false);

        assertEquals(2, p.getCurrentPosition().getRow());
        assertEquals(3, p.getCurrentPosition().getColumn());
    }

    @Test
    void GameManagerTest_updatePlayerPositionsAfterPush_shouldWrapWhenPushedRight() throws Exception {

        PlayerState p = new PlayerState(player1, null, null, null, 0);
        p.setCurrentPosition(new Coordinates(6, 1));

        when(playerManager.getNonNullPlayerStates()).thenReturn(new PlayerState[] { p });

        board.setExtraTile(new Tile(List.of(DirectionType.LEFT), TileType.STRAIGHT));
        gameManager.setCurrentBoard(board);
        gameManager.setTurnState(TurnState.WAITING_FOR_PUSH);

        gameManager.handlePushTile(1, DirectionType.RIGHT, player1.getId(), false);

        assertEquals(1, p.getCurrentPosition().getRow());
        assertEquals(0, p.getCurrentPosition().getColumn(),
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

        gameManager.handlePushTile(1, DirectionType.UP, player1.getId(), false);

        assertEquals(4, p.getCurrentPosition().getColumn());
        assertEquals(4, p.getCurrentPosition().getRow());
    }

    @Test
    void GameManagerTest_handleMovePawn_shouldBroadcastGameOverWhenLastTreasureCollected() throws Exception {
        gameManager.setTurnState(TurnState.WAITING_FOR_MOVE);

        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                board.setTile(r, c, null);
            }
        }

        Coordinates startPos = new Coordinates(0, 0);
        state1.setCurrentPosition(startPos);

        Treasure targetTreasure = new Treasure(1, "Treasure 1");
        state1.setCurrentTreasure(targetTreasure);

        Tile startTile = new Tile(List.of(DirectionType.DOWN), TileType.STRAIGHT);
        Tile targetTile = new Tile(List.of(DirectionType.UP), TileType.STRAIGHT);
        targetTile.setTreasure(targetTreasure);

        board.setTile(0, 0, startTile);
        board.setTile(1, 0, targetTile);

        when(playerManager.getCurrentPlayerState()).thenReturn(state1);
        when(playerManager.getCurrentPlayer()).thenReturn(player1);
        when(playerManager.getNonNullPlayerStates()).thenReturn(new PlayerState[] { state1 });
        when(playerManager.getPlayerStatesOfPlayersNotOnTurn()).thenReturn(new PlayerState[] {});

        PlayerGameStats endStats = new PlayerGameStats(10, 5, 1);
        RankingEntry finalRanking = new RankingEntry(player1, 1, 100, endStats);
        when(gameStatsManager.getSortedRankings()).thenReturn(List.of(finalRanking));

        boolean result = gameManager.handleMovePawn(new Coordinates(0, 1), player1.getId(), false);

        assertTrue(result);
        verify(gameStatsManager).increaseTreasuresCollected(1, player1.getId());
        verify(gameStatsManager).updateScoresForAllPlayers();
        verify(socketBroadcastService, atLeastOnce()).broadcastMessage(
                argThat(message -> message.contains(player1.getId()) && message.contains("rank")));

        assertEquals(TurnState.NOT_STARTED, gameManager.getTurnState());
    }

    @Test
    void handleUsePushTwice_shouldConsumeBonusAndAllowSecondPush() throws Exception {
        gameManager.setTurnState(TurnState.WAITING_FOR_PUSH);

        Bonus pushTwiceBonus = new Bonus();
        pushTwiceBonus.setType(BonusType.PUSH_TWICE);
        state1.collectBonus(pushTwiceBonus);

        boolean result = gameManager.handleUsePushTwice(player1.getId());

        assertTrue(result);
        assertFalse(state1.hasBonusOfType(BonusType.PUSH_TWICE),
                "Bonus should be consumed");
    }

    @Test
    void handleUsePushTwice_shouldThrowIfNoBonus() {
        gameManager.setTurnState(TurnState.WAITING_FOR_PUSH);

        assertThrows(NoValidActionException.class, () -> gameManager.handleUsePushTwice(player1.getId()));
    }

    @Test
    void handleUsePushFixedTile_shouldConsumeBonusAndPushFixedTile() throws Exception {
        gameManager.setTurnState(TurnState.WAITING_FOR_PUSH);

        state1.setCurrentPosition(new Coordinates(1, 1));
        state2.setCurrentPosition(new Coordinates(3, 3));

        Bonus pushFixedBonus = new Bonus();
        pushFixedBonus.setType(BonusType.PUSH_FIXED);
        state1.collectBonus(pushFixedBonus);
        board.setExtraTile(new Tile(List.of(DirectionType.UP), TileType.STRAIGHT));

        boolean result = gameManager.handleUsePushFixedTile(
                DirectionType.UP, 1, player1.getId());

        assertTrue(result);
        assertFalse(state1.hasBonusOfType(BonusType.PUSH_FIXED));
    }

    @Test
    void handleUseSwap_shouldSwapPlayerPositions() throws Exception {
        gameManager.setTurnState(TurnState.WAITING_FOR_MOVE);
        Bonus swapBonus = new Bonus();
        swapBonus.setType(BonusType.SWAP);
        state1.collectBonus(swapBonus);

        state1.setCurrentPosition(new Coordinates(1, 1));
        state2.setCurrentPosition(new Coordinates(3, 3));

        when(playerManager.getPlayerStateById(player2.getId())).thenReturn(state2);

        boolean result = gameManager.handleUseSwap(player2.getId(), player1.getId());

        assertTrue(result);
        assertEquals(state1.getCurrentPosition().getColumn(), 3);
        assertEquals(state1.getCurrentPosition().getRow(), 3);
        assertEquals(state2.getCurrentPosition().getColumn(), 1);
        assertEquals(state2.getCurrentPosition().getRow(), 1);
    }

    @Test
    void handleUseSwap_shouldThrowIfSwapWithSelf() {
        gameManager.setTurnState(TurnState.WAITING_FOR_MOVE);
        Bonus swapBonus = new Bonus();
        swapBonus.setType(BonusType.SWAP);
        state1.collectBonus(swapBonus);

        assertThrows(NoValidActionException.class, () -> gameManager.handleUseSwap(player1.getId(), player1.getId()));
    }

    @Test
    void handleUseBeam_shouldConsumeBonusAndMovePawn() throws Exception {
        gameManager.setTurnState(TurnState.WAITING_FOR_MOVE);
        Bonus beamBonus = new Bonus();
        beamBonus.setType(BonusType.BEAM);
        state1.collectBonus(beamBonus);
        state1.setCurrentPosition(new Coordinates(0, 0));

        Tile start = new Tile(List.of(DirectionType.RIGHT), TileType.STRAIGHT);
        Tile target = new Tile(List.of(DirectionType.LEFT), TileType.STRAIGHT);

        board.setTile(0, 0, start);
        board.setTile(0, 1, target);

        when(playerManager.getPlayerStatesOfPlayersNotOnTurn()).thenReturn(new PlayerState[] {});

        boolean result = gameManager.handleUseBeam(new Coordinates(1, 0), player1.getId());

        assertTrue(result);
        assertFalse(state1.hasBonusOfType(BonusType.BEAM));
    }

    @Test
    void handleUseBeam_shouldThrowIfNoBeamBonus() {
        gameManager.setTurnState(TurnState.WAITING_FOR_MOVE);

        assertThrows(NoValidActionException.class,
                () -> gameManager.handleUseBeam(new Coordinates(1, 1), player1.getId()));
    }

    @Test
    void handleMovePawn_shouldNotCollectBonusIfPlayerAlreadyHasFive() throws Exception {
        gameManager.setTurnState(TurnState.WAITING_FOR_MOVE);

        for (int i = 0; i < 5; i++) {
            Bonus bonus1 = new Bonus();
            bonus1.setType(BonusType.BEAM);
            state1.collectBonus(bonus1);
        }

        state1.setCurrentPosition(new Coordinates(0, 0));

        Tile start = new Tile(List.of(DirectionType.RIGHT), TileType.STRAIGHT);
        Tile target = new Tile(List.of(DirectionType.LEFT), TileType.STRAIGHT);
        Bonus bonus2 = new Bonus();
        bonus2.setType(BonusType.SWAP);
        target.setBonus(bonus2);

        board.setTile(0, 0, start);
        board.setTile(0, 1, target);

        when(playerManager.getPlayerStatesOfPlayersNotOnTurn()).thenReturn(new PlayerState[] {});

        gameManager.handleMovePawn(new Coordinates(1, 0), player1.getId(), false);

        assertEquals(5, state1.getAvailableBonuses().length,
                "Player should not collect more than 5 bonuses");
    }

}