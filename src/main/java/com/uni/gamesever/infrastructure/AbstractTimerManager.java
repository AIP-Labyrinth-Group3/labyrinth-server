package com.uni.gamesever.infrastructure;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class AbstractTimerManager {

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> timeoutTask;

    protected synchronized void startInternal(long durationInSeconds, Runnable onTimeout) {
        stopInternal();

        scheduler = Executors.newSingleThreadScheduledExecutor();

        timeoutTask = scheduler.schedule(() -> {
            System.out.println("TIMEOUT reached!");
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
