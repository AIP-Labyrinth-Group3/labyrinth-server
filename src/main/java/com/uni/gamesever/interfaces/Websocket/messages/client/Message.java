package com.uni.gamesever.interfaces.Websocket.messages.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {
    private String type;

    public Message() {
    }

    public Message(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
