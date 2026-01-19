package com.uni.gamesever.domain.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import com.uni.gamesever.domain.enums.LobbyStateEnum;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.gamesever.domain.enums.BonusType;
import com.uni.gamesever.domain.enums.DirectionType;
import com.uni.gamesever.domain.events.GameTimeoutEvent;
import com.uni.gamesever.domain.events.TurnTimeoutEvent;
import com.uni.gamesever.domain.exceptions.BonusNotAvailable;
import com.uni.gamesever.domain.exceptions.GameNotStartedException;
import com.uni.gamesever.domain.exceptions.GameNotValidException;
import com.uni.gamesever.domain.exceptions.NoDirectionForPush;
import com.uni.gamesever.domain.exceptions.NoExtraTileException;
import com.uni.gamesever.domain.exceptions.NoValidActionException;
import com.uni.gamesever.domain.exceptions.NotPlayersTurnException;
import com.uni.gamesever.domain.exceptions.PushNotValidException;
import com.uni.gamesever.domain.exceptions.TargetCoordinateNullException;
import com.uni.gamesever.domain.model.AchievementContext;
import com.uni.gamesever.domain.model.Coordinates;
import com.uni.gamesever.domain.model.GameBoard;
import com.uni.gamesever.domain.model.PlayerInfo;
import com.uni.gamesever.domain.model.PlayerState;
import com.uni.gamesever.domain.model.PushActionInfo;
import com.uni.gamesever.domain.model.Tile;
import com.uni.gamesever.domain.model.TurnInfo;
import com.uni.gamesever.domain.model.TurnState;
import com.uni.gamesever.infrastructure.GameTimerManager;
import com.uni.gamesever.interfaces.Websocket.ObjectMapperSingleton;
import com.uni.gamesever.interfaces.Websocket.messages.server.GameOverEvent;
import com.uni.gamesever.interfaces.Websocket.messages.server.GameStateUpdate;
import com.uni.gamesever.interfaces.Websocket.messages.server.LobbyState;
import com.uni.gamesever.interfaces.Websocket.messages.server.NextTreasureCardEvent;
import com.uni.gamesever.interfaces.Websocket.messages.server.PlayerTurnEvent;
import com.uni.gamesever.services.SocketMessageService;
import org.springframework.context.event.EventListener;

@Service
public class GameManager {
    PlayerManager playerManager;
    GameStatsManager gameStatsManager;
    private GameBoard currentBoard;
    private int totalBonusCountsOnBoard = 0;
    private LobbyStateEnum lobbyState = LobbyStateEnum.LOBBY;
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
    AchievementManager achievementManager;
    private TurnInfo turnInfo;
    private String gameEndTime;
    private TurnTimer turnTimer;

    public GameManager(PlayerManager playerManager, SocketMessageService socketBroadcastService,
            GameStatsManager gameStatsManager, BoardItemPlacementService boardItemPlacementService,
            GameTimerManager gameTimerManager, AchievementManager achievementManager, TurnTimer turnTimer) {
        this.playerManager = playerManager;
        this.socketBroadcastService = socketBroadcastService;
        this.gameStatsManager = gameStatsManager;
        this.boardItemPlacementService = boardItemPlacementService;
        this.gameTimerManager = gameTimerManager;
        this.achievementManager = achievementManager;
        this.turnInfo = new TurnInfo(null, TurnState.NOT_STARTED);
        this.turnTimer = turnTimer;
    }

    public GameBoard getCurrentBoard() {
        return currentBoard;
    }

    public void setCurrentBoard(GameBoard currentBoard) {
        this.currentBoard = currentBoard;
    }

    public void reduceTotalBonusCountsOnBoard(int amount) {
        this.totalBonusCountsOnBoard -= amount;
        if (this.totalBonusCountsOnBoard < 0) {
            this.totalBonusCountsOnBoard = 0;
        }
    }

    public int getTotalBonusCountsOnBoard() {
        return totalBonusCountsOnBoard;
    }

    public void setTotalBonusCountsOnBoard(int totalBonusCountsOnBoard) {
        this.totalBonusCountsOnBoard = totalBonusCountsOnBoard;
    }

    public int getPlayerCount() {
        return playerManager.getAmountOfPlayers();
    }

    public LobbyStateEnum getLobbyState() {
        return this.lobbyState;
    }

    public void setLobbyState(LobbyStateEnum lobbyState) {
        this.lobbyState = lobbyState;
    }

    public TurnInfo getTurnInfo() {
        return turnInfo;
    }

    public String getGameEndTime() {
        return gameEndTime;
    }

    public void setGameEndTime(String gameEndTime) {
        this.gameEndTime = gameEndTime;
    }

    public boolean handlePushTile(int rowOrColIndex, DirectionType direction, String playerIdWhoPushed,
            boolean isUsingPushFixed)
            throws GameNotStartedException, NotPlayersTurnException, PushNotValidException, JsonProcessingException,
            IllegalArgumentException, NoExtraTileException, NoDirectionForPush {
        if (!playerIdWhoPushed.equals(playerManager.getCurrentPlayer().getId())) {
            throw new NotPlayersTurnException(
                    "Es ist nicht dein Zug, um eine Kachel zu schieben.");
        }
        if (getTurnInfo().getTurnState() != TurnState.WAITING_FOR_PUSH) {
            throw new GameNotStartedException("Spiel ist nicht im Zustand, um eine Kachel zu schieben.");
        }
        if (direction == null) {
            throw new NoDirectionForPush("Richtung für das Schieben darf nicht null sein");
        }
        if (currentBoard.getLastPush() != null) {
            int lastIndex = currentBoard.getLastPush().getRowOrColIndex();
            DirectionType lastDirection = currentBoard.getLastPush().getDirection();

            if (lastIndex == rowOrColIndex && isOppositeDirection(lastDirection, direction)) {
                throw new PushNotValidException(
                        "Du darfst die Kachel nicht in die entgegengesetzte Richtung der letzten Verschiebung schieben.");
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
            getTurnInfo().setTurnState(TurnState.WAITING_FOR_MOVE);
        }
        informAllPlayersAboutCurrentGameState();

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
                            player.markPushedOut();
                        }
                        player.setCurrentPosition(new Coordinates(colOfPlayer, rowOfPlayer));
                    }
                    break;
                case DOWN:
                    if (colOfPlayer == rowOrColToPushIndex) {
                        rowOfPlayer += 1;
                        if (rowOfPlayer >= rows) {
                            rowOfPlayer = 0;
                            player.markPushedOut();
                        }
                        player.setCurrentPosition(new Coordinates(colOfPlayer, rowOfPlayer));
                    }
                    break;
                case LEFT:
                    if (rowOfPlayer == rowOrColToPushIndex) {
                        colOfPlayer -= 1;
                        if (colOfPlayer < 0) {
                            colOfPlayer = cols - 1;
                            player.markPushedOut();
                        }
                        player.setCurrentPosition(new Coordinates(colOfPlayer, rowOfPlayer));
                    }
                    break;
                case RIGHT:
                    if (rowOfPlayer == rowOrColToPushIndex) {
                        colOfPlayer += 1;
                        if (colOfPlayer >= cols) {
                            colOfPlayer = 0;
                            player.markPushedOut();
                        }
                        player.setCurrentPosition(new Coordinates(colOfPlayer, rowOfPlayer));
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public boolean handleMovePawn(Coordinates targetCoordinates, String playerIdWhoMoved, boolean useBeamBonus)
            throws GameNotValidException,
            NotPlayersTurnException, NoValidActionException, JsonProcessingException, TargetCoordinateNullException {
        if (!playerIdWhoMoved.equals(playerManager.getCurrentPlayer().getId())) {
            throw new NotPlayersTurnException(
                    "Es ist nicht dein Zug, um die Spielfigur zu bewegen.");
        }
        if (getTurnInfo().getTurnState() != TurnState.WAITING_FOR_MOVE) {
            throw new GameNotValidException(
                    "Es ist nicht die Phase, um die Spielfigur zu bewegen.");
        }
        if (targetCoordinates == null) {
            throw new TargetCoordinateNullException("Die Zielkoordinaten dürfen nicht null sein.");
        }
        if (targetCoordinates.getColumn() < 0 || targetCoordinates.getRow() < 0 ||
                targetCoordinates.getColumn() >= currentBoard.getCols()
                || targetCoordinates.getRow() >= currentBoard.getRows()) {
            throw new IllegalArgumentException("Zielkoordinaten liegen außerhalb des Spielfelds.");
        }

        PlayerState currentPlayerState = playerManager.getCurrentPlayerState();
        Coordinates currentPlayerCoordinates = currentPlayerState.getCurrentPosition();

        if (!useBeamBonus && !canPlayerMove(currentBoard, currentPlayerCoordinates, targetCoordinates)) {
            throw new NoValidActionException("Der Spieler kann sich nicht zu den Zielkoordinaten bewegen.");
        }

        currentPlayerState.setCurrentPosition(targetCoordinates);

        Tile targetTile = currentBoard.getTileAtCoordinate(targetCoordinates);

        if (targetTile != null && targetTile.getTreasure() != null &&
                targetTile.getTreasure().equals(currentPlayerState.getCurrentTreasure())) {
            try {
                currentPlayerState.collectCurrentTreasure();
                gameStatsManager.increaseTreasuresCollected(1, playerIdWhoMoved);
                if (currentPlayerState.getCurrentTreasure() != null) {
                    NextTreasureCardEvent nextTreasureEvent = new NextTreasureCardEvent(
                            currentPlayerState.getCurrentTreasure());
                    socketBroadcastService.sendMessageToSession(playerIdWhoMoved,
                            objectMapper.writeValueAsString(nextTreasureEvent));
                }

            } catch (IllegalStateException e) {
                throw new GameNotValidException("Fehler beim Sammeln des Schatzes: " + e.getMessage());
            }
        }

        if (currentPlayerState.getHomePosition().getColumn() == targetCoordinates.getColumn() &&
                currentPlayerState.getHomePosition().getRow() == targetCoordinates.getRow() &&
                currentPlayerState.getCurrentTreasure() == null) {
            gameStatsManager.addAmountToScoreForASinglePlayer(playerIdWhoMoved, 20);
            return endGameByTimeoutOrAfterCollectingAllTreasures();
        }

        if (targetTile != null && targetTile.getBonus() != null) {
            if (currentPlayerState.getAvailableBonuses().length < 5) {
                currentPlayerState.collectBonus(targetTile.getBonus());
                currentBoard.removeBonusFromTile(targetCoordinates);
            }
        }

        playerManager.setNextPlayerAsCurrent();
        if (boardItemPlacementService.trySpawnBonus(currentBoard, getTotalBonusCountsOnBoard())) {
            reduceTotalBonusCountsOnBoard(1);
        }

        AchievementContext ctx = new AchievementContext(currentPlayerState.getStepsTakenThisTurn(),
                currentPlayerState.getWasPushedOutLastRound(),
                currentPlayerState.getHasCollectedTreasureThisTurn());

        achievementManager.check(currentPlayerState, ctx);

        resetAllVariablesForNextTurn();

        informAllPlayersAboutCurrentGameState();

        return true;
    }

    public void resetAllVariablesForNextTurn() throws JsonProcessingException {
        PlayerState currentPlayerState = playerManager.getCurrentPlayerState();

        currentPlayerState.setStepsTakenThisTurn(0);
        currentPlayerState.consumePushedOutFlag();
        currentPlayerState.consumeCollectedTreasureFlag();

        getTurnInfo().setTurnState(TurnState.WAITING_FOR_PUSH);
        getTurnInfo().setCurrentPlayerId(playerManager.getCurrentPlayer().getId());
        getTurnInfo().updateTurnEndTime();
        turnTimer.resetTurnTimer();

    }

    public boolean handleRotateTile(String playerIdWhoRotated) throws GameNotValidException,
            NotPlayersTurnException, NoValidActionException, JsonProcessingException {

        if (!playerIdWhoRotated.equals(playerManager.getCurrentPlayer().getId())) {
            throw new NotPlayersTurnException(
                    "Es ist nicht dein Zug, um eine Kachel zu drehen.");
        }

        if (turnInfo.getTurnState() != TurnState.WAITING_FOR_PUSH) {
            throw new GameNotValidException(
                    "Es ist nicht die Phase, um Kacheln zu drehen.");
        }

        Tile spareTile = currentBoard.getSpareTile();
        spareTile.rotateClockwise();
        currentBoard.setSpareTile(spareTile);

        getTurnInfo().setTurnState(TurnState.WAITING_FOR_PUSH);

        informAllPlayersAboutCurrentGameState();

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
            throw new IllegalArgumentException("Start- oder Zielkoordinaten dürfen nicht null sein.");
        }

        /*
         * if (currentBoard.isAnyPlayerOnTile(target,
         * playerManager.getPlayerStatesOfPlayersNotOnTurn())) {
         * throw new
         * IllegalArgumentException("Das Zielfeld ist von einem anderen Spieler besetzt."
         * );
         * }
         */

        if (start.getColumn() == target.getColumn() && start.getRow() == target.getRow()) {
            return true;
        }

        int boardRows = board.getRows();
        int boardCols = board.getCols();
        Map<Coordinates, Integer> stepsMap = new java.util.HashMap<>();
        playerManager.getCurrentPlayerState().setStepsTakenThisTurn(0);

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
                    playerManager.getCurrentPlayerState().setStepsTakenThisTurn(stepsMap.get(current) + 1);
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
            TargetCoordinateNullException, JsonProcessingException, BonusNotAvailable {
        if (turnInfo.getTurnState() != TurnState.WAITING_FOR_MOVE) {
            if (!playerIdWhoUsedBeam.equals(playerManager.getCurrentPlayer().getId())) {
                throw new NotPlayersTurnException(
                        "Es ist nicht dein Zug, um den Strahl zu benutzen.");
            }
            throw new GameNotValidException(
                    "Es ist nicht die Phase, um den Strahl zu benutzen.");
        }
        if (targetCoordinates == null) {
            throw new TargetCoordinateNullException("Zielkoordinaten dürfen nicht null sein.");
        }
        if (targetCoordinates.getColumn() < 0 || targetCoordinates.getRow() < 0 ||
                targetCoordinates.getColumn() >= currentBoard.getCols()
                || targetCoordinates.getRow() >= currentBoard.getRows()) {
            throw new IllegalArgumentException("Zielkoordinaten liegen außerhalb des Spielfelds.");
        }

        PlayerState currentPlayerState = playerManager.getCurrentPlayerState();
        if (!currentPlayerState.hasBonusOfType(BonusType.BEAM)) {
            throw new BonusNotAvailable("Du hast keinen Strahl-Bonus zum Benutzen.");
        }

        currentPlayerState.useOneBonusOfType(BonusType.BEAM);

        return handleMovePawn(targetCoordinates, playerIdWhoUsedBeam, true);
    }

    public boolean handleUseSwap(String targetPlayerId, String playerIdWhoUsedSwap)
            throws GameNotValidException, NotPlayersTurnException, NoValidActionException,
            TargetCoordinateNullException, JsonProcessingException, BonusNotAvailable {
        if (!playerIdWhoUsedSwap.equals(playerManager.getCurrentPlayer().getId())) {
            throw new NotPlayersTurnException(
                    "Es ist nicht dein Zug, um den Tausch-Bonus zu benutzen.");
        }
        if (turnInfo.getTurnState() != TurnState.WAITING_FOR_MOVE) {
            throw new GameNotValidException(
                    "Es ist nicht die Phase, um den Tausch-Bonus zu benutzen.");
        }
        if (targetPlayerId == null || targetPlayerId.isEmpty()) {
            throw new NoValidActionException("Ziel-Spieler-ID darf nicht null oder leer sein.");
        }

        if (targetPlayerId.equals(playerIdWhoUsedSwap)) {
            throw new NoValidActionException("Du kannst die Position mit dir selbst nicht tauschen.");
        }

        PlayerState currentPlayerState = playerManager.getCurrentPlayerState();
        if (!currentPlayerState.hasBonusOfType(BonusType.SWAP)) {
            throw new BonusNotAvailable("Du hast keinen Tausch-Bonus zum Benutzen.");
        }

        PlayerState targetPlayerState = playerManager.getPlayerStateById(targetPlayerId);
        if (targetPlayerState == null) {
            throw new NoValidActionException("Der Ziel-Spieler existiert nicht.");
        }

        currentPlayerState.useOneBonusOfType(BonusType.SWAP);

        Coordinates currentPlayerPosition = currentPlayerState.getCurrentPosition();
        Coordinates targetPlayerPosition = targetPlayerState.getCurrentPosition();

        currentPlayerState.setCurrentPosition(targetPlayerPosition);
        targetPlayerState.setCurrentPosition(currentPlayerPosition);

        playerManager.setNextPlayerAsCurrent();
        if (boardItemPlacementService.trySpawnBonus(currentBoard, getTotalBonusCountsOnBoard())) {
            reduceTotalBonusCountsOnBoard(1);
        }

        getTurnInfo().setTurnState(TurnState.WAITING_FOR_PUSH);

        informAllPlayersAboutCurrentGameState();

        return true;
    }

    public boolean handleUsePushFixedTile(DirectionType direction, int rowOrColIndex, String playerIdWhoUsedPushFixed)
            throws GameNotValidException, IllegalArgumentException, NoExtraTileException,
            BonusNotAvailable, NoValidActionException, JsonProcessingException {
        if (!playerIdWhoUsedPushFixed.equals(playerManager.getCurrentPlayer().getId())) {
            throw new NotPlayersTurnException(
                    "Es ist nicht dein Zug, um eine feste Kachel zu schieben.");
        }
        if (turnInfo.getTurnState() != TurnState.WAITING_FOR_PUSH) {
            throw new GameNotValidException(
                    "Es ist nicht die Phase, um eine feste Kachel zu schieben.");
        }

        PlayerState currentPlayerState = playerManager.getCurrentPlayerState();
        if (!currentPlayerState.hasBonusOfType(BonusType.PUSH_FIXED)) {
            throw new BonusNotAvailable("Du hast keinen Bonus, um eine feste Kachel zu schieben.");
        }

        if (direction == null) {
            throw new NoDirectionForPush("Die Richtung für das Schieben darf nicht null sein.");
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
            throw new NoValidActionException("Es ist verboten die Startpositionen zu verschieben.");
        }

        currentPlayerState.useOneBonusOfType(BonusType.PUSH_FIXED);

        return handlePushTile(rowOrColIndex, direction, playerIdWhoUsedPushFixed, true);
    }

    public boolean handleUsePushTwice(String playerIdWhoUsedPushTwice)
            throws GameNotValidException, NotPlayersTurnException, BonusNotAvailable {
        if (!playerIdWhoUsedPushTwice.equals(playerManager.getCurrentPlayer().getId())) {
            throw new NotPlayersTurnException(
                    "Es ist nicht dein Zug, um zweimal zu schieben.");
        }
        if (turnInfo.getTurnState() != TurnState.WAITING_FOR_PUSH) {
            throw new GameNotValidException(
                    "Es ist nicht die Phase, um zweimal zu schieben.");
        }

        PlayerState currentPlayerState = playerManager.getCurrentPlayerState();
        if (!currentPlayerState.hasBonusOfType(BonusType.PUSH_TWICE)) {
            throw new BonusNotAvailable("Du hast keinen Bonus, um zweimal zu schieben.");
        }

        currentPlayerState.useOneBonusOfType(BonusType.PUSH_TWICE);

        pushTwiceUsedInCurrentTurn = true;

        return true;
    }

    public void informAllPlayersAboutCurrentGameState() throws JsonProcessingException {
        GameStateUpdate gameStatUpdate = new GameStateUpdate(currentBoard, playerManager.getNonNullPlayerStates(),
                getTurnInfo(), getGameEndTime());
        socketBroadcastService.broadcastMessage(objectMapper.writeValueAsString(gameStatUpdate));

        //PlayerTurnEvent turn = new PlayerTurnEvent(playerManager.getCurrentPlayer().getId(),
        //        currentBoard.getSpareTile(), 60);
        //socketBroadcastService.broadcastMessage(objectMapper.writeValueAsString(turn));
    }

    @EventListener
    public void onGameTimeout(GameTimeoutEvent event) {
        try {
            endGameByTimeoutOrAfterCollectingAllTreasures();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean endGameByTimeoutOrAfterCollectingAllTreasures() throws JsonProcessingException {
        gameTimerManager.stop();
        gameStatsManager.updateScoresForAllPlayersAtTheEndOfTheGame();
        gameStatsManager.updateRankForAllPlayersBasedOnScore();
        GameStateUpdate finalGameState = new GameStateUpdate(currentBoard,
                playerManager.getNonNullPlayerStates(), getTurnInfo(), getGameEndTime());
        socketBroadcastService.broadcastMessage(objectMapper.writeValueAsString(finalGameState));
        GameOverEvent gameOver = new GameOverEvent(gameStatsManager.getSortedRankings());
        if (gameOver.getWinnerId() != null) {
            socketBroadcastService.broadcastMessage(objectMapper.writeValueAsString(gameOver));
            getTurnInfo().setTurnState(TurnState.NOT_STARTED);
            setLobbyState(LobbyStateEnum.LOBBY);
        } else {
            throw new IllegalStateException("Die Gewinner-ID ist null, obwohl alle Schätze gesammelt wurden.");
        }
        for (PlayerInfo player : playerManager.getNonNullPlayers()) {
            achievementManager.broadCastAchievementsFromPlayerWithId(player.getId());
        }
        LobbyState lobbyState = new LobbyState(playerManager.getNonNullPlayers());
        socketBroadcastService.broadcastMessage(objectMapper.writeValueAsString(lobbyState));
        return true;
    }

    @EventListener
    public void onTurnTimeout(TurnTimeoutEvent event) {
        try {
            playerManager.setNextPlayerAsCurrent();
            resetAllVariablesForNextTurn();
            informAllPlayersAboutCurrentGameState();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

}
