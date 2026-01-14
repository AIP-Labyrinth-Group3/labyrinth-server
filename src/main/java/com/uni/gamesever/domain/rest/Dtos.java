package com.uni.gamesever.domain.rest;

import java.time.Instant;
import java.util.UUID;


/**
 * Sammlung aller DTOs für den Server-Registry-Client
 */
public final class Dtos {

    private Dtos() {} // keine Instanzen

    // ===== Enums =====
    public enum ServerStatus {
        LOBBY,
        IN_GAME,
        FINISHED
    }

    // ===== Request DTOs =====
    public record GameServerRegistration(
            String name,
            String uri,
            Integer maxPlayers
    ) {}

    public record GameServerUpdate(
            Integer currentPlayerCount,
            ServerStatus status
    ) {}

    // ===== Response DTOs =====
    public record GameServer(
            UUID id,
            String name,
            String uri,
            Integer maxPlayers,
            Integer currentPlayerCount,
            ServerStatus status,
            Instant lastSeen
    ) {}

    public record ErrorResponse(
            String message,
            Integer code,
            Instant timestamp
    ) {}
}