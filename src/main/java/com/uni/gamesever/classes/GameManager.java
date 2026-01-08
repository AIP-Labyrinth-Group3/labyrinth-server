package com.uni.gamesever.classes;

import java.util.ArrayList;
import java.util.List;
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
import com.uni.gamesever.models.BonusType;
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
import com.uni.gamesever.services.BoardItemPlacementService;
import com.uni.gamesever.services.SocketMessageService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

@Service
public class GameManager {
    PlayerManager playerManager;
    GameStatsManager gameStatsManager;
    private GameBoard currentBoard;
    private TurnState turnState = TurnState.NOT_STARTED;
    SocketMessageService socketBroadcastService;
    GameTimerManager gameTimerManager;
    private final ObjectMapper objectMapper = ObjectMapperSingleton.getInstance();
    private boolean pushTwiceUsedInCurrentTurn = false;
    private final Map<DirectionType, Coordinates> DIRECTION_OFFSETS = Map.of(
            DirectionType.UP, new Coordinates(0, -1),
            DirectionType.DOWN, new Coordinates(0, 1),
            DirectionType.LEFT, new Coordinates(-1, 0),
            DirectionType.RIGHT, new Coordinates(1, 0));
    BoardItemPlacementService boardItemPlacementService;

    public GameManager(PlayerManager playerManager, SocketMessageService socketBroadcastService,
            GameStatsManager gameStatsManager, BoardItemPlacementService boardItemPlacementService,
            GameTimerManager gameTimerManager) {
        this.playerManager = playerManager;
        this.socketBroadcastService = socketBroadcastService;
        this.gameStatsManager = gameStatsManager;
        this.boardItemPlacementService = boardItemPlacementService;
        this.gameTimerManager = gameTimerManager;
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

    public boolean handlePushTile(int rowOrColIndex, DirectionType direction, String playerIdWhoPushed,
            boolean isUsingPushFixed)
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

        currentBoard.pushTile(rowOrColIndex, direction, isUsingPushFixed);
        gameStatsManager.increaseTilesPushed(1, playerManager.getCurrentPlayer().getId());
        updatePlayerPositionsAfterPush(rowOrColIndex, direction, currentBoard.getRows(), currentBoard.getCols());
        PushActionInfo pushInfo = new PushActionInfo(rowOrColIndex);
        pushInfo.setDirections(direction.name());
        currentBoard.setLastPush(pushInfo);
        if (pushTwiceUsedInCurrentTurn) {
            pushTwiceUsedInCurrentTurn = false;
        } else {
            setTurnState(TurnState.WAITING_FOR_MOVE);
        }
        GameStateUpdate gameStateUpdate = new GameStateUpdate(currentBoard, playerManager.getNonNullPlayerStates(),
                playerManager.getCurrentPlayer().getId(), getTurnState().name());
        socketBroadcastService.broadcastMessage(objectMapper.writeValueAsString(gameStateUpdate));

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

    public boolean handleMovePawn(Coordinates targetCoordinates, String playerIdWhoMoved, boolean useBeamBonus)
            throws GameNotValidException,
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

        if (!useBeamBonus && !canPlayerMove(currentBoard, currentPlayerCoordinates, targetCoordinates)) {
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
                    return endGameByTimeoutOrAfterCollectingAllTreasures();
                }

            } catch (IllegalStateException e) {
                throw new GameNotValidException(e.getMessage());
            }
        }

        if (targetTile != null && targetTile.getBonus() != null) {
            if (currentPlayerState.getAvailableBonuses().length < 5) {
                currentPlayerState.collectBonus(targetTile.getBonus());
                currentBoard.removeBonusFromTile(targetCoordinates);
            }
        }

        setTurnState(TurnState.WAITING_FOR_PUSH);
        playerManager.setNextPlayerAsCurrent();
        boardItemPlacementService.trySpawnBonus(currentBoard);

        GameStateUpdate gameStatUpdate = new GameStateUpdate(currentBoard, playerManager.getNonNullPlayerStates(),
                playerManager.getCurrentPlayer().getId(), getTurnState().name());
        socketBroadcastService.broadcastMessage(objectMapper.writeValueAsString(gameStatUpdate));

        PlayerTurn turn = new PlayerTurn(playerManager.getCurrentPlayer().getId(), currentBoard.getExtraTile(), 60);
        socketBroadcastService.broadcastMessage(objectMapper.writeValueAsString(turn));

        return true;
    }

    public boolean handleRotateTile(String playerIdWhoRotated) throws GameNotValidException,
            NotPlayersTurnException, NoValidActionException, JsonProcessingException {

        if (!playerIdWhoRotated.equals(playerManager.getCurrentPlayer().getId())) {
            throw new NotPlayersTurnException(
                    "It is not your turn to rotate tiles.");
        }

        if (turnState != TurnState.WAITING_FOR_PUSH) {
            throw new GameNotValidException(
                    "It is not the phase to rotate tiles.");
        }

        Tile spareTile = currentBoard.getExtraTile();
        spareTile.rotateClockwise();
        currentBoard.setExtraTile(spareTile);

        setTurnState(TurnState.WAITING_FOR_PUSH);
        GameStateUpdate gameStatUpdate = new GameStateUpdate(currentBoard, playerManager.getNonNullPlayerStates(),
                playerManager.getCurrentPlayer().getId(), getTurnState().name());
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

    public boolean handleUseBeam(Coordinates targetCoordinates, String playerIdWhoUsedBeam)
            throws GameNotValidException, NotPlayersTurnException, NoValidActionException,
            TargetCoordinateNullException, JsonProcessingException {
        if (turnState != TurnState.WAITING_FOR_MOVE) {
            throw new GameNotValidException(
                    "It is not the phase to use the beam.");
        }
        if (targetCoordinates == null) {
            throw new TargetCoordinateNullException("Target coordinates cannot be null");
        }
        if (targetCoordinates.getColumn() < 0 || targetCoordinates.getRow() < 0 ||
                targetCoordinates.getColumn() >= currentBoard.getCols()
                || targetCoordinates.getRow() >= currentBoard.getRows()) {
            throw new IllegalArgumentException("Target coordinates are out of board bounds.");
        }
        if (!playerIdWhoUsedBeam.equals(playerManager.getCurrentPlayer().getId())) {
            throw new NotPlayersTurnException(
                    "It is not your turn to use the beam.");
        }

        PlayerState currentPlayerState = playerManager.getCurrentPlayerState();
        if (!currentPlayerState.hasBonusOfType(BonusType.BEAM)) {
            throw new NoValidActionException("Player does not have a BEAM bonus to use.");
        }

        currentPlayerState.useOneBonusOfType(BonusType.BEAM);

        return handleMovePawn(targetCoordinates, playerIdWhoUsedBeam, true);
    }

    public boolean handleUseSwap(String targetPlayerId, String playerIdWhoUsedSwap)
            throws GameNotValidException, NotPlayersTurnException, NoValidActionException,
            TargetCoordinateNullException, JsonProcessingException {
        if (!playerIdWhoUsedSwap.equals(playerManager.getCurrentPlayer().getId())) {
            throw new NotPlayersTurnException(
                    "It is not your turn to use the swap bonus.");
        }
        if (turnState != TurnState.WAITING_FOR_MOVE) {
            throw new GameNotValidException(
                    "It is not the phase to use the swap bonus.");
        }
        if (targetPlayerId == null || targetPlayerId.isEmpty()) {
            throw new NoValidActionException("Target player ID cannot be null or empty");
        }

        if (targetPlayerId.equals(playerIdWhoUsedSwap)) {
            throw new NoValidActionException("Cannot swap with yourself.");
        }

        PlayerState currentPlayerState = playerManager.getCurrentPlayerState();
        if (!currentPlayerState.hasBonusOfType(BonusType.SWAP)) {
            throw new NoValidActionException("Player does not have a SWAP bonus to use.");
        }

        PlayerState targetPlayerState = playerManager.getPlayerStateById(targetPlayerId);
        if (targetPlayerState == null) {
            throw new NoValidActionException("Target player does not exist.");
        }

        currentPlayerState.useOneBonusOfType(BonusType.SWAP);

        Coordinates currentPlayerPosition = currentPlayerState.getCurrentPosition();
        Coordinates targetPlayerPosition = targetPlayerState.getCurrentPosition();

        currentPlayerState.setCurrentPosition(targetPlayerPosition);
        targetPlayerState.setCurrentPosition(currentPlayerPosition);

        setTurnState(TurnState.WAITING_FOR_PUSH);
        playerManager.setNextPlayerAsCurrent();
        boardItemPlacementService.trySpawnBonus(currentBoard);

        GameStateUpdate gameStatUpdate = new GameStateUpdate(currentBoard, playerManager.getNonNullPlayerStates(),
                playerManager.getCurrentPlayer().getId(), getTurnState().name());
        socketBroadcastService.broadcastMessage(objectMapper.writeValueAsString(gameStatUpdate));

        PlayerTurn turn = new PlayerTurn(playerManager.getCurrentPlayer().getId(), currentBoard.getExtraTile(), 60);
        socketBroadcastService.broadcastMessage(objectMapper.writeValueAsString(turn));

        return true;
    }

    public boolean handleUsePushFixedTile(DirectionType direction, int rowOrColIndex, String playerIdWhoUsedPushFixed)
            throws GameNotValidException, NotPlayersTurnException, NoValidActionException,
            JsonProcessingException, IllegalArgumentException, NoExtraTileException, NoDirectionForPush {
        if (!playerIdWhoUsedPushFixed.equals(playerManager.getCurrentPlayer().getId())) {
            throw new NotPlayersTurnException(
                    "It is not your turn to use the push fixed tile bonus.");
        }
        if (turnState != TurnState.WAITING_FOR_PUSH) {
            throw new GameNotValidException(
                    "It is not the phase to use the push fixed tile bonus.");
        }

        PlayerState currentPlayerState = playerManager.getCurrentPlayerState();
        if (!currentPlayerState.hasBonusOfType(BonusType.PUSH_FIXED)) {
            throw new NoValidActionException("Player does not have a bonus to push a fixed tile.");
        }

        if (direction == null) {
            throw new NoDirectionForPush("Direction for push cannot be null");
        }

        GameBoard board = getCurrentBoard();
        int rows = board.getRows();
        int cols = board.getCols();

        List<Coordinates> forbiddenStartPositions = new ArrayList<>();
        forbiddenStartPositions.add(new Coordinates(0, 0));
        forbiddenStartPositions.add(new Coordinates(0, rows - 1));
        forbiddenStartPositions.add(new Coordinates(cols - 1, 0));
        forbiddenStartPositions.add(new Coordinates(cols - 1, rows - 1));

        if (forbiddenStartPositions.stream().anyMatch(
                start -> (start.getColumn() == rowOrColIndex &&
                        (rowOrColIndex == 0 || rowOrColIndex == cols - 1)) ||
                        (start.getRow() == rowOrColIndex &&
                                (rowOrColIndex == 0 || rowOrColIndex == rows - 1)))) {
            throw new NoValidActionException("Cannot push on a forbidden start position.");
        }

        currentPlayerState.useOneBonusOfType(BonusType.PUSH_FIXED);

        return handlePushTile(rowOrColIndex, direction, playerIdWhoUsedPushFixed, true);
    }

    public boolean handleUsePushTwice(String playerIdWhoUsedPushTwice)
            throws GameNotValidException, NotPlayersTurnException, NoValidActionException,
            JsonProcessingException {
        if (!playerIdWhoUsedPushTwice.equals(playerManager.getCurrentPlayer().getId())) {
            throw new NotPlayersTurnException(
                    "It is not your turn to use the push twice bonus.");
        }
        if (turnState != TurnState.WAITING_FOR_PUSH) {
            throw new GameNotValidException(
                    "It is not the phase to use the push twice bonus.");
        }

        PlayerState currentPlayerState = playerManager.getCurrentPlayerState();
        if (!currentPlayerState.hasBonusOfType(BonusType.PUSH_TWICE)) {
            throw new NoValidActionException("Player does not have a PUSH_TWICE bonus to use.");
        }

        currentPlayerState.useOneBonusOfType(BonusType.PUSH_TWICE);

        pushTwiceUsedInCurrentTurn = true;

        return true;
    }

    @EventListener
    public void onGameTimeout(GameTimeoutEvent event) {
        try {
            System.out.println("[GameManager] Game ended due to timeout (event)");
            endGameByTimeoutOrAfterCollectingAllTreasures();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean endGameByTimeoutOrAfterCollectingAllTreasures() throws JsonProcessingException {
        System.out.println("[GameManager] Game ended due to timeout");
        gameTimerManager.stop();
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
}
