package com.uni.gamesever.domain.exceptions;

public class NotEnoughPlayerException extends GameNotValidException {
    public NotEnoughPlayerException(String message) {
        super(message);
    }

}
