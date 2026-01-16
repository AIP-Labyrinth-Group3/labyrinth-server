package com.uni.gamesever.infrastructure;

import org.springframework.stereotype.Service;

@Service
public class ReconnectTimerManager extends AbstractTimerManager {
    public void start(long durationInSeconds, Runnable onTimeout) {
        startInternal(durationInSeconds, onTimeout);
    }

    public void stop() {
        stopInternal();
    }

    public boolean isRunning() {
        return super.isRunning();
    }
}
