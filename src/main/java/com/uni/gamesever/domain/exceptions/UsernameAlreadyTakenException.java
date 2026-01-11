package com.uni.gamesever.domain.exceptions;

public class UsernameAlreadyTakenException extends NoValidActionException {
    public UsernameAlreadyTakenException(String message) {
        super(message);
    }
}
