package com.uni.gamesever.classes;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.gamesever.models.LobbyState;
import com.uni.gamesever.models.PlayerInfo;
import com.uni.gamesever.models.messages.ConnectRequest;
import com.uni.gamesever.services.SocketBroadcastService;

@Service
public class ConnectionHandler {
    private final PlayerManager playerManager;
    private final SocketBroadcastService socketBroadcastService;

    public ConnectionHandler(PlayerManager playerManager, SocketBroadcastService socketBroadcastService) {
        this.playerManager = playerManager;
        this.socketBroadcastService = socketBroadcastService;
    }

    public int handleConnectMessage(ConnectRequest request, String userId) throws JsonProcessingException {
           if (playerManager.addPlayer(new PlayerInfo(userId, request.getUsername()))){
                System.out.println("User " + userId + " connected as " + request.getUsername());
                LobbyState lobbyState = new LobbyState("LOBBY_State", playerManager.getPlayers());
                ObjectMapper mapper = new ObjectMapper();
                String lobbyStateMessageToBroadcast = mapper.writeValueAsString(lobbyState);
                socketBroadcastService.broadcastMessage(lobbyStateMessageToBroadcast);
            } else {
                System.err.println("Game is full. User " + userId + " cannot join.");
                return -1;
           }
              return 1; 
    }

    public int handleDisconnectRequest(ConnectRequest request, String userId) throws JsonProcessingException {
        if (playerManager.removePlayer(request.getUsername())){
             System.out.println("User " + userId + " disconnected" + request.getUsername());
             LobbyState lobbyState = new LobbyState("LOBBY_State", playerManager.getPlayers());
             ObjectMapper mapper = new ObjectMapper();
             String lobbyStateMessageToBroadcast = mapper.writeValueAsString(lobbyState);
             socketBroadcastService.broadcastMessage(lobbyStateMessageToBroadcast);
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

