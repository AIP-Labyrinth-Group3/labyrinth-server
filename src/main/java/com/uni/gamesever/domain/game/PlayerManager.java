package com.uni.gamesever.domain.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.uni.gamesever.domain.enums.Color;
import com.uni.gamesever.domain.exceptions.UserNotFoundException;
import com.uni.gamesever.domain.exceptions.UsernameAlreadyTakenException;
import com.uni.gamesever.domain.model.Coordinates;
import com.uni.gamesever.domain.model.GameBoard;
import com.uni.gamesever.domain.model.PlayerInfo;
import com.uni.gamesever.domain.model.PlayerState;
import com.uni.gamesever.services.SocketMessageService;

@Service
public class PlayerManager {
    private static final int MAX_PLAYERS = 4;
    private PlayerInfo[] players = new PlayerInfo[MAX_PLAYERS];
    private PlayerState[] playerStates = new PlayerState[MAX_PLAYERS];
    private boolean hasAdministrator = false;
    private List<Color> currentAvailableColors = new ArrayList<>(
            Arrays.asList(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW));
    private PlayerInfo currentPlayer = null;
    private final SocketMessageService socketMessageService;

    public PlayerManager(SocketMessageService socketMessageService) {
        this.socketMessageService = socketMessageService;
    }

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

    public boolean addPlayer(PlayerInfo newPlayer) throws UsernameAlreadyTakenException {
        if (newPlayer == null) {
            return false;
        }
        for (PlayerInfo player : players) {
            if (player != null && player.getName().equals(newPlayer.getName())) {
                throw new UsernameAlreadyTakenException("Der Benutzername ist bereits vergeben.");
            }
        }
        for (int i = 0; i < MAX_PLAYERS; i++) {
            if (players[i] == null) {
                players[i] = newPlayer;
                if (i == 0 && !hasAdministrator) {
                    newPlayer.setAdmin(true);
                    hasAdministrator = true;
                }
                newPlayer.setColor(currentAvailableColors.remove(0));
                return true;
            }
        }
        return false;
    }

    public boolean disconnectPlayer(String playerId) {
        if (playerId == null) {
            return false;
        }

        PlayerInfo player = getPlayerById(playerId);
        if (player == null) {
            return false;
        }
        player.setIsConnected(false);
        return true;
    }

    public boolean removePlayer(String userID) throws UserNotFoundException {
        if (userID == null || userID.isEmpty()) {
            return false;
        }
        boolean playerFound = false;
        hasAdministrator = false;
        for (int i = 0; i < MAX_PLAYERS; i++) {
            if (players[i] != null && players[i].getId().equals(userID)) {
                playerFound = true;
                if (players[i].getIsAdmin()) {
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
                if (this.getAmountOfPlayers() == 0) {
                    hasAdministrator = false;
                }
            }
        }

        if (!playerFound) {
            throw new UserNotFoundException("Benutzer mit der angegebenen ID wurde nicht gefunden.");
        }

        PlayerInfo[] newPlayers = new PlayerInfo[MAX_PLAYERS];
        PlayerState[] newPlayerStates = new PlayerState[MAX_PLAYERS];
        int index = 0;
        for (int i = 0; i < MAX_PLAYERS; i++) {
            if (players[i] != null) {
                newPlayers[index] = players[i];
                newPlayerStates[index] = playerStates[i];
                index++;
            }
        }
        this.players = newPlayers;
        this.playerStates = newPlayerStates;

        socketMessageService.removeDisconnectedSessionWithID(userID);

        return true;
    }

    public boolean reconnectPlayer(String identifierToken, String newSessionId) throws UserNotFoundException {
        for (PlayerInfo player : players) {
            // Suche nach identifierToken (bleibt fix) statt nach ID (ändert sich)
            if (player != null && player.getIdentifierToken() != null
                && player.getIdentifierToken().equals(identifierToken)) {
                player.setIsConnected(true);
                player.setId(newSessionId); // Update Session ID für Message-Routing
                System.out.println("📝 Reconnect: identifierToken=" + identifierToken + " → neue SessionID=" + newSessionId);
                return true;
            }
        }
        throw new UserNotFoundException("Es wurde kein Benutzer zum Verbinden gefunden!");
    }

    public PlayerInfo[] getPlayers() {
        return players.clone();
    }

    public PlayerInfo[] getNonNullPlayers() {
        return Arrays.stream(players)
                .filter(player -> player != null)
                .toArray(PlayerInfo[]::new);
    }

    public PlayerState[] getPlayerStates() {
        return playerStates;
    }

    public PlayerState getPlayerStateById(String playerId) {
        for (PlayerState state : playerStates) {
            if (state != null && state.getPlayerInfo().getId().equals(playerId)) {
                return state;
            }
        }
        return null;
    }

    public PlayerInfo getPlayerById(String playerId) {
        for (PlayerInfo player : players) {
            if (player != null && player.getId().equals(playerId)) {
                return player;
            }
        }
        return null;
    }

    public PlayerInfo getPlayerByIdentifierToken(String identifierToken) {
        for (PlayerInfo player : players) {
            if (player != null && player.getIdentifierToken() != null
                && player.getIdentifierToken().equals(identifierToken)) {
                return player;
            }
        }
        return null;
    }

    public PlayerState getPlayerStateByIdentifierToken(String identifierToken) {
        for (PlayerState state : playerStates) {
            if (state != null && state.getPlayerInfo() != null
                && state.getPlayerInfo().getIdentifierToken() != null
                && state.getPlayerInfo().getIdentifierToken().equals(identifierToken)) {
                return state;
            }
        }
        return null;
    }

    public PlayerState[] getNonNullPlayerStates() {
        return Arrays.stream(playerStates)
                .filter(state -> state != null)
                .toArray(PlayerState[]::new);
    }

    public PlayerState[] getPlayerStatesOfPlayersNotOnTurn() {
        List<PlayerState> otherPlayerStates = new ArrayList<>();
        for (PlayerState state : playerStates) {
            if (state != null && !state.getPlayerInfo().getId().equals(this.currentPlayer.getId())) {
                otherPlayerStates.add(state);
            }
        }
        return otherPlayerStates.toArray(new PlayerState[0]);
    }

    public PlayerState getCurrentPlayerState() {
        if (this.currentPlayer == null) {
            return null;
        }
        for (PlayerState state : playerStates) {
            if (state != null && state.getPlayerInfo().getId().equals(this.currentPlayer.getId())) {
                return state;
            }
        }
        return null;
    }

    public void setCurrentPlayer(PlayerInfo player) {
        this.currentPlayer = player;
    }

    public PlayerInfo getCurrentPlayer() {
        return this.currentPlayer;
    }

    public void setNextPlayerAsCurrent() {
        PlayerInfo[] nonNullPlayers = getNonNullPlayers();
        if (nonNullPlayers.length == 0) {
            this.currentPlayer = null;
            return;
        }
        if (this.currentPlayer == null) {
            this.currentPlayer = nonNullPlayers[0];
            return;
        }
        for (int i = 0; i < nonNullPlayers.length; i++) {
            if (nonNullPlayers[i].getId().equals(this.currentPlayer.getId())) {
                this.currentPlayer = nonNullPlayers[(i + 1) % nonNullPlayers.length];
                return;
            }
        }
        this.currentPlayer = nonNullPlayers[0];
    }

    public void initializePlayerStates(GameBoard board) {
        int boardRows = board.getSize().getRows();
        int boardCols = board.getSize().getCols();

        Coordinates[] startingPositions = new Coordinates[] {
                new Coordinates(0, 0),
                new Coordinates(boardCols - 1, 0),
                new Coordinates(boardCols - 1, boardRows - 1),
                new Coordinates(0, boardRows - 1)
        };

        for (int i = 0; i < players.length; i++) {
            if (players[i] != null) {
                Coordinates startPos = startingPositions[i];
                PlayerState state = new PlayerState(
                        players[i],
                        startPos,
                        startPos,
                        null,
                        0);
                playerStates[i] = state;
            }
        }
    }
}
