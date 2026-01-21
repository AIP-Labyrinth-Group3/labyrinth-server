package com.uni.gamesever.domain.game;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.uni.gamesever.infrastructure.TurnTimerManager;
import com.uni.gamesever.domain.events.TurnTimeoutEvent;

@Service
public class TurnTimer {

    private final TurnTimerManager turnTimerManager;
    private final ApplicationEventPublisher eventPublisher;

    private static final int TURN_DURATION = 60;

    public TurnTimer(TurnTimerManager turnTimerManager,
            ApplicationEventPublisher eventPublisher) {
        this.turnTimerManager = turnTimerManager;
        this.eventPublisher = eventPublisher;
    }

    public void resetTurnTimer() {
        turnTimerManager.stop();
        turnTimerManager.start(TURN_DURATION, () -> {
            eventPublisher.publishEvent(new TurnTimeoutEvent());
        });
    }

    public void stop() {
        turnTimerManager.stop();
    }
}
