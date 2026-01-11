package com.uni.gamesever.interfaces.Timer;

public interface TimerManager {
    void start(long durationInSeconds, Runnable onTimeOut);

    void stop();

    boolean isRunning();
}
