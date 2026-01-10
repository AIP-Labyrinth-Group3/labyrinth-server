package com.uni.gamesever.domain.exceptions;

public class UsernameNullOrEmptyException extends NoValidActionException {
    public UsernameNullOrEmptyException(String message) {
        super(message);
    }
}
