package com.uni.gamesever.interfaces.Websocket.messages.server;

import com.uni.gamesever.domain.model.GameBoard;
import com.uni.gamesever.domain.model.PlayerState;
import com.uni.gamesever.domain.model.TurnInfo;
import com.uni.gamesever.interfaces.Websocket.messages.client.Message;

public class GameStateUpdate extends Message {
    private GameBoard board;
    private PlayerState[] players;
    private TurnInfo currentTurnInfo;
    private String gameEndTime;

    public GameStateUpdate(GameBoard board, PlayerState[] players, TurnInfo currentTurnInfo, String gameEndTime) {
        super("GAME_STATE_UPDATE");
        this.board = board;
        this.players = players;
        this.currentTurnInfo = currentTurnInfo;
        this.gameEndTime = gameEndTime;
    }

    public GameBoard getBoard() {
        return board;
    }

    public PlayerState[] getPlayers() {
        return players;
    }

    public TurnInfo getCurrentTurnInfo() {
        return currentTurnInfo;
    }

    public String getGameEndTime() {
        return gameEndTime;
    }

}
