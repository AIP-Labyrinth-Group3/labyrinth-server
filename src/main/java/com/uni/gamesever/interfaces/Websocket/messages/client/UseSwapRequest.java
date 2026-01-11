package com.uni.gamesever.interfaces.Websocket.messages.client;

public class UseSwapRequest extends Message {
    private String targetPlayerId;

    public UseSwapRequest() {
        super("USE_SWAP");
        this.targetPlayerId = null;
    }

    public UseSwapRequest(String targetPlayerId) {
        super("USE_SWAP");
        this.targetPlayerId = targetPlayerId;
    }

    public String getTargetPlayerId() {
        return targetPlayerId;
    }

    public void setTargetPlayerId(String targetPlayerId) {
        this.targetPlayerId = targetPlayerId;
    }
}
