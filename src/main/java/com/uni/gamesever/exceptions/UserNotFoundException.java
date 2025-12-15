package com.uni.gamesever.exceptions;

public class UserNotFoundException extends NoValidActionException {
    public UserNotFoundException(String message) {
        super(message);
    }

}
