package com.uni.gamesever.models.messages;

import com.uni.gamesever.models.BoardSize;

public class StartGameAction extends Message {
    private int gameDurationInSeconds;
    private BoardSize boardSize;

    public StartGameAction() {}
    public StartGameAction(String action, int gameDurationInSeconds, BoardSize boardSize) {
        super(action);
        this.gameDurationInSeconds = gameDurationInSeconds;
        this.boardSize = boardSize;
    }
    public int getGameDurationInSeconds() {
        return gameDurationInSeconds;
    }
    public BoardSize getBoardSize() {
        return boardSize;
    }
}
