package com.uni.gamesever.domain.ai;

import com.uni.gamesever.domain.model.*;

import java.util.List;

/**
 * GameState wrapper for AI decision making
 * Aggregates server models into a format the AI can work with
 */
public class GameState {
    private Tile[][] board;
    private List<PlayerState> players;
    private PlayerState myPlayerState;
    private String myPlayerId;
    private TurnState currentTurnState;
    private LastPush lastPush;

    // Getters / Setters
    public Tile[][] getBoard() {
        return board;
    }

    public void setBoard(Tile[][] board) {
        this.board = board;
    }

    public List<PlayerState> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerState> players) {
        this.players = players;
    }

    public PlayerState getMyPlayerState() {
        return myPlayerState;
    }

    public void setMyPlayerState(PlayerState myPlayerState) {
        this.myPlayerState = myPlayerState;
    }

    public String getMyPlayerId() {
        return myPlayerId;
    }

    public void setMyPlayerId(String myPlayerId) {
        this.myPlayerId = myPlayerId;
    }

    public TurnState getCurrentTurnState() {
        return currentTurnState;
    }

    public void setCurrentTurnState(TurnState currentTurnState) {
        this.currentTurnState = currentTurnState;
    }

    public LastPush getLastPush() {
        return lastPush;
    }

    public void setLastPush(LastPush lastPush) {
        this.lastPush = lastPush;
    }
}
