package com.uni.gamesever.infrastructure;

import org.springframework.stereotype.Service;

@Service
public class GameTimerManager extends AbstractTimerManager {

    public void start(long durationInSeconds, Runnable onTimeout) {
        System.out.println("Starting Game Timer for " + durationInSeconds + " seconds");
        startInternal(durationInSeconds, onTimeout);
    }

    public void stop() {
        System.out.println("Stopping Game Timer");
        stopInternal();
    }

    public boolean isRunning() {
        return super.isRunning();
    }

}
