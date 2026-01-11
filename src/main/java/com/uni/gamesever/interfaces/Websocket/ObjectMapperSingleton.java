package com.uni.gamesever.interfaces.Websocket;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectMapperSingleton {
    private static ObjectMapper instance = new ObjectMapper();

    private ObjectMapperSingleton() {
    }

    public static ObjectMapper getInstance() {
        return instance;
    }
}
