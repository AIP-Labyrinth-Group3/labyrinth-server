package com.uni.gamesever.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.uni.gamesever.exceptions.NoExtraTileException;

public class GameBoard {
    private int rows;
    private int cols;
    private Tile[][] tiles;
    private PushActionInfo lastPush;
    @JsonIgnore
    private BoardSize size;
    @JsonIgnore
    private Tile extraTile;
    private static final int spacing = 2;

    private GameBoard(BoardSize size) {
        this.size = size;
        this.rows = size.getRows();
        this.cols = size.getCols();
        this.tiles = new Tile[rows][cols];
    }

    public BoardSize getSize() {
        return size;
    }

    public Tile[][] getTiles() {
        return tiles;
    }

    public void setTile(int row, int col, Tile tile) {
        this.tiles[row][col] = tile;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public PushActionInfo getLastPush() {
        return lastPush;
    }

    public void setLastPush(PushActionInfo lastPush) {
        this.lastPush = lastPush;
    }

    public Tile getExtraTile() {
        return extraTile;
    }

    public void setExtraTile(Tile extraTile) {
        this.extraTile = extraTile;
    }

    public Tile getTileAtCoordinate(Coordinates coord) {
        return tiles[coord.getX()][coord.getY()];
    }

    public boolean isPlayerOnTile(Coordinates coord, PlayerState[] playerStates) {
        for (PlayerState ps : playerStates) {
            if (ps.getCurrentPosition().getX() == coord.getX() && ps.getCurrentPosition().getY() == coord.getY()) {
                return true;
            }
        }
        return false;
    }

    public void updateBoard(int rowOrColIndex, String direction) throws NoExtraTileException {
        if (extraTile == null) {
            throw new NoExtraTileException("Extra tile is not set.");
        }

        if (rowOrColIndex < 0
                || rowOrColIndex >= (direction.equalsIgnoreCase("LEFT") || direction.equalsIgnoreCase("RIGHT") ? rows
                        : cols)) {
            throw new IllegalArgumentException("Index out of bounds.");
        }

        Tile checkTile = null;
        if (direction.equalsIgnoreCase("UP"))
            checkTile = tiles[0][rowOrColIndex];
        else if (direction.equalsIgnoreCase("DOWN"))
            checkTile = tiles[rows - 1][rowOrColIndex];
        else if (direction.equalsIgnoreCase("LEFT"))
            checkTile = tiles[rowOrColIndex][0];
        else if (direction.equalsIgnoreCase("RIGHT"))
            checkTile = tiles[rowOrColIndex][cols - 1];

        if (checkTile != null && checkTile.getIsFixed()) {
            throw new IllegalArgumentException("Cannot push a fixed tile.");
        }

        Tile tempTile;
        switch (direction.toUpperCase()) {
            case "UP":
                tempTile = tiles[0][rowOrColIndex];
                for (int i = 0; i < rows - 1; i++) {
                    tiles[i][rowOrColIndex] = tiles[i + 1][rowOrColIndex];
                }
                tiles[rows - 1][rowOrColIndex] = extraTile;
                extraTile = tempTile;
                break;

            case "DOWN":
                tempTile = tiles[rows - 1][rowOrColIndex];
                for (int i = rows - 1; i > 0; i--) {
                    tiles[i][rowOrColIndex] = tiles[i - 1][rowOrColIndex];
                }
                tiles[0][rowOrColIndex] = extraTile;
                extraTile = tempTile;
                break;

            case "LEFT":
                tempTile = tiles[rowOrColIndex][0];
                for (int j = 0; j < cols - 1; j++) {
                    tiles[rowOrColIndex][j] = tiles[rowOrColIndex][j + 1];
                }
                tiles[rowOrColIndex][cols - 1] = extraTile;
                extraTile = tempTile;
                break;

            case "RIGHT":
                tempTile = tiles[rowOrColIndex][cols - 1];
                for (int j = cols - 1; j > 0; j--) {
                    tiles[rowOrColIndex][j] = tiles[rowOrColIndex][j - 1];
                }
                tiles[rowOrColIndex][0] = extraTile;
                extraTile = tempTile;
                break;

            default:
                throw new IllegalArgumentException("Invalid direction: " + direction);
        }
    }

    // Tile generation and assignment
    public static GameBoard generateBoard(BoardSize size) throws NoExtraTileException {
        GameBoard board = new GameBoard(size);
        int rows = size.getRows();
        int cols = size.getCols();

        board.rows = rows;
        board.cols = cols;

        if (rows > 0 && cols > 0) {
            board.setTile(0, 0, new Tile(List.of("RIGHT", "DOWN"), "CORNER", true));
            board.setTile(0, cols - 1, new Tile(List.of("LEFT", "DOWN"), "CORNER", true));
            board.setTile(rows - 1, 0, new Tile(List.of("UP", "RIGHT"), "CORNER", true));
            board.setTile(rows - 1, cols - 1, new Tile(List.of("UP", "LEFT"), "CORNER", true));
        }

        for (int j = 1; j < cols - 1; j++) {
            if (j % 2 == 0) {
                board.setTile(0, j,
                        new Tile(generateEdgeCrossEntrances(0, j, rows, cols), "CROSS", true));
                board.setTile(rows - 1, j,
                        new Tile(generateEdgeCrossEntrances(rows - 1, j, rows, cols), "CROSS", true));
            }
        }

        for (int i = 1; i < rows - 1; i++) {
            if (i % 2 == 0) {
                board.setTile(i, 0,
                        new Tile(generateEdgeCrossEntrances(i, 0, rows, cols), "CROSS", true));
                board.setTile(i, cols - 1,
                        new Tile(generateEdgeCrossEntrances(i, cols - 1, rows, cols), "CROSS", true));
            }
        }

        for (int i = 1; i < rows - 1; i++) {
            for (int j = 1; j < cols - 1; j++) {
                if (i % spacing == 0 && j % spacing == 0) {
                    if (i == 0 || i == rows - 1 || j == 0 || j == cols - 1)
                        continue;
                    board.setTile(i, j,
                            new Tile(generateEntrancesForTypeWithRandomRotation("CROSS"), "CROSS", true));
                }
            }
        }
        fillRandomTiles(board);

        return board;
    }

    private static void fillRandomTiles(GameBoard board) throws NoExtraTileException {
        int rows = board.getSize().getRows();
        int cols = board.getSize().getCols();
        int totalTiles = rows * cols;
        int totalCards = totalTiles + 1;

        int baseRows = 7;
        int baseCols = 7;
        int baseCorners = 20;
        int baseCrosses = 18;
        int baseStraights = 12;

        int corners = totalCards * baseCorners / (baseRows * baseCols + 1);
        int crosses = totalCards * baseCrosses / (baseRows * baseCols + 1);
        int straights = totalCards * baseStraights / (baseRows * baseCols + 1);

        int sum = corners + crosses + straights;
        int diff = totalCards - sum;
        if (diff > 0)
            straights += diff;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Tile t = board.getTiles()[i][j];
                if (t != null && t.getType() != null) {
                    switch (t.getType()) {
                        case "CORNER":
                            corners--;
                            break;
                        case "CROSS":
                            crosses--;
                            break;
                        case "STRAIGHT":
                            straights--;
                            break;
                    }
                }
            }
        }

        List<String> remainingTiles = new ArrayList<>();
        for (int i = 0; i < corners; i++)
            remainingTiles.add("CORNER");
        for (int i = 0; i < crosses; i++)
            remainingTiles.add("CROSS");
        for (int i = 0; i < straights; i++)
            remainingTiles.add("STRAIGHT");

        Collections.shuffle(remainingTiles);

        if (!remainingTiles.isEmpty()) {
            String extraTileType = remainingTiles.remove(remainingTiles.size() - 1);
            Tile extraTile = new Tile(generateEntrancesForTypeWithRandomRotation(extraTileType), extraTileType);
            board.setExtraTile(extraTile);
        } else {
            throw new NoExtraTileException("No tiles available to assign as extra tile.");
        }

        int index = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (board.getTiles()[i][j] == null && index < remainingTiles.size()) {
                    String type = remainingTiles.get(index++);
                    Tile t = new Tile(generateEntrancesForTypeWithRandomRotation(type), type);
                    board.setTile(i, j, t);
                }
            }
        }

    }

    private static List<String> generateEdgeCrossEntrances(int row, int col, int rows, int cols) {
        List<String> entrances = new ArrayList<>();

        boolean topEdge = (row == 0);
        boolean bottomEdge = (row == rows - 1);
        boolean leftEdge = (col == 0);
        boolean rightEdge = (col == cols - 1);

        List<String> allDirs = List.of("UP", "RIGHT", "DOWN", "LEFT");
        entrances.addAll(allDirs);

        if (topEdge)
            entrances.remove("UP");
        if (bottomEdge)
            entrances.remove("DOWN");
        if (leftEdge)
            entrances.remove("LEFT");
        if (rightEdge)
            entrances.remove("RIGHT");

        if (entrances.size() < 3) {
            if (topEdge)
                entrances.add("DOWN");
            if (bottomEdge)
                entrances.add("UP");
            if (leftEdge)
                entrances.add("RIGHT");
            if (rightEdge)
                entrances.add("LEFT");
        }

        return entrances;
    }

    private static List<String> generateEntrancesForTypeWithRandomRotation(String type) {
        Random rnd = new Random();
        switch (type) {
            case "CORNER":
                List<List<String>> corners = List.of(
                        List.of("UP", "RIGHT"),
                        List.of("RIGHT", "DOWN"),
                        List.of("DOWN", "LEFT"),
                        List.of("LEFT", "UP"));
                return new ArrayList<>(corners.get(rnd.nextInt(corners.size())));

            case "STRAIGHT":
                List<List<String>> straights = List.of(
                        List.of("UP", "DOWN"),
                        List.of("LEFT", "RIGHT"));
                return new ArrayList<>(straights.get(rnd.nextInt(straights.size())));

            case "CROSS":
                List<List<String>> crosses = List.of(
                        List.of("UP", "LEFT", "RIGHT"),
                        List.of("UP", "RIGHT", "DOWN"),
                        List.of("RIGHT", "DOWN", "LEFT"),
                        List.of("DOWN", "LEFT", "UP"));
                return new ArrayList<>(crosses.get(rnd.nextInt(crosses.size())));

            default:
                return new ArrayList<>();
        }
    }

}
