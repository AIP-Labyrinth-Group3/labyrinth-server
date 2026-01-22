package com.uni.gamesever.domain.exceptions;

public class ConnectionRejectedException extends RuntimeException {
    public ConnectionRejectedException(String message) {
        super(message);
    }
}
