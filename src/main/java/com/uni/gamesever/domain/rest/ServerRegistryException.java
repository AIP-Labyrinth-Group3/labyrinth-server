package com.uni.gamesever.domain.rest;

import com.uni.gamesever.domain.rest.Dtos.*;

public class ServerRegistryException extends RuntimeException {
    private final int httpStatus;
    private final ErrorResponse error;

    public ServerRegistryException(int httpStatus, ErrorResponse error) {
        super(error != null ? error.message() : ("HTTP " + httpStatus));
        this.httpStatus = httpStatus;
        this.error = error;
    }

    public int getHttpStatus() { return httpStatus; }
    public ErrorResponse getError() { return error; }
}