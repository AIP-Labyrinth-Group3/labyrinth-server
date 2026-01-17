package com.uni.gamesever;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.uni.gamesever.interfaces.Websocket.SocketConnectionHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;

@EnableScheduling
@SpringBootApplication
public class GameseverApplication {

    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(GameseverApplication.class);

        // Wichtig: Listener hinzufügen, bevor app.run() aufgerufen wird
        app.addListeners(new PortRangeEnvironmentListener());

        app.run(args);
    }

    static class PortRangeEnvironmentListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

        private static final Logger log = LoggerFactory.getLogger(SocketConnectionHandler.class);

        @Override
        public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
            ConfigurableEnvironment env = event.getEnvironment();

            String configuredPort = env.getProperty("server.port");
            if (configuredPort != null && !configuredPort.isBlank()) {
                log.info("Server-Port extern gesetzt: {}", configuredPort);
                return;
            }

            int min = env.getProperty("game-server.port.min", Integer.class, 8000);
            int max = env.getProperty("game-server.port.max", Integer.class, 8500);

            int port = findFreePortInRange(min, max);

            // Höchste Priorität: überschreibt server.port aus Dateien/Args
            env.getPropertySources().addFirst(
                    new MapPropertySource("portRangeOverride", Map.of("server.port", port)));
        }
    }

    private static int findFreePortInRange(int min, int max) {
        for (int p = min; p <= max; p++) {
            try (ServerSocket socket = new ServerSocket(p)) {
                socket.setReuseAddress(true);
                return p;
            } catch (IOException ignored) {
            }
        }
        throw new IllegalStateException("Kein freier Port im Bereich " + min + "-" + max);
    }
}
