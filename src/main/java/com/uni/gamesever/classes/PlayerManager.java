package com.uni.gamesever.classes;

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

    public int getAmountOfPlayers() {
        int count = 0;
        for (PlayerInfo player : players) {
            if (player != null) {
                count++;
            }
        }
        return count;
    }

    public boolean addPlayer(PlayerInfo newPlayer) {
        for (int i = 0; i < MAX_PLAYERS; i++) {
            if (players[i] == null) {
                players[i] = newPlayer;
                if(i == 0 && !hasAdministrator){ //first player is admin
                    newPlayer.setAdmin(true);
                    hasAdministrator = true;
                }
                return true;
            }
        }
        return false; // Game is full
    }

    public boolean removePlayer(String username) {
        for (int i = 0; i < MAX_PLAYERS; i++) {
            if (players[i] != null && players[i].getName().equals(username)) {
                if(players[i].isAdmin()){ //if admin leaves, assign new admin
                    for (int j = 0; j < MAX_PLAYERS; j++) {
                        if (players[j] != null && j != i) {
                            players[j].setAdmin(true);
                            hasAdministrator = true;
                            break;
                        }
                    }
                }
                players[i] = null;
                playerStates[i] = null;
                if(this.getAmountOfPlayers() == 0){
                    hasAdministrator = false;
                }
                return true;
            }
        }
        return false; // Player not found
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

        //koordinaten der ecken festlegen
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
