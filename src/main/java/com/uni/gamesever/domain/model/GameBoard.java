package com.uni.gamesever.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.uni.gamesever.domain.enums.DirectionType;
import com.uni.gamesever.domain.enums.TileType;
import com.uni.gamesever.domain.exceptions.NoExtraTileException;

public class GameBoard {
    private int rows;
    private int cols;
    private Tile[][] tiles;
    private PushActionInfo lastPush;
    @JsonIgnore
    private BoardSize size;
    private Tile spareTile;
    private static final int spacing = 2;

    public GameBoard(BoardSize size) {
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

    public Tile getSpareTile() {
        return spareTile;
    }

    public void setSpareTile(Tile spareTile) {
        this.spareTile = spareTile;
    }

    public Tile getTileAtCoordinate(Coordinates coordinate) {
        return tiles[coordinate.getRow()][coordinate.getColumn()];
    }

    public boolean isAnyPlayerOnTile(Coordinates coordinate, PlayerState[] playerStates) {
        for (PlayerState ps : playerStates) {
            if (ps.getCurrentPosition().getColumn() == coordinate.getColumn()
                    && ps.getCurrentPosition().getRow() == coordinate.getRow()) {
                return true;
            }
        }
        return false;
    }

    public void pushTile(int rowOrColIndex, DirectionType direction, boolean isUsingPushFixed)
            throws NoExtraTileException {
        if (spareTile == null) {
            throw new NoExtraTileException("Es gibt kein Ersatzkachel zum Einfügen.");
        }

        if (rowOrColIndex < 0
                || rowOrColIndex >= (direction == DirectionType.LEFT || direction == DirectionType.RIGHT ? rows
                        : cols)) {
            throw new IllegalArgumentException("Index außerhalb des gültigen Bereichs.");
        }

        Tile tileToBePushedOut = null;
        if (direction == DirectionType.UP)
            tileToBePushedOut = tiles[0][rowOrColIndex];
        else if (direction == DirectionType.DOWN)
            tileToBePushedOut = tiles[rows - 1][rowOrColIndex];
        else if (direction == DirectionType.LEFT)
            tileToBePushedOut = tiles[rowOrColIndex][0];
        else if (direction == DirectionType.RIGHT)
            tileToBePushedOut = tiles[rowOrColIndex][cols - 1];

        if (tileToBePushedOut == null) {
            throw new IllegalArgumentException(
                    "Keine Kachel zum Herausdrücken an dem angegebenen Index und der Richtung.");
        }

        if (tileToBePushedOut.getIsFixed() && !isUsingPushFixed) {
            throw new IllegalArgumentException("Es ist verboten, eine feste Kachel zu verschieben.");
        }

        switch (direction) {
            case UP:
                for (int r = 0; r < rows - 1; r++) {
                    tiles[r][rowOrColIndex] = tiles[r + 1][rowOrColIndex];
                }
                tiles[rows - 1][rowOrColIndex] = spareTile;
                spareTile = tileToBePushedOut;
                break;

            case DOWN:
                for (int r = rows - 1; r > 0; r--) {
                    tiles[r][rowOrColIndex] = tiles[r - 1][rowOrColIndex];
                }
                tiles[0][rowOrColIndex] = spareTile;
                spareTile = tileToBePushedOut;
                break;

            case LEFT:
                for (int c = 0; c < cols - 1; c++) {
                    tiles[rowOrColIndex][c] = tiles[rowOrColIndex][c + 1];
                }
                tiles[rowOrColIndex][cols - 1] = spareTile;
                spareTile = tileToBePushedOut;
                break;

            case RIGHT:
                for (int c = cols - 1; c > 0; c--) {
                    tiles[rowOrColIndex][c] = tiles[rowOrColIndex][c - 1];
                }
                tiles[rowOrColIndex][0] = spareTile;
                spareTile = tileToBePushedOut;
                break;

            default:
                throw new IllegalArgumentException("Keine gültige Richtung: " + direction);

        }

        if (isUsingPushFixed) {
            recomputeFixedTilesAfterPush();
            tileToBePushedOut.setisFixed(false);
        }
    }

    // Tile generation and assignment
    public static GameBoard generateBoard(BoardSize size) throws NoExtraTileException {
        GameBoard board = new GameBoard(size);
        int boardRows = size.getRows();
        int boardCols = size.getCols();

        board.rows = boardRows;
        board.cols = boardCols;

        if (boardRows > 0 && boardCols > 0) {
            board.setTile(0, 0, new Tile(List.of(DirectionType.RIGHT, DirectionType.DOWN), TileType.CORNER, true));
            board.setTile(0, boardCols - 1,
                    new Tile(List.of(DirectionType.LEFT, DirectionType.DOWN), TileType.CORNER, true));
            board.setTile(boardRows - 1, 0,
                    new Tile(List.of(DirectionType.UP, DirectionType.RIGHT), TileType.CORNER, true));
            board.setTile(boardRows - 1, boardCols - 1,
                    new Tile(List.of(DirectionType.UP, DirectionType.LEFT), TileType.CORNER, true));
        }

        for (int c = 1; c < boardCols - 1; c++) {
            if (c % 2 == 0) {
                board.setTile(0, c,
                        new Tile(generateEdgeCrossEntrances(0, c, boardRows, boardCols), TileType.CROSS, true));
                board.setTile(boardRows - 1, c,
                        new Tile(generateEdgeCrossEntrances(boardRows - 1, c, boardRows, boardCols), TileType.CROSS,
                                true));
            }
        }

        for (int r = 1; r < boardRows - 1; r++) {
            if (r % 2 == 0) {
                board.setTile(r, 0,
                        new Tile(generateEdgeCrossEntrances(r, 0, boardRows, boardCols), TileType.CROSS, true));
                board.setTile(r, boardCols - 1,
                        new Tile(generateEdgeCrossEntrances(r, boardCols - 1, boardRows, boardCols), TileType.CROSS,
                                true));
            }
        }

        for (int r = 1; r < boardRows - 1; r++) {
            for (int c = 1; c < boardCols - 1; c++) {
                if (r % spacing == 0 && c % spacing == 0) {
                    if (r == 0 || r == boardRows - 1 || c == 0 || c == boardCols - 1)
                        continue;
                    board.setTile(r, c,
                            new Tile(generateEntrancesForTypeWithRandomRotation(TileType.CROSS), TileType.CROSS, true));
                }
            }
        }
        fillRandomTiles(board);

        return board;
    }

    private static void fillRandomTiles(GameBoard board) throws NoExtraTileException {
        int boardRows = board.getSize().getRows();
        int boardCols = board.getSize().getCols();
        int totalTiles = boardRows * boardCols;
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

        for (int r = 0; r < boardRows; r++) {
            for (int c = 0; c < boardCols; c++) {
                Tile t = board.getTiles()[r][c];
                if (t != null && t.getType() != null) {
                    switch (t.getType()) {
                        case CORNER:
                            corners--;
                            break;
                        case CROSS:
                            crosses--;
                            break;
                        case STRAIGHT:
                            straights--;
                            break;
                    }
                }
            }
        }

        List<TileType> remainingTiles = new ArrayList<>();
        for (int i = 0; i < corners; i++)
            remainingTiles.add(TileType.CORNER);
        for (int i = 0; i < crosses; i++)
            remainingTiles.add(TileType.CROSS);
        for (int i = 0; i < straights; i++)
            remainingTiles.add(TileType.STRAIGHT);

        Collections.shuffle(remainingTiles);

        if (!remainingTiles.isEmpty()) {
            TileType extraTileType = remainingTiles.remove(remainingTiles.size() - 1);
            Tile extraTile = new Tile(generateEntrancesForTypeWithRandomRotation(extraTileType), extraTileType);
            board.setSpareTile(extraTile);
        } else {
            throw new NoExtraTileException("Es konnte kein Ersatzkachel generiert werden.");
        }

        int remainingTileIndex = 0;
        for (int r = 0; r < boardRows; r++) {
            for (int c = 0; c < boardCols; c++) {
                if (board.getTiles()[r][c] == null && remainingTileIndex < remainingTiles.size()) {
                    TileType type = remainingTiles.get(remainingTileIndex++);
                    Tile t = new Tile(generateEntrancesForTypeWithRandomRotation(type), type);
                    board.setTile(r, c, t);
                }
            }
        }

    }

    private static List<DirectionType> generateEdgeCrossEntrances(int row, int col, int boardRows, int boardCols) {
        List<DirectionType> entrances = new ArrayList<>();

        boolean topEdge = (row == 0);
        boolean bottomEdge = (row == boardRows - 1);
        boolean leftEdge = (col == 0);
        boolean rightEdge = (col == boardCols - 1);

        List<DirectionType> allDirs = List.of(DirectionType.UP, DirectionType.RIGHT, DirectionType.DOWN,
                DirectionType.LEFT);
        entrances.addAll(allDirs);

        if (topEdge)
            entrances.remove(DirectionType.UP);
        if (bottomEdge)
            entrances.remove(DirectionType.DOWN);
        if (leftEdge)
            entrances.remove(DirectionType.LEFT);
        if (rightEdge)
            entrances.remove(DirectionType.RIGHT);

        if (entrances.size() < 3) {
            if (topEdge)
                entrances.add(DirectionType.DOWN);
            if (bottomEdge)
                entrances.add(DirectionType.UP);
            if (leftEdge)
                entrances.add(DirectionType.RIGHT);
            if (rightEdge)
                entrances.add(DirectionType.LEFT);
        }

        return entrances;
    }

    private static List<DirectionType> generateEntrancesForTypeWithRandomRotation(TileType type) {
        Random rnd = new Random();
        switch (type) {
            case CORNER:
                List<List<DirectionType>> corners = List.of(
                        List.of(DirectionType.UP, DirectionType.RIGHT),
                        List.of(DirectionType.RIGHT, DirectionType.DOWN),
                        List.of(DirectionType.DOWN, DirectionType.LEFT),
                        List.of(DirectionType.LEFT, DirectionType.UP));
                return new ArrayList<>(corners.get(rnd.nextInt(corners.size())));

            case STRAIGHT:
                List<List<DirectionType>> straights = List.of(
                        List.of(DirectionType.UP, DirectionType.DOWN),
                        List.of(DirectionType.LEFT, DirectionType.RIGHT));
                return new ArrayList<>(straights.get(rnd.nextInt(straights.size())));

            case CROSS:
                List<List<DirectionType>> crosses = List.of(
                        List.of(DirectionType.UP, DirectionType.LEFT, DirectionType.RIGHT),
                        List.of(DirectionType.UP, DirectionType.RIGHT, DirectionType.DOWN),
                        List.of(DirectionType.RIGHT, DirectionType.DOWN, DirectionType.LEFT),
                        List.of(DirectionType.DOWN, DirectionType.LEFT, DirectionType.UP));
                return new ArrayList<>(crosses.get(rnd.nextInt(crosses.size())));

            default:
                return new ArrayList<>();
        }
    }

    public void removeTreasureFromTile(Coordinates coordinate) {
        Tile tile = getTileAtCoordinate(coordinate);
        if (tile != null) {
            tile.setTreasure(null);
        }
    }

    public void removeBonusFromTile(Coordinates coordinate) {
        Tile tile = getTileAtCoordinate(coordinate);
        if (tile != null) {
            tile.setBonus(null);
        }
    }

    public void recomputeFixedTilesAfterPush() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Tile tile = tiles[row][col];
                if (tile != null) {
                    tile.setisFixed(false);
                }
            }
        }

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Tile tile = tiles[row][col];
                if (tile == null)
                    continue;

                if (isFixedPosition(row, col)) {
                    tile.setisFixed(true);
                }
            }
        }
    }

    private boolean isFixedPosition(int boardRow, int boardCol) {
        if ((boardRow == 0 && boardCol == 0)
                || (boardRow == 0 && boardCol == cols - 1)
                || (boardRow == rows - 1 && boardCol == 0)
                || (boardRow == rows - 1 && boardCol == cols - 1)) {
            return true;
        }
        if ((boardRow == 0 || boardRow == rows - 1) && boardCol % 2 == 0) {
            return true;
        }
        if ((boardCol == 0 || boardCol == cols - 1) && boardRow % 2 == 0) {
            return true;
        }
        if (boardRow > 0 && boardRow < rows - 1
                && boardCol > 0 && boardCol < cols - 1
                && boardRow % spacing == 0
                && boardCol % spacing == 0) {
            return true;
        }

        return false;
    }

}
