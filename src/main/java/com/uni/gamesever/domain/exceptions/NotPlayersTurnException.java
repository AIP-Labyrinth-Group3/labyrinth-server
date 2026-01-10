package com.uni.gamesever.domain.exceptions;

public class NotPlayersTurnException extends NoValidActionException {
    public NotPlayersTurnException(String message) {
        super(message);
    }

}
