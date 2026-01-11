package com.uni.gamesever.domain.exceptions;

public class GameAlreadyStartedException extends NoValidActionException {
    public GameAlreadyStartedException(String message) {
        super(message);
    }
}
