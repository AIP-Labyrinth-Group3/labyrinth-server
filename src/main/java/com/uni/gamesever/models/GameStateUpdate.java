package com.uni.gamesever.models;

public class GameStateUpdate {
    private String type = "GAME_STATE_UPDATE";
    private GameBoard board;
    private PlayerState[] players;
    private String currentPlayerId;
    private String currentTurnState;

    public GameStateUpdate(GameBoard board, PlayerState[] players) {
        this.board = board;
        this.players = players;
        this.currentPlayerId = players[0].getPlayer().getId();
        this.currentTurnState = TurnState.WAITING_FOR_PUSH.name();
    }

    public String getType() {
        return type;
    }

    public GameBoard getBoard() {
        return board;
    }

    public PlayerState[] getPlayers() {
        return players;
    }

    public String getCurrentPlayerId() {
        return currentPlayerId;
    }

    public void setCurrentPlayerId(String currentPlayerId) {
        this.currentPlayerId = currentPlayerId;
    }

    public String getCurrentTurnState() {
        return currentTurnState;
    }

    public void setCurrentTurnState(String currentTurnState) {
        try {
            TurnState.valueOf(currentTurnState);
            this.currentTurnState = currentTurnState;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid turn state: " + currentTurnState);
        }
    }

    public void setBoard(GameBoard board) {
        this.board = board;
    }

    public void setPlayerState(PlayerState[] players) {
        this.players = players;
    }

}
