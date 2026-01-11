package com.uni.gamesever.domain.exceptions;

public class GameFullException extends GameNotValidException {
    public GameFullException(String message) {
        super(message);
    }
}
