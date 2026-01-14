package com.uni.gamesever.domain.rest;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.net.SocketException;

@Component
public class ConnectManagement implements CommandLineRunner {

    private final ServerRegistryClient client;
    private final ServerPortHolder localServerPortHolder;

    public ConnectManagement(ServerRegistryClient client, ServerPortHolder localServerPortHolder) {
        this.client = client;
        this.localServerPortHolder = localServerPortHolder;
    }

    @Override
    public void run(String... args) throws SocketException {
        WifiIPv4.getWifiIPv4();
        String localIp = "ws://localhost:"+localServerPortHolder.getPort()+"/game";

    }
}
