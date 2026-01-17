package com.uni.gamesever.infrastructure;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

import java.util.concurrent.ScheduledFuture;

@Service
public class ReconnectTimerManager {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    private final Map<String, ScheduledFuture<?>> reconnectTimers = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger("GAME_LOG");

    public void start(String playerId, long timeoutSeconds, Runnable onTimeout) {
        System.out.println("Starting reconnect timer for player: " + playerId);
        log.info("Starte Wiederverbindungstimer für Spieler: {}", playerId);
        stop(playerId);

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            reconnectTimers.remove(playerId);
            onTimeout.run();
        }, timeoutSeconds, TimeUnit.SECONDS);

        reconnectTimers.put(playerId, future);
    }

    public void stop(String playerId) {
        ScheduledFuture<?> future = reconnectTimers.remove(playerId);
        if (future != null) {
            future.cancel(false);
        }
    }

    public boolean isRunning(String playerId) {
        ScheduledFuture<?> future = reconnectTimers.get(playerId);
        return future != null && !future.isDone();
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }
}
