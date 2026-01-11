package com.uni.gamesever.classes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.uni.gamesever.domain.game.GameManager;
import com.uni.gamesever.domain.game.PlayerManager;
import com.uni.gamesever.domain.model.BoardSize;
import com.uni.gamesever.domain.model.GameBoard;
import com.uni.gamesever.domain.model.PlayerInfo;
import com.uni.gamesever.domain.model.PlayerState;
import com.uni.gamesever.domain.model.Treasure;
import com.uni.gamesever.services.SocketMessageService;

public class PlayerStateTest {

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
