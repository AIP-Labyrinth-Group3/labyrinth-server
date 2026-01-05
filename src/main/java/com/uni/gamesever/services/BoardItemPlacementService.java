package com.uni.gamesever.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.uni.gamesever.models.*;

@Service
public class BoardItemPlacementService {

    public int countBonusesOnBoard(GameBoard board) {
        int count = 0;
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                if (board.getTiles()[r][c].getBonus() != null) {
                    count++;
                }
            }
        }
        return count;
    }

    public void placeOneBonus(GameBoard board, Bonus bonus) {
        List<Tile> validTiles = collectValidTiles(board, false);

        if (validTiles.isEmpty()) {
            throw new IllegalArgumentException("No valid tile for bonus placement");
        }

        Collections.shuffle(validTiles);
        validTiles.get(0).setBonus(bonus);
    }

    public void placeTreasures(GameBoard board, List<Treasure> treasures) {
        List<Tile> validTiles = collectValidTiles(board, true);

        Collections.shuffle(validTiles);
        int max = Math.min(treasures.size(), validTiles.size());

        for (int i = 0; i < max; i++) {
            validTiles.get(i).setTreasure(treasures.get(i));
        }

        if (treasures.size() > validTiles.size()) {
            throw new IllegalArgumentException("Not enough tiles for treasures");
        }
    }

    public boolean trySpawnBonus(GameBoard board) {
        int bonusCount = countBonusesOnBoard(board);
        double chance = getSpawnChance(bonusCount);

        if (chance <= 0)
            return false;

        if (Math.random() > chance)
            return false;

        Bonus bonus = createRandomBonus();
        placeOneBonus(board, bonus);
        return true;
    }

    public double getSpawnChance(int bonusCount) {
        if (bonusCount >= 4)
            return 0.0;
        if (bonusCount >= 2)
            return 0.33;
        if (bonusCount < 2)
            return 0.4;
        return 0.0;
    }

    public Bonus createRandomBonus() {
        BonusType[] types = BonusType.values();
        Bonus bonus = new Bonus();
        bonus.setType(types[(int) (Math.random() * types.length)]);
        return bonus;
    }

    private List<Tile> collectValidTiles(GameBoard board, boolean allowTreasure) {
        int rows = board.getSize().getRows();
        int cols = board.getSize().getCols();
        Tile[][] tiles = board.getTiles();

        List<Tile> validTiles = new ArrayList<>();

        List<Coordinates> forbiddenStartPositions = new ArrayList<>();
        forbiddenStartPositions.add(new Coordinates(0, 0));
        forbiddenStartPositions.add(new Coordinates(0, rows - 1));
        forbiddenStartPositions.add(new Coordinates(cols - 1, 0));
        forbiddenStartPositions.add(new Coordinates(cols - 1, rows - 1));

        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            for (int colIndex = 0; colIndex < cols; colIndex++) {
                boolean isStartField = false;
                for (Coordinates start : forbiddenStartPositions) {
                    if (start.getColumn() == colIndex && start.getRow() == rowIndex) {
                        isStartField = true;
                        break;
                    }
                }
                if (!isStartField && tiles[rowIndex][colIndex].getTreasure() == null) {
                    validTiles.add(tiles[rowIndex][colIndex]);
                }
            }
        }
        return validTiles;
    }

    public List<Treasure> createTreasures(int amountOfTreasures) throws IllegalArgumentException {
        if (amountOfTreasures <= 0) {
            throw new IllegalArgumentException("Amount of treasures must be positive");
        }
        List<Treasure> treasures = new ArrayList<>();
        for (int i = 1; i <= amountOfTreasures; i++) {
            treasures.add(new Treasure(i, "Treasure " + i));
        }

        return treasures;
    }

}
