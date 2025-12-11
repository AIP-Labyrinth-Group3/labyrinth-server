package com.uni.gamesever.classes;

import java.util.Map;
import java.util.Queue;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.gamesever.exceptions.GameNotValidException;
import com.uni.gamesever.exceptions.NoValidActionException;
import com.uni.gamesever.exceptions.NotPlayersTurnException;
import com.uni.gamesever.exceptions.PushNotValidException;
import com.uni.gamesever.models.Coordinates;
import com.uni.gamesever.models.GameBoard;
import com.uni.gamesever.models.GameStateUpdate;
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
    private final Map<String, Coordinates> DIRECTION_OFFSETS = Map.of(
            "UP", new Coordinates(-1, 0),
            "DOWN", new Coordinates(1, 0),
            "LEFT", new Coordinates(0, -1),
            "RIGHT", new Coordinates(0, 1));

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

    public boolean handlePushTile(int rowOrColIndex, String direction, String playerIdWhoPushed)
            throws GameNotValidException, NotPlayersTurnException, PushNotValidException, JsonProcessingException,
            IllegalArgumentException {
        if (turnState != TurnState.WAITING_FOR_PUSH) {
            throw new GameNotValidException("Game is not active. Cannot push tile.");
        }
        if (!playerIdWhoPushed.equals(playerManager.getCurrentPlayer().getId())) {
            throw new NotPlayersTurnException(
                    "It's not the turn of player " + playerIdWhoPushed + ". Cannot push tile.");
        }
        if (currentBoard.getLastPush() != null) {
            int lastIndex = currentBoard.getLastPush().getRowOrColIndex();
            String lastDirection = currentBoard.getLastPush().getDirection();

            if (lastIndex == rowOrColIndex && isOppositeDirection(lastDirection, direction)) {
                throw new PushNotValidException("Invalid push: same index and opposite direction");
            }
        }

        currentBoard.updateBoard(rowOrColIndex, direction);
        PushActionInfo pushInfo = new PushActionInfo(rowOrColIndex);
        pushInfo.setDirections(direction);
        currentBoard.setLastPush(pushInfo);

        GameBoard.printBoard(currentBoard);

        GameStateUpdate gameStateUpdate = new GameStateUpdate(currentBoard, playerManager.getNonNullPlayerStates());
        socketBroadcastService.broadcastMessage(objectMapper.writeValueAsString(gameStateUpdate));

        setTurnState(TurnState.WAITING_FOR_MOVE);

        return true;
    }

    public boolean handleMovePawn(Coordinates targetCoordinates, String playerIdWhoMoved) throws GameNotValidException,
            NotPlayersTurnException, NoValidActionException, JsonProcessingException, IllegalArgumentException {
        if (turnState != TurnState.WAITING_FOR_MOVE) {
            throw new GameNotValidException(
                    "Game is not in the WAITING_FOR_MOVE state. Current state: " + turnState);
        }
        if (!playerIdWhoMoved.equals(playerManager.getCurrentPlayer().getId())) {
            throw new NotPlayersTurnException(
                    "It's not the turn of player " + playerIdWhoMoved + ". Cannot move pawn.");
        }

        PlayerState currentPlayerState = playerManager.getCurrentPlayerState();
        Coordinates currentCoordinates = currentPlayerState.getCurrentPosition();

        if (!canPlayerMove(currentBoard, currentCoordinates, targetCoordinates)) {
            throw new NoValidActionException("Player cannot move to the target coordinates.");
        }

        currentPlayerState.setCurrentPosition(targetCoordinates);
        GameStateUpdate gameStatUpdate = new GameStateUpdate(currentBoard, playerManager.getNonNullPlayerStates());
        socketBroadcastService.broadcastMessage(objectMapper.writeValueAsString(gameStatUpdate));

        PlayerTurn turn = new PlayerTurn(playerManager.getCurrentPlayer().getId(), currentBoard.getExtraTile(), 60);
        socketBroadcastService.broadcastMessage(objectMapper.writeValueAsString(turn));

        setTurnState(TurnState.WAITING_FOR_PUSH);
        playerManager.setNextPlayerAsCurrent();

        return true;
    }

    public boolean isOppositeDirection(String dir1, String dir2) {
        return (dir1.equals("UP") && dir2.equals("DOWN")) ||
                (dir1.equals("DOWN") && dir2.equals("UP")) ||
                (dir1.equals("LEFT") && dir2.equals("RIGHT")) ||
                (dir1.equals("RIGHT") && dir2.equals("LEFT"));
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

            for (String dir : tile.getEntrances()) {
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
