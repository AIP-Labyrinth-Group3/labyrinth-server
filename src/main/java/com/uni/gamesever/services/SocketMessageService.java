package com.uni.gamesever.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

@Service
public class SocketMessageService {
    // In this list all the connections will be stored
    // Then it will be used to broadcast the message
    private final List<WebSocketSession> webSocketSessions = Collections.synchronizedList(new ArrayList<>());

    public void addIncomingSession(WebSocketSession session) {
        webSocketSessions.add(session);
    }

    public void removeDisconnectedSession(WebSocketSession session) {
        webSocketSessions.remove(session);
    }

    public void sendMessageToSession(String sessionId, String message) {
        WebSocketSession session = null;
        for (WebSocketSession s : webSocketSessions) {
            if (s.getId().equals(sessionId)) {
                session = s;
                break;
            }
        }
        try {
            if (session != null && session.isOpen()) {
                session.sendMessage(new org.springframework.web.socket.TextMessage(message));
            }
        } catch (Exception e) {
            System.out.println("Fehler beim Senden der Nachricht an die Sitzung: " + e.getMessage());
        }
    }

    public void broadcastMessage(String message) {
        for (WebSocketSession s : webSocketSessions) {
            try {
                if (s.isOpen()) {
                    s.sendMessage(new org.springframework.web.socket.TextMessage(message));
                }
            } catch (Exception e) {
                System.out.println("Fehler beim Senden der Nachricht an die Sitzung: " + e.getMessage());
            }
        }
    }
}
