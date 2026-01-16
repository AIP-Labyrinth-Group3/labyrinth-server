package com.uni.gamesever.domain.rest;

import com.uni.gamesever.domain.game.GameManager;
import com.uni.gamesever.domain.rest.Dtos.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class HeartbeatService {

    private final ServerRegistryClient registryClient;
    private final GameManager gameManager;
    private final ServerPortHolder serverPortHolder;

    private final String name;
    private final String uriPrefix;
    private final int maxPlayers;

    private final AtomicReference<UUID> serverId = new AtomicReference<>(null);

    public HeartbeatService(
            ServerRegistryClient registryClient,
            GameManager gameManager,
            ServerPortHolder serverPortHolder,
            @Value("${game-server.name:Group3}") String name,
            @Value("${game-server.uri:ws://localhost:9000}") String uriPrefix,
            @Value("${game-server.max-players:4}") int maxPlayers) {
        this.registryClient = registryClient;
        this.gameManager = gameManager;
        this.serverPortHolder = serverPortHolder;
        this.name = name;
        this.uriPrefix = uriPrefix;
        this.maxPlayers = maxPlayers;
    }

    private String fullUri() {
        int port = serverPortHolder.getPort();
        if (port <= 0)
            throw new IllegalStateException("Der Port des Servers ist nicht gesetzt.");
        return uriPrefix + "/game";
    }

    @Scheduled(initialDelay = 1000, fixedDelayString = "#{${server-registry.heartbeat-seconds:10} * 1000}")
    public void heartbeatTick() {
        try {
            UUID id = serverId.get();
            int players = gameManager.getPlayerCount();
            ServerStatus status = ServerStatus.valueOf(gameManager.getLobbyState().name());

            if (id == null) {
                var created = registryClient.createServer(
                        new GameServerRegistration(name, fullUri(), maxPlayers));
                serverId.set(created.id());
                System.out.println("[Heartbeat] Registered serverId=" + created.id());
                return;
            }

            registryClient.updateServer(id, new GameServerUpdate(players, status));

        } catch (Exception e) {
            System.out.println("[Heartbeat] " + e.getMessage());
        }
    }
}