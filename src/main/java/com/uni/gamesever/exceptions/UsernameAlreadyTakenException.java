package com.uni.gamesever.exceptions;

public class UsernameAlreadyTakenException extends NoValidActionException {
    public UsernameAlreadyTakenException(String message) {
        super(message);
    }
}
