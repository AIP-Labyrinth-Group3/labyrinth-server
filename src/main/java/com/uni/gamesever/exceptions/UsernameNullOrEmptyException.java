package com.uni.gamesever.exceptions;

public class UsernameNullOrEmptyException extends NoValidActionException {
    public UsernameNullOrEmptyException(String message) {
        super(message);
    }
}
