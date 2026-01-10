package com.uni.gamesever.interfaces.Websocket.messages.client;

public class ConnectRequest extends Message {
    private String username;

    public ConnectRequest() {
        this.username = "";
    }

    public ConnectRequest(String action, String username) {
        super(action);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
