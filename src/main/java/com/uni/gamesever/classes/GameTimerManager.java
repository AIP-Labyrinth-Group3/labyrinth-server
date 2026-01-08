package com.uni.gamesever.classes;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.uni.gamesever.Interfaces.TimerManager;

@Service
public class GameTimerManager implements TimerManager {

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> timeoutTask;

    @Override
    public synchronized void start(long durationInSeconds, Runnable onTimeout) {
        stop();

        System.out.println("[GameTimer] Starting game timer: " + durationInSeconds + " seconds");

        scheduler = Executors.newSingleThreadScheduledExecutor();

        timeoutTask = scheduler.schedule(() -> {
            System.out.println("[GameTimer] TIMEOUT reached!");
            if (onTimeout != null) {
                onTimeout.run();
            }
        }, durationInSeconds, TimeUnit.SECONDS);
    }

    @Override
    public synchronized void stop() {
        if (timeoutTask != null) {
            timeoutTask.cancel(false);
            timeoutTask = null;
        }
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
    }

    @Override
    public synchronized boolean isRunning() {
        return timeoutTask != null && !timeoutTask.isDone();
    }
}
