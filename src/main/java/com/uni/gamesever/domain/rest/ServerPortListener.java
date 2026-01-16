package com.uni.gamesever.domain.rest;

import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
@Component
public class ServerPortListener {

    private final ServerPortHolder portHolder;

    public ServerPortListener(ServerPortHolder portHolder) {
        this.portHolder = portHolder;
    }

    @EventListener
    public void onApplicationEvent(ServletWebServerInitializedEvent  event) {
        portHolder.setPort(event.getWebServer().getPort());
    }
}