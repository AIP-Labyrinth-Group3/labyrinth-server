package com.uni.gamesever.interfaces.Websocket.messages.server;

import com.uni.gamesever.interfaces.Websocket.messages.client.Message;

public class ConnectAck extends Message {
    public String playerId;
    private String identifierToken;

    public ConnectAck(String playerId, String identifierToken) {
        super("CONNECT_ACK");
        this.playerId = playerId;
        this.identifierToken = identifierToken;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getIdentifierToken() {
        return identifierToken;
    }
}
