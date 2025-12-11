package com.uni.gamesever.exceptions;

public class GameAlreadyStartedException extends NoValidActionException {
    public GameAlreadyStartedException(String message) {
        super(message);
    }
}
