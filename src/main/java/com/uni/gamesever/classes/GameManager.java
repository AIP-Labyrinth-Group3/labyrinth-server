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
    private GameBoard currentBoard;
    private TurnState turnState = TurnState.NOT_STARTED;
    SocketMessageService socketBroadcastService;
    private final ObjectMapper objectMapper = ObjectMapperSingleton.getInstance();
    private final Map<DirectionType, Coordinates> DIRECTION_OFFSETS = Map.of(
            DirectionType.UP, new Coordinates(-1, 0),
            DirectionType.DOWN, new Coordinates(1, 0),
            DirectionType.LEFT, new Coordinates(0, -1),
            DirectionType.RIGHT, new Coordinates(0, 1));

    public GameManager(PlayerManager playerManager, SocketMessageService socketBroadcastService) {
        this.playerManager = playerManager;
        this.socketBroadcastService = socketBroadcastService;
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

        currentBoard.updateBoard(rowOrColIndex, direction);
        updatePlayerPositionsAfterPush(rowOrColIndex, direction, currentBoard.getRows(), currentBoard.getCols());
        PushActionInfo pushInfo = new PushActionInfo(rowOrColIndex);
        pushInfo.setDirections(direction.name());
        currentBoard.setLastPush(pushInfo);
        GameStateUpdate gameStateUpdate = new GameStateUpdate(currentBoard, playerManager.getNonNullPlayerStates());
        socketBroadcastService.broadcastMessage(objectMapper.writeValueAsString(gameStateUpdate));

        setTurnState(TurnState.WAITING_FOR_MOVE);

        return true;
    }

    private void updatePlayerPositionsAfterPush(int index, DirectionType direction, int rows, int cols) {
        PlayerState[] allPlayerStates = playerManager.getNonNullPlayerStates();

        for (PlayerState player : allPlayerStates) {
            Coordinates pos = player.getCurrentPosition();
            int x = pos.getX();
            int y = pos.getY();

            switch (direction) {
                case UP:
                    if (y == index) {
                        x -= 1;
                        if (x < 0) {
                            x = rows - 1;
                        }
                        player.setCurrentPosition(new Coordinates(x, y));
                    }
                    break;
                case DOWN:
                    if (y == index) {
                        x += 1;
                        if (x >= rows) {
                            x = 0;
                        }
                        player.setCurrentPosition(new Coordinates(x, y));
                    }
                    break;
                case LEFT:
                    if (x == index) {
                        y -= 1;
                        if (y < 0) {
                            y = cols - 1;
                        }
                        player.setCurrentPosition(new Coordinates(x, y));
                    }
                    break;
                case RIGHT:
                    if (x == index) {
                        y += 1;
                        if (y >= cols) {
                            y = 0;
                        }
                        player.setCurrentPosition(new Coordinates(x, y));
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
        if (targetCoordinates.getX() < 0 || targetCoordinates.getY() < 0 ||
                targetCoordinates.getX() >= currentBoard.getRows()
                || targetCoordinates.getY() >= currentBoard.getCols()) {
            throw new IllegalArgumentException("Target coordinates are out of board bounds.");
        }
        if (!playerIdWhoMoved.equals(playerManager.getCurrentPlayer().getId())) {
            throw new NotPlayersTurnException(
                    "It is not your turn to move the pawn.");
        }

        PlayerState currentPlayerState = playerManager.getCurrentPlayerState();
        Coordinates currentCoordinates = currentPlayerState.getCurrentPosition();

        if (!canPlayerMove(currentBoard, currentCoordinates, targetCoordinates)) {
            throw new NoValidActionException("Player cannot move to the target coordinates.");
        }

        currentPlayerState.setCurrentPosition(targetCoordinates);

        Tile targetTile = currentBoard.getTileAtCoordinate(targetCoordinates);
        if (targetTile != null && targetTile.getTreasure() != null &&
                targetTile.getTreasure().equals(currentPlayerState.getCurrentTreasure())) {
            try {
                currentPlayerState.collectCurrentTreasure();
                currentBoard.removeTreasureFromTile(targetCoordinates);
                if (currentPlayerState.getCurrentTreasure() != null) {
                    NextTreasureCardEvent nextTreasureEvent = new NextTreasureCardEvent(
                            currentPlayerState.getCurrentTreasure());
                    socketBroadcastService.sendMessageToSession(playerIdWhoMoved,
                            objectMapper.writeValueAsString(nextTreasureEvent));
                } else {
                    // Player has collected all treasures
                    // Handle end-of-game logic here if needed
                }

            } catch (IllegalStateException e) {
                throw new GameNotValidException(e.getMessage());
            }
        }

        GameStateUpdate gameStatUpdate = new GameStateUpdate(currentBoard, playerManager.getNonNullPlayerStates());
        socketBroadcastService.broadcastMessage(objectMapper.writeValueAsString(gameStatUpdate));

        PlayerTurn turn = new PlayerTurn(playerManager.getCurrentPlayer().getId(), currentBoard.getExtraTile(), 60);
        socketBroadcastService.broadcastMessage(objectMapper.writeValueAsString(turn));

        setTurnState(TurnState.WAITING_FOR_PUSH);
        playerManager.setNextPlayerAsCurrent();

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

        if (currentBoard.isPlayerOnTile(target,
                playerManager.getThePlayerStatesOfAllOtherPlayers())) {
            throw new IllegalArgumentException("Target tile is occupied by another player");
        }

        if (start.getX() == target.getX() && start.getY() == target.getY()) {
            return true;
        }

        int rows = board.getRows();
        int cols = board.getCols();

        boolean[][] visited = new boolean[rows][cols];
        Queue<Coordinates> queue = new java.util.LinkedList<>();
        queue.add(start);
        visited[start.getX()][start.getY()] = true;

        while (!queue.isEmpty()) {
            Coordinates current = queue.poll();
            int x = current.getX();
            int y = current.getY();

            Tile tile = board.getTileAtCoordinate(current);
            if (tile == null) {
                continue;
            }

            for (DirectionType dir : tile.getEntrances()) {
                Coordinates offset = DIRECTION_OFFSETS.get(dir);
                int newX = x + offset.getX();
                int newY = y + offset.getY();

                if (newX < 0 || newX >= rows || newY < 0 || newY >= cols) {
                    continue;
                }

                Coordinates neighbor = new Coordinates(newX, newY);
                Tile neighborTile = board.getTileAtCoordinate(neighbor);
                if (neighborTile == null || visited[newX][newY]) {
                    continue;
                }
                boolean hasOppositeEntrance = neighborTile.getEntrances().stream()
                        .anyMatch(d -> isOppositeDirection(dir, d));
                if (!hasOppositeEntrance) {
                    continue;
                }
                if (neighbor.getX() == target.getX() && neighbor.getY() == target.getY()) {
                    return true;
                }
                queue.add(neighbor);
                visited[newX][newY] = true;
            }
        }
        return false;
    }
}
