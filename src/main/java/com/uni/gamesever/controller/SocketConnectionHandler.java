package com.uni.gamesever.controller;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.gamesever.classes.MessageHandler;
import com.uni.gamesever.models.ServerInfoEvent;
import com.uni.gamesever.services.SocketMessageService;

// Socket-Connection Configuration class
public class SocketConnectionHandler extends TextWebSocketHandler {
    private final SocketMessageService socketBroadcastService;
    private final MessageHandler messageHandler;
    @Value("${server.version}")
    private String serverVersion;
    @Value("${protocol.version}")
    private String protocolVersion;
    @Value("${server.motd}")
    private String serverMotd;
    private final ObjectMapper objectmapper = new ObjectMapper();

    public SocketConnectionHandler(SocketMessageService socketBroadcastService, MessageHandler messageHandler) {
        this.socketBroadcastService = socketBroadcastService;
        this.messageHandler = messageHandler;
    }

    // This method is executed when client tries to connect
    // to the sockets
    @Override
    public void afterConnectionEstablished(WebSocketSession session)
            throws Exception {

        super.afterConnectionEstablished(session);
        try {
            socketBroadcastService.addIncomingSession(session);
            System.out.println(session.getId() + " Connected");
            ServerInfoEvent serverInfoEvent = new ServerInfoEvent(Instant.now().toString(), serverVersion,
                    protocolVersion, serverMotd);
            socketBroadcastService.sendMessageToSession(session.getId(),
                    objectmapper.writeValueAsString(serverInfoEvent));
        } catch (Exception e) {
            throw e;
        }
    }

    // When client disconnect from WebSocket then this
    // method is called
    @Override
    public void afterConnectionClosed(WebSocketSession session,
            CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        try {
            socketBroadcastService.removeDisconnectedSession(session);
            System.out.println(session.getId() + " DisConnected");
        } catch (Exception e) {
            throw e;
        }
    }

    // It will handle exchanging of message in the network
    // It will have a session info who is sending the
    // message Also the Message object passes as parameter
    @Override
    public void handleMessage(WebSocketSession session,
            WebSocketMessage<?> message)
            throws Exception {

        super.handleMessage(session, message);

        System.out.println("Message Received from user " + session.getId() + ": " + message.getPayload());

        // the whole game logic goes here
        if (!messageHandler.handleClientMessage(message.getPayload().toString(), session.getId())) {
            // we return a error message to the client

        }
    }
}
