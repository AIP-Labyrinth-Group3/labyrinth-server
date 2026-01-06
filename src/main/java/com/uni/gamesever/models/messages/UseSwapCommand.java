package com.uni.gamesever.models.messages;

public class UseSwapCommand extends Message {
    private String targetPlayerId;

    public UseSwapCommand() {
        super("USE_SWAP");
        this.targetPlayerId = null;
    }

    public UseSwapCommand(String targetPlayerId) {
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
