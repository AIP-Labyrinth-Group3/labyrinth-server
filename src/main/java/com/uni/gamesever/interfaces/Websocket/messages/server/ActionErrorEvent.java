package com.uni.gamesever.interfaces.Websocket.messages.server;

import com.uni.gamesever.domain.model.ErrorCode;
import com.uni.gamesever.interfaces.Websocket.messages.client.Message;

public class ActionErrorEvent extends Message {
    private ErrorCode errorCode;
    private String message;

    public ActionErrorEvent(ErrorCode errorCode, String message) {
        super("ACTION_ERROR");
        this.errorCode = errorCode;
        this.message = message;
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
