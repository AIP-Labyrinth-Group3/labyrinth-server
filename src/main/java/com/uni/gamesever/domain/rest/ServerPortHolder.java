package com.uni.gamesever.domain.rest;

import org.springframework.stereotype.Component;

@Component
public class ServerPortHolder {

    private int port;

    public int getPort() {
        return port;
    }

    void setPort(int port) {
        this.port = port;
    }
}