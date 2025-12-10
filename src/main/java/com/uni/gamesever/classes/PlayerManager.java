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
    private PlayerInfo currentPlayer = null;

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

    public boolean addPlayer(PlayerInfo newPlayer) throws IllegalArgumentException {
        if(newPlayer == null){
            return false;
        }
        for (PlayerInfo player : players) {
            if (player != null && player.getName().equals(newPlayer.getName())) {
                throw new IllegalArgumentException("Username already taken.");
            }
        }
        for (int i = 0; i < MAX_PLAYERS; i++) {
            if (players[i] == null) {
                players[i] = newPlayer;
                if(i == 0 && !hasAdministrator){
                    newPlayer.setAdmin(true);
                    hasAdministrator = true;
                }
                newPlayer.setColor(currentAvailableColors.remove(0));
                return true;
            }
        }
        return false;
    }

    public boolean removePlayer(String userID) {
        if(userID == null){
            return false;
        }
        hasAdministrator = false;
        for (int i = 0; i < MAX_PLAYERS; i++) {
            if (players[i] != null && players[i].getId().equals(userID)) {
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
        return players.clone();
    }

    public PlayerInfo[] getNonNullPlayers(){
        return Arrays.stream(players)
                     .filter(player -> player != null)
                     .toArray(PlayerInfo[]::new);
    }


    public PlayerState[] getPlayerStates() {
        return playerStates.clone();
    }
    public PlayerState[] getNonNullPlayerStates(){
        return Arrays.stream(playerStates)
                     .filter(state -> state != null)
                     .toArray(PlayerState[]::new);
    }

    public PlayerState getCurrentPlayerState(){
        if(this.currentPlayer == null){
            return null;
        }
        for(PlayerState state : playerStates){
            if(state != null && state.getPlayer().getId().equals(this.currentPlayer.getId())){
                return state;
            }
        }
        return null;
    }

    public void setCurrentPlayer(PlayerInfo player){
        this.currentPlayer = player;
    }
    public PlayerInfo getCurrentPlayer(){
        return this.currentPlayer;
    }

    public void setNextPlayerAsCurrent(){
        PlayerInfo[] nonNullPlayers = getNonNullPlayers();
        if(nonNullPlayers.length == 0){
            this.currentPlayer = null;
            return;
        }
        if(this.currentPlayer == null){
            this.currentPlayer = nonNullPlayers[0];
            return;
        }
        for(int i = 0; i < nonNullPlayers.length; i++){
            if(nonNullPlayers[i].getId().equals(this.currentPlayer.getId())){
                this.currentPlayer = nonNullPlayers[(i + 1) % nonNullPlayers.length];
                return;
            }
        }
        this.currentPlayer = nonNullPlayers[0];
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
                Coordinates startPos = startingPositions[i];
                PlayerState state = new PlayerState(
                    players[i],
                    startPos,
                    startPos,
                    new Treasure[0],
                    null,
                    0
                );
                playerStates[i] = state;
            }
        }
    }
}
