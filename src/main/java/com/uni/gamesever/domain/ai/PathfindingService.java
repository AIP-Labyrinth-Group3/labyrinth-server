package com.uni.gamesever.domain.ai;

import com.uni.gamesever.domain.enums.DirectionType;
import com.uni.gamesever.domain.model.*;

import java.util.*;

/**
 * Pathfinding-Service der genau wie der Server BFS verwendet
 * um alle erreichbaren Felder zu finden
 */
public class PathfindingService {

    private static final Map<DirectionType, Coordinates> DIRECTION_OFFSETS = new HashMap<>();

    static {
        DIRECTION_OFFSETS.put(DirectionType.UP, new Coordinates(0, -1));
        DIRECTION_OFFSETS.put(DirectionType.DOWN, new Coordinates(0, 1));
        DIRECTION_OFFSETS.put(DirectionType.LEFT, new Coordinates(-1, 0));
        DIRECTION_OFFSETS.put(DirectionType.RIGHT, new Coordinates(1, 0));
    }

    /**
     * Findet alle erreichbaren Felder von einer Startposition
     * Verwendet BFS genau wie der Server (GameManager.canPlayerMove)
     * @param occupiedPositions Positionen anderer Spieler (dürfen nicht betreten werden)
     */
    public Set<Coordinates> findReachableFields(Tile[][] board, Coordinates start, Set<Coordinates> occupiedPositions) {
        Set<Coordinates> reachable = new HashSet<>();

        if (start == null || board == null || board.length == 0) {
            return reachable;
        }

        if (occupiedPositions == null) {
            occupiedPositions = new HashSet<>();
        }

        int boardRows = board.length;
        int boardCols = board[0].length;

        // Startposition ist immer erreichbar
        reachable.add(new Coordinates(start.getColumn(), start.getRow()));

        boolean[][] visited = new boolean[boardRows][boardCols];
        Queue<Coordinates> queue = new LinkedList<>();
        queue.add(start);
        visited[start.getRow()][start.getColumn()] = true;

        while (!queue.isEmpty()) {
            Coordinates current = queue.poll();
            int currentColumn = current.getColumn();
            int currentRow = current.getRow();

            Tile tile = getTile(board, current);
            if (tile == null) {
                continue;
            }

            // Prüfe alle Ausgänge des aktuellen Tiles
            for (DirectionType dir : tile.getEntrances()) {
                Coordinates offset = DIRECTION_OFFSETS.get(dir);
                int newColumn = currentColumn + offset.getColumn();
                int newRow = currentRow + offset.getRow();

                // Außerhalb des Boards?
                if (newColumn < 0 || newColumn >= boardCols || newRow < 0 || newRow >= boardRows) {
                    continue;
                }

                Coordinates neighbor = new Coordinates(newColumn, newRow);

                // WICHTIG: Prüfe ob auf diesem Feld bereits ein anderer Spieler steht!
                if (isOccupied(neighbor, occupiedPositions)) {
                    continue;
                }

                Tile neighborTile = getTile(board, neighbor);

                if (neighborTile == null || visited[newRow][newColumn]) {
                    continue;
                }

                // Prüfe ob Nachbar-Tile entgegengesetzte Richtung hat
                boolean hasOppositeEntrance = false;
                for (DirectionType neighborDir : neighborTile.getEntrances()) {
                    if (isOppositeDirection(dir, neighborDir)) {
                        hasOppositeEntrance = true;
                        break;
                    }
                }

                if (!hasOppositeEntrance) {
                    continue;
                }

                // Feld ist erreichbar!
                reachable.add(neighbor);
                queue.add(neighbor);
                visited[newRow][newColumn] = true;
            }
        }

        return reachable;
    }

    /**
     * Berechnet die Manhattan-Distanz zwischen zwei Koordinaten
     */
    public int calculateDistance(Coordinates from, Coordinates to) {
        return Math.abs(from.getColumn() - to.getColumn()) + Math.abs(from.getRow() - to.getRow());
    }

    /**
     * Prüft ob zwei Richtungen entgegengesetzt sind (wie im Server)
     */
    private boolean isOppositeDirection(DirectionType dir1, DirectionType dir2) {
        return (dir1.equals(DirectionType.UP) && dir2.equals(DirectionType.DOWN)) ||
               (dir1.equals(DirectionType.DOWN) && dir2.equals(DirectionType.UP)) ||
               (dir1.equals(DirectionType.LEFT) && dir2.equals(DirectionType.RIGHT)) ||
               (dir1.equals(DirectionType.RIGHT) && dir2.equals(DirectionType.LEFT));
    }

    /**
     * Prüft ob ein Feld von einem anderen Spieler besetzt ist
     */
    private boolean isOccupied(Coordinates position, Set<Coordinates> occupiedPositions) {
        for (Coordinates occupied : occupiedPositions) {
            if (occupied.getColumn() == position.getColumn() && occupied.getRow() == position.getRow()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Holt ein Tile an gegebener Position
     */
    private Tile getTile(Tile[][] board, Coordinates coords) {
        if (coords.getRow() >= 0 && coords.getRow() < board.length &&
            coords.getColumn() >= 0 && coords.getColumn() < board[0].length) {
            return board[coords.getRow()][coords.getColumn()];
        }
        return null;
    }
}
