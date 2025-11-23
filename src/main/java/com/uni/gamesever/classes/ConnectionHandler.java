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

    public ConnectionHandler(PlayerManager playerManager, SocketMessageService socketMessageService) {
        this.playerManager = playerManager;
        this.socketMessageService = socketMessageService;
    }

    public int handleConnectMessage(ConnectRequest request, String userId) throws JsonProcessingException {
            PlayerInfo newPlayer = new PlayerInfo(userId);
            if(newPlayer.setName(request.getUsername()) == -1){
                System.err.println("Invalid username length for user " + userId + ": " + request.getUsername());
                return -1;
            }
            newPlayer.setName(request.getUsername());
           if (playerManager.addPlayer(newPlayer)){
                System.out.println("User " + userId + " connected as " + request.getUsername());



                ConnectionAck connectionAck = new ConnectionAck(newPlayer.getId());
                ObjectMapper mapper = new ObjectMapper();
                socketMessageService.sendMessageToSession(userId, mapper.writeValueAsString(connectionAck));         

                LobbyState lobbyState = new LobbyState("LOBBY_State", playerManager.getPlayers());
                String lobbyStateMessageToBroadcast = mapper.writeValueAsString(lobbyState);
                socketMessageService.broadcastMessage(lobbyStateMessageToBroadcast);
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
             socketMessageService.broadcastMessage(lobbyStateMessageToBroadcast);
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

