package com.uni.gamesever.classes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.uni.gamesever.models.Coordinates;
import com.uni.gamesever.models.GameBoard;
import com.uni.gamesever.models.PlayerInfo;
import com.uni.gamesever.models.PlayerState;
import com.uni.gamesever.models.Treasure;

@Service
public class PlayerManager {
    private static final int MAX_PLAYERS = 4;
    private PlayerInfo[] players = new PlayerInfo[MAX_PLAYERS];
    private PlayerState[] playerStates = new PlayerState[MAX_PLAYERS];
    private boolean hasAdministrator = false;
    private List<String> currentAvailableColors = new ArrayList<>(Arrays.asList("RED", "BLUE", "GREEN", "YELLOW"));

    public int getAmountOfPlayers() {
        int count = 0;
        for (PlayerInfo player : players) {
            if (player != null) {
                count++;
            }
        }
        return count;
    }

    public String getAdminID() {
        for (PlayerInfo player : players) {
            if (player != null && player.getIsAdmin()) {
                return player.getId();
            }
        }
        return null;
    }

    public boolean addPlayer(PlayerInfo newPlayer) {
        if(newPlayer == null){
            return false;
        }
        for (int i = 0; i < MAX_PLAYERS; i++) {
            if (players[i] == null) {
                players[i] = newPlayer;
                if(i == 0 && !hasAdministrator){
                    newPlayer.setAdmin(true);
                    hasAdministrator = true;
                }
                newPlayer.setColor(currentAvailableColors.remove(0));
                newPlayer.setReady(true);
                return true;
            }
        }
        return false;
    }

    public boolean removePlayer(String username) {
        if(username == null){
            return false;
        }
        for (int i = 0; i < MAX_PLAYERS; i++) {
            if (players[i] != null && players[i].getName().equals(username)) {
                if(players[i].getIsAdmin()){
                    for (int j = 0; j < MAX_PLAYERS; j++) {
                        if (players[j] != null && j != i) {
                            players[j].setAdmin(true);
                            hasAdministrator = true;
                            break;
                        }
                    }
                }
                currentAvailableColors.add(players[i].getColor());
                players[i] = null;
                playerStates[i] = null;
                if(this.getAmountOfPlayers() == 0){
                    hasAdministrator = false;
                }
                
                return true;
            }
        }
        return false;
    }

    public PlayerInfo[] getPlayers() {
        return players;
    }

    public PlayerState[] getPlayerStates() {
        return playerStates;
    }


    public void initializePlayerStates(GameBoard board) {
        int rows = board.getSize().getRows();
        int cols = board.getSize().getCols();

        Coordinates[] startingPositions = new Coordinates[] {
            new Coordinates(0, 0),
            new Coordinates(0, cols - 1),
            new Coordinates(rows - 1, 0),
            new Coordinates(rows - 1, cols - 1)
        };

        for (int i=0; i< players.length; i++){
            if(players[i] != null){
                playerStates[i] = new PlayerState(
                    players[i],
                    startingPositions[i],
                    new Treasure[0], 
                    0,
                    new String[0]
                );
            }
        }
    }
}
