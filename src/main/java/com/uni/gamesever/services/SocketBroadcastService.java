package com.uni.gamesever.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

@Service
public class SocketBroadcastService {
    // In this list all the connections will be stored
    // Then it will be used to broadcast the message
    private final List<WebSocketSession> webSocketSessions = Collections.synchronizedList(new ArrayList<>());

    public void addIncomingSession(WebSocketSession session) {
        webSocketSessions.add(session);
    }

    public void removeDisconnectedSession(WebSocketSession session) {
        webSocketSessions.remove(session);
    }

    public void broadcastMessage(String message) {
        for (WebSocketSession s : webSocketSessions) {
            try {
                if (s.isOpen()) {
                    s.sendMessage(new org.springframework.web.socket.TextMessage(message));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
