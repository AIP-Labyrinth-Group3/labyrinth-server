package com.uni.gamesever.classes;

import java.util.Map;
import java.util.Queue;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.gamesever.exceptions.GameNotStartedException;
import com.uni.gamesever.exceptions.GameNotValidException;
import com.uni.gamesever.exceptions.NoDirectionForPush;
import com.uni.gamesever.exceptions.NoExtraTileException;
import com.uni.gamesever.exceptions.NoValidActionException;
import com.uni.gamesever.exceptions.NotPlayersTurnException;
import com.uni.gamesever.exceptions.PushNotValidException;
import com.uni.gamesever.exceptions.TargetCoordinateNullException;
import com.uni.gamesever.models.Coordinates;
import com.uni.gamesever.models.DirectionType;
import com.uni.gamesever.models.GameBoard;
import com.uni.gamesever.models.GameOver;
import com.uni.gamesever.models.GameStateUpdate;
import com.uni.gamesever.models.NextTreasureCardEvent;
import com.uni.gamesever.models.PlayerState;
import com.uni.gamesever.models.PlayerTurn;
import com.uni.gamesever.models.PushActionInfo;
import com.uni.gamesever.models.Tile;
import com.uni.gamesever.models.TurnState;
import com.uni.gamesever.services.SocketMessageService;

@Service
public class GameManager {
    PlayerManager playerManager;
    GameStatsManager gameStatsManager;
    private GameBoard currentBoard;
    private TurnState turnState = TurnState.NOT_STARTED;
    SocketMessageService socketBroadcastService;
    private final ObjectMapper objectMapper = ObjectMapperSingleton.getInstance();
    private final Map<DirectionType, Coordinates> DIRECTION_OFFSETS = Map.of(
            DirectionType.UP, new Coordinates(0, -1),
            DirectionType.DOWN, new Coordinates(0, 1),
            DirectionType.LEFT, new Coordinates(-1, 0),
            DirectionType.RIGHT, new Coordinates(1, 0));

    public GameManager(PlayerManager playerManager, SocketMessageService socketBroadcastService,
            GameStatsManager gameStatsManager) {
        this.playerManager = playerManager;
        this.socketBroadcastService = socketBroadcastService;
        this.gameStatsManager = gameStatsManager;
    }

    public GameBoard getCurrentBoard() {
        return currentBoard;
    }

    public void setCurrentBoard(GameBoard currentBoard) {
        this.currentBoard = currentBoard;
    }

    public TurnState getTurnState() {
        return turnState;
    }

    public void setTurnState(TurnState turnState) {
        this.turnState = turnState;
    }

    public boolean handlePushTile(int rowOrColIndex, DirectionType direction, String playerIdWhoPushed)
            throws GameNotStartedException, NotPlayersTurnException, PushNotValidException, JsonProcessingException,
            IllegalArgumentException, NoExtraTileException, NoDirectionForPush {
        if (turnState != TurnState.WAITING_FOR_PUSH) {
            throw new GameNotStartedException("Game is not active. Cannot push tile.");
        }
        if (!playerIdWhoPushed.equals(playerManager.getCurrentPlayer().getId())) {
            throw new NotPlayersTurnException(
                    "It is not your turn to push a tile.");
        }
        if (direction == null) {
            throw new NoDirectionForPush("Direction for push cannot be null");
        }
        if (currentBoard.getLastPush() != null) {
            int lastIndex = currentBoard.getLastPush().getRowOrColIndex();
            DirectionType lastDirection = currentBoard.getLastPush().getDirection();

            if (lastIndex == rowOrColIndex && isOppositeDirection(lastDirection, direction)) {
                throw new PushNotValidException("You are not allowed to push back the tile that was just pushed.");
            }
        }

        currentBoard.pushTile(rowOrColIndex, direction);
        gameStatsManager.increaseTilesPushed(1, playerManager.getCurrentPlayer().getId());
        updatePlayerPositionsAfterPush(rowOrColIndex, direction, currentBoard.getRows(), currentBoard.getCols());
        PushActionInfo pushInfo = new PushActionInfo(rowOrColIndex);
        pushInfo.setDirections(direction.name());
        currentBoard.setLastPush(pushInfo);
        GameStateUpdate gameStateUpdate = new GameStateUpdate(currentBoard, playerManager.getNonNullPlayerStates());
        socketBroadcastService.broadcastMessage(objectMapper.writeValueAsString(gameStateUpdate));

        setTurnState(TurnState.WAITING_FOR_MOVE);

        return true;
    }

    private void updatePlayerPositionsAfterPush(int rowOrColToPushIndex, DirectionType direction, int rows, int cols) {
        PlayerState[] allPlayerStates = playerManager.getNonNullPlayerStates();

        for (PlayerState player : allPlayerStates) {
            Coordinates pos = player.getCurrentPosition();
            int colOfPlayer = pos.getColumn();
            int rowOfPlayer = pos.getRow();

            switch (direction) {
                case UP:
                    if (colOfPlayer == rowOrColToPushIndex) {
                        rowOfPlayer -= 1;
                        if (rowOfPlayer < 0) {
                            rowOfPlayer = rows - 1;
                        }
                        player.setCurrentPosition(new Coordinates(colOfPlayer, rowOfPlayer));
                    }
                    break;
                case DOWN:
                    if (colOfPlayer == rowOrColToPushIndex) {
                        rowOfPlayer += 1;
                        if (rowOfPlayer >= rows) {
                            rowOfPlayer = 0;
                        }
                        player.setCurrentPosition(new Coordinates(colOfPlayer, rowOfPlayer));
                    }
                    break;
                case LEFT:
                    if (rowOfPlayer == rowOrColToPushIndex) {
                        colOfPlayer -= 1;
                        if (colOfPlayer < 0) {
                            colOfPlayer = cols - 1;
                        }
                        player.setCurrentPosition(new Coordinates(colOfPlayer, rowOfPlayer));
                    }
                    break;
                case RIGHT:
                    if (rowOfPlayer == rowOrColToPushIndex) {
                        colOfPlayer += 1;
                        if (colOfPlayer >= cols) {
                            colOfPlayer = 0;
                        }
                        player.setCurrentPosition(new Coordinates(colOfPlayer, rowOfPlayer));
                    }
                    break;
            }
        }
    }

    public boolean handleMovePawn(Coordinates targetCoordinates, String playerIdWhoMoved) throws GameNotValidException,
            NotPlayersTurnException, NoValidActionException, JsonProcessingException, TargetCoordinateNullException {
        if (turnState != TurnState.WAITING_FOR_MOVE) {
            throw new GameNotValidException(
                    "It is not the phase to move the pawn.");
        }
        if (targetCoordinates == null) {
            throw new TargetCoordinateNullException("Target coordinates cannot be null");
        }
        if (targetCoordinates.getColumn() < 0 || targetCoordinates.getRow() < 0 ||
                targetCoordinates.getColumn() >= currentBoard.getCols()
                || targetCoordinates.getRow() >= currentBoard.getRows()) {
            throw new IllegalArgumentException("Target coordinates are out of board bounds.");
        }
        if (!playerIdWhoMoved.equals(playerManager.getCurrentPlayer().getId())) {
            throw new NotPlayersTurnException(
                    "It is not your turn to move the pawn.");
        }

        PlayerState currentPlayerState = playerManager.getCurrentPlayerState();
        Coordinates currentPlayerCoordinates = currentPlayerState.getCurrentPosition();

        if (!canPlayerMove(currentBoard, currentPlayerCoordinates, targetCoordinates)) {
            throw new NoValidActionException("Player cannot move to the target coordinates.");
        }

        currentPlayerState.setCurrentPosition(targetCoordinates);

        Tile targetTile = currentBoard.getTileAtCoordinate(targetCoordinates);
        if (targetTile != null && targetTile.getTreasure() != null &&
                targetTile.getTreasure().equals(currentPlayerState.getCurrentTreasure())) {
            try {
                currentPlayerState.collectCurrentTreasure();
                currentBoard.removeTreasureFromTile(targetCoordinates);
                gameStatsManager.increaseTreasuresCollected(1, playerIdWhoMoved);
                if (currentPlayerState.getCurrentTreasure() != null) {
                    NextTreasureCardEvent nextTreasureEvent = new NextTreasureCardEvent(
                            currentPlayerState.getCurrentTreasure());
                    socketBroadcastService.sendMessageToSession(playerIdWhoMoved,
                            objectMapper.writeValueAsString(nextTreasureEvent));
                } else {
                    gameStatsManager.updateScoresForAllPlayers();
                    gameStatsManager.updateRankForAllPlayersBasedOnAmountOfTreasures();
                    GameOver gameOver = new GameOver(gameStatsManager.getSortedRankings());
                    if (gameOver.getWinnerId() != null) {
                        socketBroadcastService.broadcastMessage(objectMapper.writeValueAsString(gameOver));
                        setTurnState(TurnState.NOT_STARTED);
                        return true;
                    } else {
                        throw new IllegalStateException("Winner ID is null despite all treasures being collected.");
                    }
                }

            } catch (IllegalStateException e) {
                throw new GameNotValidException(e.getMessage());
            }
        }

        setTurnState(TurnState.WAITING_FOR_PUSH);
        playerManager.setNextPlayerAsCurrent();

        GameStateUpdate gameStatUpdate = new GameStateUpdate(currentBoard, playerManager.getNonNullPlayerStates());
        socketBroadcastService.broadcastMessage(objectMapper.writeValueAsString(gameStatUpdate));

        PlayerTurn turn = new PlayerTurn(playerManager.getCurrentPlayer().getId(), currentBoard.getExtraTile(), 60);
        socketBroadcastService.broadcastMessage(objectMapper.writeValueAsString(turn));

        return true;
    }

    public boolean isOppositeDirection(DirectionType dir1, DirectionType dir2) {
        return (dir1.equals(DirectionType.UP) && dir2.equals(DirectionType.DOWN)) ||
                (dir1.equals(DirectionType.DOWN) && dir2.equals(DirectionType.UP)) ||
                (dir1.equals(DirectionType.LEFT) && dir2.equals(DirectionType.RIGHT)) ||
                (dir1.equals(DirectionType.RIGHT) && dir2.equals(DirectionType.LEFT));
    }

    public boolean canPlayerMove(GameBoard board, Coordinates start, Coordinates target)
            throws IllegalArgumentException {
        if (start == null || target == null) {
            throw new IllegalArgumentException("Start or target coordinates cannot be null");
        }

        if (currentBoard.isAnyPlayerOnTile(target,
                playerManager.getPlayerStatesOfPlayersNotOnTurn())) {
            throw new IllegalArgumentException("Target tile is occupied by another player");
        }

        if (start.getColumn() == target.getColumn() && start.getRow() == target.getRow()) {
            return true;
        }

        int boardRows = board.getRows();
        int boardCols = board.getCols();
        Map<Coordinates, Integer> stepsMap = new java.util.HashMap<>();

        boolean[][] visited = new boolean[boardRows][boardCols];
        Queue<Coordinates> queue = new java.util.LinkedList<>();
        queue.add(start);
        visited[start.getRow()][start.getColumn()] = true;
        stepsMap.put(start, 0);

        while (!queue.isEmpty()) {
            Coordinates current = queue.poll();
            int currentColumn = current.getColumn();
            int currentRow = current.getRow();

            Tile tile = board.getTileAtCoordinate(current);
            if (tile == null) {
                continue;
            }

            for (DirectionType dir : tile.getEntrances()) {
                Coordinates offset = DIRECTION_OFFSETS.get(dir);
                int newColumn = currentColumn + offset.getColumn();
                int newRow = currentRow + offset.getRow();

                if (newColumn < 0 || newColumn >= boardCols || newRow < 0 || newRow >= boardRows) {
                    continue;
                }

                Coordinates neighbor = new Coordinates(newColumn, newRow);
                Tile neighborTile = board.getTileAtCoordinate(neighbor);
                if (neighborTile == null || visited[newRow][newColumn]) {
                    continue;
                }
                boolean hasOppositeEntrance = neighborTile.getEntrances().stream()
                        .anyMatch(d -> isOppositeDirection(dir, d));
                if (!hasOppositeEntrance) {
                    continue;
                }
                if (neighbor.getColumn() == target.getColumn() && neighbor.getRow() == target.getRow()) {
                    gameStatsManager.increaseStepsTaken(stepsMap.get(current) + 1,
                            playerManager.getCurrentPlayer().getId());
                    return true;
                }
                queue.add(neighbor);
                visited[newRow][newColumn] = true;
                stepsMap.put(neighbor, stepsMap.get(current) + 1);
            }
        }
        return false;
    }

}
