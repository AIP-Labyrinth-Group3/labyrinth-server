package com.uni.gamesever.models;

public class ActionErrorEvent {
    private String type = "ACTION_ERROR";
    private ErrorCode errorCode;
    private String message;

    public ActionErrorEvent(ErrorCode errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
