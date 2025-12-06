package com.uni.gamesever.models.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.uni.gamesever.models.BoardSize;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StartGameAction extends Message {
    private int gameDurationInSeconds;
    private BoardSize boardSize;
    private int treasureCardCount;
    private int totalBonusCount;

    public StartGameAction() {}
    public StartGameAction(int gameDurationInSeconds, BoardSize boardSize) {
        super("START_GAME");
        this.gameDurationInSeconds = gameDurationInSeconds;
        this.boardSize = boardSize;
        this.treasureCardCount = 24;
        this.totalBonusCount = 0;
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
        if(treasureCardCount < 2 || treasureCardCount > 24) {
            throw new IllegalArgumentException("Treasure card count must be between 2 and 24.");
        }
        this.treasureCardCount = treasureCardCount;
    }

}

