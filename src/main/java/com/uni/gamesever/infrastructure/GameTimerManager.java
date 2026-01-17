package com.uni.gamesever.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GameTimerManager extends AbstractTimerManager {
    private static final Logger log = LoggerFactory.getLogger("GAME_LOG");

    public void start(long durationInSeconds, Runnable onTimeout) {
        System.out.println("Starting Game Timer for " + durationInSeconds + " seconds");
        log.info("Starte Spieltimer für {} Sekunden", durationInSeconds);
        startInternal(durationInSeconds, onTimeout);
    }

    public void stop() {
        System.out.println("Stopping Game Timer");
        log.info("Stoppe Spieltimer");
        stopInternal();
    }

    public boolean isRunning() {
        return super.isRunning();
    }

}
