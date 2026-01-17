package com.uni.gamesever.domain.rest;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ConnectManagement {

    private final ServerPortHolder localServerPortHolder;

    public ConnectManagement(ServerRegistryClient client, ServerPortHolder localServerPortHolder) {
        this.localServerPortHolder = localServerPortHolder;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {

        try {
            int port = localServerPortHolder.getPortOrThrow();

        } catch (IllegalStateException e) {

        }
    }

}