package com.uni.gamesever.exceptions;

public class NotPlayersTurnException extends NoValidActionException {
    public NotPlayersTurnException(String message) {
        super(message);
    }
    
}
