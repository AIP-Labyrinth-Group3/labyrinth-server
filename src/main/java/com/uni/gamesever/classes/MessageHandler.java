package com.uni.gamesever.classes;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.gamesever.models.messages.ConnectRequest;
import com.uni.gamesever.models.messages.Message;
import com.uni.gamesever.models.messages.StartGameAction;
import com.uni.gamesever.services.SocketBroadcastService;

@Service
public class MessageHandler {

    private final PlayerManager playerManager;

    private final ConnectionHandler connectionHandler;
    private final GameInitialitionController gameBoardHandler;

    public MessageHandler(SocketBroadcastService socketBroadcastService, PlayerManager playerManager, GameInitialitionController gameBoardHandler, ConnectionHandler connectionHandler) {
        this.connectionHandler = connectionHandler;
        this.gameBoardHandler = gameBoardHandler;
        this.playerManager = playerManager;
    }

    public int handleClientMessage(String message, String userId) throws JsonMappingException, JsonProcessingException {
        //parsing the client message into a connectRequest object
        System.out.println("Received message from user " + userId + ": " + message);
        ObjectMapper objectMapper = new ObjectMapper();
        Message request;
        try {
             request = objectMapper.readValue(message, Message.class);
        } catch (JsonMappingException e) {
            System.err.println("Failed to parse message from user " + userId + ": " + e.getMessage());
            return -1;
        }

       switch (request.getType()) {
            // connect action from client
           case "CONNECT":
                //convert message to connectRequest
                ConnectRequest connectReq = objectMapper.readValue(message, ConnectRequest.class);
                return connectionHandler.handleConnectMessage(connectReq, userId);
            case "DISCONNECT":
                ConnectRequest disconnectRequest = objectMapper.readValue(message, ConnectRequest.class);
                return connectionHandler.handleDisconnectRequest(disconnectRequest, userId);
            case "START_GAME":
                StartGameAction startGameReq = objectMapper.readValue(message, StartGameAction.class);
                //check if the message sender is the administrator
               if(userId.equals(playerManager.getPlayers()[0].getId())){
                   return gameBoardHandler.handleStartGameMessage(startGameReq.getBoardSize());
                } else {
                   System.err.println("User " + userId + " is not authorized to start the game.");
                   return -1;
                }
           default:
               return -1;
         }
    }

}
