package com.uni.gamesever.domain.rest;

import org.springframework.stereotype.Component;

@Component
public class LocalBaseUrlProvider {

    private final ServerPortHolder portHolder;

    public LocalBaseUrlProvider(ServerPortHolder portHolder) {
        this.portHolder = portHolder;
    }

    public String getBaseUrl() {
        return "http://localhost:" + portHolder.getPort();
    }

    public String url(String path) {
        if (path == null || path.isBlank()) return getBaseUrl();
        return getBaseUrl() + (path.startsWith("/") ? path : "/" + path);
    }
}