package com.uni.gamesever.classes;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.gamesever.models.ConnectionAck;
import com.uni.gamesever.models.LobbyState;
import com.uni.gamesever.models.PlayerInfo;
import com.uni.gamesever.models.messages.ConnectRequest;
import com.uni.gamesever.services.SocketMessageService;

@Service
public class ConnectionHandler {
    private final PlayerManager playerManager;
    private final SocketMessageService socketMessageService;
    ObjectMapper objectMapper = new ObjectMapper();

    public ConnectionHandler(PlayerManager playerManager, SocketMessageService socketMessageService) {
        this.playerManager = playerManager;
        this.socketMessageService = socketMessageService;
    }

    public int handleConnectMessage(ConnectRequest request, String userId) throws JsonProcessingException {
            PlayerInfo newPlayer = new PlayerInfo(userId);
            try{
                newPlayer.setName(request.getUsername());
            }
            catch (IllegalArgumentException e){
                System.err.println("Invalid username for user " + userId + ": " + request.getUsername());
                return -1;
            }
           if (playerManager.addPlayer(newPlayer)){
                System.out.println("User " + userId + " connected as " + request.getUsername());
                ConnectionAck connectionAck = new ConnectionAck(newPlayer.getId());
                socketMessageService.sendMessageToSession(userId, objectMapper.writeValueAsString(connectionAck));         

                LobbyState lobbyState = new LobbyState(playerManager.getNonNullPlayers());
                socketMessageService.broadcastMessage(objectMapper.writeValueAsString(lobbyState));
            } else {
                System.err.println("Game is full. User " + userId + " cannot join.");
                return -1;
           }
              return 1; 
    }

    public int handleDisconnectRequest(ConnectRequest request, String userId) throws JsonProcessingException {
        if (playerManager.removePlayer(request.getUsername())){
             System.out.println("User " + userId + " disconnected" + request.getUsername());
             LobbyState lobbyState = new LobbyState(playerManager.getPlayers());
             socketMessageService.broadcastMessage(objectMapper.writeValueAsString(lobbyState));
         } else {
             System.err.println("User " + userId + " not found in player list.");
             return -1;
        }
           return 1;
    }

    public void processGameAction(String action, String userId) {
        // Implement game action processing logic here
        System.out.println("Processing action: " + action + " for user: " + userId);
    }


}

