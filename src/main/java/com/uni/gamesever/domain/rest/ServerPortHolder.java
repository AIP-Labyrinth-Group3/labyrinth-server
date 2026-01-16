package com.uni.gamesever.domain.rest;

import org.springframework.stereotype.Component;

@Component
public class ServerPortHolder {

    private volatile Integer port;

    //public int getPort() {
    //    return port;
    //}

    public void setPort(int port) {
        this.port = port;
    }

    public int getPortOrThrow() {
        Integer p = port;
        if (p == null || p <= 0) throw new IllegalStateException("Server port not set yet");
        return p;
    }

}