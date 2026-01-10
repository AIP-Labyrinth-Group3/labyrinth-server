package com.uni.gamesever.domain.exceptions;

public class PlayerNotAdminException extends GameNotValidException {
    public PlayerNotAdminException(String message) {
        super(message);
    }

}
