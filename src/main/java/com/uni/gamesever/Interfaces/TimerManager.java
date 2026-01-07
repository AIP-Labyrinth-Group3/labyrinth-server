package com.uni.gamesever.Interfaces;

public interface TimerManager {
    void start(long durationInSeconds, Runnable onTimeOut);

    void stop();

    boolean isRunning();
}
