package com.uni.gamesever.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GameTimerManager extends AbstractTimerManager {
    private static final Logger log = LoggerFactory.getLogger("GAME_LOG");

    public void start(long durationInSeconds, Runnable onTimeout) {
        log.info("Starte Spieltimer für {} Sekunden", durationInSeconds);
        startInternal(durationInSeconds, onTimeout);
    }

    public void stop() {
        log.info("Stoppe Spieltimer");
        stopInternal();
    }

    public boolean isRunning() {
        return super.isRunning();
    }

}
