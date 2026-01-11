package com.uni.gamesever.domain.exceptions;

public class UserNotFoundException extends NoValidActionException {
    public UserNotFoundException(String message) {
        super(message);
    }

}
