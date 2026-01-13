package com.uni.gamesever.domain.rest;


import com.uni.gamesever.domain.rest.Dtos.*;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class ServerRegistryClient {

    private final WebClient webClient;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    public ServerRegistryClient(WebClient serverRegistryWebClient) {
        this.webClient = serverRegistryWebClient;
    }

    private <T> Mono<T> handleErrors(WebClient.ResponseSpec spec, Class<T> bodyClass) {
        return spec
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(ErrorResponse.class)
                                .defaultIfEmpty(new ErrorResponse("Request failed", resp.statusCode().value(), null))
                                .flatMap(err -> Mono.error(new ServerRegistryException(resp.statusCode().value(), err)))
                )
                .bodyToMono(bodyClass);
    }

    public List<GameServer> listServers() {
        var spec = webClient.get()
                .uri("/servers")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve();

        return spec
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(ErrorResponse.class)
                                .defaultIfEmpty(new ErrorResponse("Request failed", resp.statusCode().value(), null))
                                .flatMap(err -> Mono.error(new ServerRegistryException(resp.statusCode().value(), err)))
                )
                .bodyToFlux(GameServer.class)
                .collectList()
                .timeout(TIMEOUT)
                .block();
    }

    public GameServer createServer(GameServerRegistration registration) {
        var spec = webClient.post()
                .uri("/servers")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(registration)
                .retrieve();

        return handleErrors(spec, GameServer.class)
                .timeout(TIMEOUT)
                .block();
    }

    public GameServer updateServer(UUID serverId, GameServerUpdate update) {
        var spec = webClient.put()
                .uri("/servers/{serverId}", serverId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(update)
                .retrieve();

        return handleErrors(spec, GameServer.class)
                .timeout(TIMEOUT)
                .block();
    }

    public void deleteServer(UUID serverId) {
        webClient.delete()
                .uri("/servers/{serverId}", serverId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(ErrorResponse.class)
                                .defaultIfEmpty(new ErrorResponse("Request failed", resp.statusCode().value(), null))
                                .flatMap(err -> Mono.error(new ServerRegistryException(resp.statusCode().value(), err)))
                )
                .toBodilessEntity()
                .timeout(TIMEOUT)
                .block();
    }
}
