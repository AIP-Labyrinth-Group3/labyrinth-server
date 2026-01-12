package com.uni.gamesever.interfaces.Websocket.messages.server;

import com.uni.gamesever.domain.model.GameBoard;
import com.uni.gamesever.domain.model.PlayerState;
import com.uni.gamesever.domain.model.TurnInfo;
import com.uni.gamesever.interfaces.Websocket.messages.client.Message;

public class GameStarted extends Message {
    private GameBoard board;
    private PlayerState[] players;
    private TurnInfo turnInfo;
    private String gameEndTime;

    public GameStarted(GameBoard board, PlayerState[] players, TurnInfo turnInfo, String gameEndTime) {
        super("GAME_STARTED");
        this.board = board;
        this.players = players;
        this.turnInfo = turnInfo;
        this.gameEndTime = gameEndTime;
    }

    public GameBoard getBoard() {
        return board;
    }

    public PlayerState[] getPlayers() {
        return players;
    }

    public TurnInfo getTurnInfo() {
        return turnInfo;
    }

    public String getGameEndTime() {
        return gameEndTime;
    }
}
