package com.uni.gamesever.interfaces.Websocket.messages.client;

import com.uni.gamesever.domain.model.BoardSize;

public class StartGameRequest extends Message {
    private int gameDurationInSeconds;
    private BoardSize boardSize;
    private int treasureCardCount;
    private int totalBonusCount;

    public StartGameRequest() {
        super("START_GAME");
        this.boardSize = new BoardSize(7, 7);
        this.gameDurationInSeconds = 3600;
        this.treasureCardCount = 24;
        this.totalBonusCount = 0;
    }

    public StartGameRequest(int gameDurationInSeconds, BoardSize boardSize, int treasureCardCount,
            int totalBonusCount) {
        super("START_GAME");
        this.gameDurationInSeconds = gameDurationInSeconds;
        this.boardSize = boardSize;
        this.treasureCardCount = treasureCardCount;
        this.totalBonusCount = totalBonusCount;
    }

    public int getGameDurationInSeconds() {
        return gameDurationInSeconds;
    }

    public BoardSize getBoardSize() {
        return boardSize;
    }

    public int getTreasureCardCount() {
        return treasureCardCount;
    }

    public int getTotalBonusCount() {
        return totalBonusCount;
    }

    public void setTreasureCardCount(int treasureCardCount) throws IllegalArgumentException {
        if (treasureCardCount < 2 || treasureCardCount > 24) {
            throw new IllegalArgumentException("Die Anzahl der Schatzkarten muss zwischen 2 und 24 liegen.");
        }
        this.treasureCardCount = treasureCardCount;
    }

    public void setGameDurationInSeconds(int gameDurationInSeconds) {
        if (gameDurationInSeconds < 0) {
            throw new IllegalArgumentException("Die Spieldauer muss positiv sein.");
        }
        this.gameDurationInSeconds = gameDurationInSeconds;
    }

}
