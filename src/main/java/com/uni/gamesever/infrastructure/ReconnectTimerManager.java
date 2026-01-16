package com.uni.gamesever.infrastructure;

import org.springframework.stereotype.Service;

@Service
public class ReconnectTimerManager extends AbstractTimerManager {
    public void start(long durationInSeconds, Runnable onTimeout) {
        System.out.println("Starting Reconnect Timer for " + durationInSeconds + " seconds");
        startInternal(durationInSeconds, onTimeout);
    }

    public void stop() {
        System.out.println("Stopping Reconnect Timer");
        stopInternal();
    }

    public boolean isRunning() {
        return super.isRunning();
    }
}
