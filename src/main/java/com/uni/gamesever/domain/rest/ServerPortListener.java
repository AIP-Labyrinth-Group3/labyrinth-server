package com.uni.gamesever.domain.rest;


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
    public void onApplicationEvent(ServletWebServerInitializedEvent event) {
        int port = event.getWebServer().getPort();
        portHolder.setPort(port);
    }
}