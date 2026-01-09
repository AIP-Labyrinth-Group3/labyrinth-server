package com.uni.gamesever.services;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.uni.gamesever.exceptions.NoExtraTileException;
import com.uni.gamesever.models.BoardSize;
import com.uni.gamesever.models.Bonus;
import com.uni.gamesever.models.BonusType;
import com.uni.gamesever.models.GameBoard;
import com.uni.gamesever.models.Tile;
import com.uni.gamesever.models.Treasure;
import com.uni.gamesever.models.Coordinates;

public class BoardItemPlacementTest {

    private BoardItemPlacementService boardItemPlacementService;

    @BeforeEach
    void setUp() throws Throwable {
        boardItemPlacementService = new BoardItemPlacementService();
        GameBoard.generateBoard(new BoardSize());
    }

    @Test
    void createTreasures_shouldReturn24TreasuresWithCorrectIds() {
        List<Treasure> treasures = boardItemPlacementService.createTreasures(24);

        assertEquals(24, treasures.size());
        assertEquals(1, treasures.get(0).getId());
        assertEquals(24, treasures.get(23).getId());

        for (Treasure t : treasures) {
            assertNotNull(t.getName(), "Treasure name should not be null");
        }
    }

    @Test
    void createTreasures_shouldThrowExceptionForZeroOrNegative() {
        assertThrows(IllegalArgumentException.class, () -> boardItemPlacementService.createTreasures(0));
        assertThrows(IllegalArgumentException.class, () -> boardItemPlacementService.createTreasures(-5));
    }

    @Test
    void placeTreasures_shouldPlaceAllTreasuresOnValidTiles() throws NoExtraTileException {
        BoardSize size = new BoardSize();
        size.setRows(7);
        size.setCols(7);

        GameBoard board = GameBoard.generateBoard(size);
        List<Treasure> treasures = boardItemPlacementService.createTreasures(10);

        boardItemPlacementService.placeTreasures(board, treasures);

        int placedCount = 0;
        Tile[][] tiles = board.getTiles();
        List<Coordinates> forbidden = List.of(
                new Coordinates(0, 0),
                new Coordinates(0, size.getCols() - 1),
                new Coordinates(size.getRows() - 1, 0),
                new Coordinates(size.getRows() - 1, size.getCols() - 1));

        for (int i = 0; i < tiles.length; i++) {
            for (int j = 0; j < tiles[i].length; j++) {
                Tile t = tiles[i][j];
                if (t.getTreasure() != null) {
                    placedCount++;
                    for (Coordinates c : forbidden) {
                        assertFalse(c.getColumn() == i && c.getRow() == j, "No treasure on start tile");
                    }
                }
            }
        }

        assertEquals(treasures.size(), placedCount, "All treasures should be placed");
    }

    @Test
    void placeTreasures_shouldThrowExceptionWhenNotEnoughTiles() throws NoExtraTileException {
        BoardSize size = new BoardSize();
        size.setRows(3);
        size.setCols(3);

        GameBoard board = GameBoard.generateBoard(size);

        List<Treasure> treasures = new ArrayList<>();
        for (int i = 1; i <= 10; i++)
            treasures.add(new Treasure(i, "Treasure " + i));

        assertThrows(IllegalArgumentException.class,
                () -> boardItemPlacementService.placeTreasures(board, treasures));
    }

    @Test
    void getSpawnChance_shouldReturnCorrectValues() {
        assertEquals(0.4, boardItemPlacementService.getSpawnChance(0));
        assertEquals(0.4, boardItemPlacementService.getSpawnChance(1));
        assertEquals(0.33, boardItemPlacementService.getSpawnChance(2));
        assertEquals(0.33, boardItemPlacementService.getSpawnChance(3));
        assertEquals(0.0, boardItemPlacementService.getSpawnChance(4));
        assertEquals(0.0, boardItemPlacementService.getSpawnChance(5));
    }

    @Test
    void createRandomBonus_shouldReturnValidBonusType() {
        for (int i = 0; i < 10; i++) {
            Bonus bonus = boardItemPlacementService.createRandomBonus();
            assertNotNull(bonus.getType(), "BonusType sollte nicht null sein");
            assertTrue(List.of(BonusType.values()).contains(bonus.getType()));
        }
    }

}
