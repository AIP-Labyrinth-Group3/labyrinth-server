package com.uni.gamesever.interfaces.Websocket.messages.client;

public class ToggleAiCommand extends Message {
    private boolean enabled;

    public ToggleAiCommand() {
        super("TOGGLE_AI");
        this.enabled = false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
