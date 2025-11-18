package com.uni.gamesever.models.messages;

public class ConnectRequest extends Message {
    private String username;

    // Default constructor for JSON deserialization
    public ConnectRequest() {}

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
