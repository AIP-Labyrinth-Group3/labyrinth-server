package com.uni.gamesever.domain.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ServerRegistryClientConfig {

    @Bean
    WebClient serverRegistryWebClient(
            WebClient.Builder builder,
            @Value("${server-registry.base-url}") String baseUrl
    ) {
        return builder.baseUrl(baseUrl).build();
    }
}