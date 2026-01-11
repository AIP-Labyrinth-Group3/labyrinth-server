package com.uni.gamesever.interfaces.Websocket.messages.server;

public class ConnectAck {
    public String type = "CONNECT_ACK";
    public String playerId;

    public ConnectAck(String playerId) {
        this.playerId = playerId;
    }

    public String getPlayerId() {
        return playerId;
    }
}
