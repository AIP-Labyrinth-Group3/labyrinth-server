package com.uni.gamesever.interfaces.Websocket;

import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ShutdownState {

    private volatile boolean shuttingDown = false;

    public boolean isShuttingDown() {
        return shuttingDown;
    }

    @EventListener
    public void onContextClosed(ContextClosedEvent event) {
        shuttingDown = true;
    }
}
