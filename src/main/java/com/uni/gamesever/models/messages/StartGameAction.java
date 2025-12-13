package com.uni.gamesever.models.messages;

import com.uni.gamesever.models.BoardSize;

public class StartGameAction extends Message {
    private int gameDurationInSeconds;
    private BoardSize boardSize;
    private int treasureCardCount;
    private int totalBonusCount;

    public StartGameAction() {
        super("START_GAME");
        this.boardSize = new BoardSize(7, 7);
        this.gameDurationInSeconds = 600;
        this.treasureCardCount = 24;
        this.totalBonusCount = 0;
    }

    public StartGameAction(int gameDurationInSeconds, BoardSize boardSize, int treasureCardCount, int totalBonusCount) {
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
        this.treasureCardCount = treasureCardCount;
    }

}
