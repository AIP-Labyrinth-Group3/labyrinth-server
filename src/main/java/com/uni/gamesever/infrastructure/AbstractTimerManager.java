package com.uni.gamesever.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class AbstractTimerManager {

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> timeoutTask;
    private static final Logger log = LoggerFactory.getLogger("GAME_LOG");
    protected synchronized void startInternal(long durationInSeconds, Runnable onTimeout) {
        stopInternal();

        scheduler = Executors.newSingleThreadScheduledExecutor();

        timeoutTask = scheduler.schedule(() -> {
            log.info("TIMEOUT reached!");
            if (onTimeout != null) {
                onTimeout.run();
            }
        }, durationInSeconds, TimeUnit.SECONDS);
    }

    protected synchronized void stopInternal() {
        if (timeoutTask != null) {
            timeoutTask.cancel(false);
            timeoutTask = null;
        }
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
    }

    public synchronized boolean isRunning() {
        return timeoutTask != null && !timeoutTask.isDone();
    }
}
