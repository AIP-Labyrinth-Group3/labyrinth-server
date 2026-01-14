package com.uni.gamesever.interfaces.Websocket.messages.client;

public class ConnectRequest extends Message {
    private String username;
    private String identifierToken;

    public ConnectRequest() {
        super("CONNECT");
        this.username = "";
        this.identifierToken = "";
    }

    public ConnectRequest(String action, String username, String identifierToken) {
        super(action);
        this.username = username;
        this.identifierToken = identifierToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getIdentifierToken() {
        return identifierToken;
    }

    public void setIdentifierToken(String identifierToken) {
        this.identifierToken = identifierToken;
    }
}
