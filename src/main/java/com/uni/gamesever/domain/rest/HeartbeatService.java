package com.uni.gamesever.domain.rest;

import com.uni.gamesever.domain.game.GameManager;
import com.uni.gamesever.domain.rest.Dtos.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private String host;
    private final AtomicReference<UUID> serverId = new AtomicReference<>(null);
    private static final Logger log = LoggerFactory.getLogger("GAME_LOG");

    public HeartbeatService(
            ServerRegistryClient registryClient,
            GameManager gameManager,
            ServerPortHolder serverPortHolder,
            @Value("${game-server.name:Group3}") String name,
            @Value("${game-server.uri:ws://}") String uriPrefix,
            @Value("${game-server.host:localhost}") String host,
            @Value("${game-server.max-players:4}") int maxPlayers

    ) {
        this.registryClient = registryClient;
        this.gameManager = gameManager;
        this.serverPortHolder = serverPortHolder;
        this.name = name;
        this.uriPrefix = uriPrefix;
        this.host = host;
        this.maxPlayers = maxPlayers;
        log.info("[HeartbeatService] Initialized with uriPrefix=" + this.uriPrefix);
    }

    private String fullUri() {

        Integer port = serverPortHolder.getPortOrThrow();
        if (port == null || port <= 0)
            throw new IllegalStateException("Server port not set yet!");
        return uriPrefix + host + ":" + port;  // /game deleted
    }

    @Scheduled(initialDelay = 1000, fixedDelayString = "#{1 * 1000}")
    public void heartbeatTick() {
        try {
            UUID id = serverId.get();
            int players = gameManager.getPlayerCount();
            ServerStatus status = ServerStatus.valueOf(gameManager.getLobbyState().name());

            if (id == null) {
                var created = registryClient.createServer(
                        new GameServerRegistration(name, fullUri(), maxPlayers));
                serverId.set(created.id());
                log.info("Server registriert mit ID {}", created.id());
                log.info("Server URI: {}", fullUri());
                return;
            }

            registryClient.updateServer(id, new GameServerUpdate(players, status));

        } catch (Exception e) {
            log.error("Fehler im Heartbeat: {}", e.getMessage());
        }
    }
}