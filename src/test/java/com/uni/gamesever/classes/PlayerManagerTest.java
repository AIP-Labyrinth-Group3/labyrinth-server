package com.uni.gamesever.classes;

import com.uni.gamesever.exceptions.UserNotFoundException;
import com.uni.gamesever.exceptions.UsernameAlreadyTakenException;
import com.uni.gamesever.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlayerManagerTest {
    // Die zu testende Klasse
    private PlayerManager playerManager;

    // Gemockte PlayerInfo Objekte zur Simulation von Spielern
    @Mock
    private PlayerInfo mockPlayer1;
    @Mock
    private PlayerInfo mockPlayer2;
    @Mock
    private PlayerInfo mockPlayer3;
    @Mock
    private PlayerInfo mockPlayer4;
    @Mock
    private PlayerInfo mockPlayer5; // Für den Test der Kapazitätsgrenze

    // non Mock Players
    private PlayerInfo player1 = new PlayerInfo("id1");
    private PlayerInfo player2 = new PlayerInfo("id2");
    private PlayerInfo player3 = new PlayerInfo("id3");
    private PlayerInfo player4 = new PlayerInfo("id4");
    private PlayerInfo player5 = new PlayerInfo("id5");
    {
        player1.setName("Player1");
        player2.setName("Player2");
        player3.setName("Player3");
        player4.setName("Player4");
        player5.setName("Player5");
    }

    // Gemocktes GameBoard Objekt für initializePlayerStates
    @Mock
    private GameBoard mockBoard;
    @Mock
    private BoardSize mockSize;

    // Setzt den Manager vor jedem Test in einen sauberen Zustand zurück.
    // Da @InjectMocks eine neue Instanz pro Test erstellt, sind die internen Arrays
    // leer (null-initialisiert).
    @BeforeEach
    void setUp() {
        playerManager = new PlayerManager();
    }

    @Nested
    @DisplayName("getAmountOfPlayers Tests")
    public class getAmountOfPlayers_test {
        @Test
        void getAmountOfPlayers_shouldReturnZeroWhenEmpty() {
            // WHEN
            int amount = playerManager.getAmountOfPlayers();

            // THEN
            assertEquals(0, amount, "Die Anzahl der Spieler sollte 0 sein, wenn die Liste leer ist.");
        }

        @Test
        void getAmountOfPlayers_shouldReturnCorrectCount() throws UsernameAlreadyTakenException {
            // GIVEN
            playerManager.addPlayer(player1);
            playerManager.addPlayer(player2);

            // WHEN
            int amount = playerManager.getAmountOfPlayers();

            // THEN
            assertEquals(2, amount, "Die Anzahl der Spieler sollte 2 sein.");
        }

        @Test
        void getAmountOfPlayers_shouldReturnCorrectCountWhenNewPlayerIsNull() throws UsernameAlreadyTakenException {
            // GIVEN
            playerManager.addPlayer(player1);
            playerManager.addPlayer(null);
            playerManager.addPlayer(player2);
            playerManager.addPlayer(player3);

            // WHEN
            int amount = playerManager.getAmountOfPlayers();

            // THEN
            assertEquals(3, amount, "Die Anzahl der Spieler sollte 3 sein.");
        }
    }

    @Nested
    @DisplayName("addPlayer Tests")
    public class addPlayer_test {
        @Test
        void addPlayer_shouldAddNewPlayerSuccessfully() throws UsernameAlreadyTakenException {
            // WHEN
            boolean result = playerManager.addPlayer(player1);

            // THEN
            assertTrue(result, "Das Hinzufügen des Spielers sollte erfolgreich sein.");
            assertEquals(1, playerManager.getAmountOfPlayers(), "Nach dem Hinzufügen sollte die Anzahl 1 sein.");
        }

        @Test
        void addPlayer_shouldAddPlayersUpToMaxCapacity() throws UsernameAlreadyTakenException {
            // GIVEN
            playerManager.addPlayer(player1);
            playerManager.addPlayer(player2);
            playerManager.addPlayer(player3);

            // WHEN
            boolean result = playerManager.addPlayer(player4);

            // THEN
            assertTrue(result, "Der vierte Spieler sollte erfolgreich hinzugefügt werden.");
            assertEquals(4, playerManager.getAmountOfPlayers(), "Die Anzahl der Spieler sollte 4 sein.");
        }

        @Test
        void addPlayer_shouldFailWhenGameIsFull() throws UsernameAlreadyTakenException {
            // GIVEN
            playerManager.addPlayer(player1);
            playerManager.addPlayer(player2);
            playerManager.addPlayer(player3);
            playerManager.addPlayer(player4);

            // WHEN
            boolean result = playerManager.addPlayer(player5);

            // THEN
            assertFalse(result, "Das Hinzufügen des fünften Spielers sollte fehlschlagen.");
            assertEquals(4, playerManager.getAmountOfPlayers(), "Die Anzahl der Spieler sollte immer noch 4 sein.");
        }

        @Test
        void addPlayer_shouldFailWhenNewPlayerIsNull() throws UsernameAlreadyTakenException {
            // WHEN
            boolean result = playerManager.addPlayer(null);

            // THEN
            assertFalse(result, "Das Hinzufügen von null sollte fehlschlagen.");
            assertEquals(0, playerManager.getAmountOfPlayers(), "Die Anzahl der Spieler sollte immer noch 0 sein.");
        }

        @Test
        void addPlayer_shouldAddNewPlayerAsAdmin() throws UsernameAlreadyTakenException {
            PlayerInfo adminPlayer = new PlayerInfo("testAdmin");
            // WHEN
            playerManager.addPlayer(adminPlayer);

            // THEN
            assertTrue(playerManager.getPlayers()[0].getIsAdmin(),
                    "Nach dem Hinzufügen des ersten Spielers sollte dieser Admin sein.");
        }

        // add a test to check if a player can not gave the same username - exception
        @Test
        void addPlayer_shouldFailWhenUsernameAlreadyExists() throws UsernameAlreadyTakenException {
            // GIVEN
            playerManager.addPlayer(player1);
            PlayerInfo duplicatePlayer = new PlayerInfo("idDuplicate");
            duplicatePlayer.setName(player1.getName()); // Gleicher Username wie player1
            // WHEN & THEN
            Exception exception = assertThrows(UsernameAlreadyTakenException.class, () -> {
                playerManager.addPlayer(duplicatePlayer);
            });
            String expectedMessage = "Username already taken.";
            String actualMessage = exception.getMessage();
            assertEquals(expectedMessage, actualMessage, "Die Exception-Nachricht sollte korrekt sein.");

        }
    }

    @Nested
    @DisplayName("removePlayer Tests")
    public class removePlayer_test {
        @Test
        void removePlayer_shouldRemovePlayerSuccessfully() throws UsernameAlreadyTakenException, UserNotFoundException {
            // GIVEN
            playerManager.addPlayer(player1);

            // WHEN
            boolean result = playerManager.removePlayer(player1.getId());

            // THEN
            assertTrue(result, "Das Entfernen des Spielers sollte erfolgreich sein.");
            assertEquals(0, playerManager.getAmountOfPlayers(), "Nach dem Entfernen sollte die Anzahl 1 sein.");
        }

        @Test
        void removePlayer_shouldThrowAnExceptionWhenRemovingPlayerTwice()
                throws UsernameAlreadyTakenException, UserNotFoundException {
            // GIVEN
            playerManager.addPlayer(player1);
            playerManager.removePlayer(player1.getId());

            assertThrows(UserNotFoundException.class, () -> {
                playerManager.removePlayer(player1.getId());
            }, "Das zweite Entfernen des gleichen Spielers sollte eine UserNotFoundException werfen.");
            assertEquals(0, playerManager.getAmountOfPlayers(),
                    "Nach dem zweimaligen Entfernen sollte die Anzahl 0 sein.");
        }

        @Test
        void removePlayer_shouldFailWhenRemovePlayerWithNull()
                throws UsernameAlreadyTakenException, UserNotFoundException {
            // GIVEN
            playerManager.addPlayer(player1);

            // WHEN
            boolean result = playerManager.removePlayer(null);

            // THEN
            assertFalse(result, "Das Entfernen des Spielers mit Username null sollte fehlschlagen.");
            assertEquals(1, playerManager.getAmountOfPlayers(), "Die Anzahl der Spieler sollte immer noch 1 sein.");
        }

        @Test
        void removePlayer_shouldRemoveAdminSuccessfully() throws UsernameAlreadyTakenException, UserNotFoundException {
            // GIVEN
            PlayerInfo adminPlayer = new PlayerInfo("id1");
            adminPlayer.setName("testAdmin");
            PlayerInfo player2 = new PlayerInfo("id2");
            player2.setName("testPlayer2");
            playerManager.addPlayer(adminPlayer);
            playerManager.addPlayer(player2);

            // WHEN
            boolean result = playerManager.removePlayer(adminPlayer.getId());

            // THEN
            assertTrue(result, "Das Entfernen des Spielers sollte erfolgreich sein.");
            assertEquals(1, playerManager.getAmountOfPlayers(), "Nach dem Entfernen sollte die Anzahl 1 sein.");
            assertTrue(playerManager.getPlayers()[1].getIsAdmin(),
                    "Nach dem Entfernen von Spieler 1 sollte Spieler 2 Admin sein.");
        }
    }

    @Nested
    @DisplayName("getPlayers Tests")
    public class getPlayers_test {
        @Test
        void getPlayers_shouldReturnArrayOfNulls() {
            // WHEN
            PlayerInfo[] players = playerManager.getPlayers();

            // THEN
            assertNotNull(players, "Das zurückgegebene Array sollte nicht null sein.");
            assertEquals(4, players.length, "Das Array sollte die maximale Größe von 4 haben.");
            // Standardmäßig sollte das Array mit null initialisiert sein, bevor
            // initializePlayerStates aufgerufen wird.
            assertTrue(Arrays.stream(players).allMatch(playerInfo -> playerInfo == null),
                    "Alle Spieler sollten anfangs null sein.");
        }

        @Test
        void getPlayers_shouldReturnArrayOfPlayerInfos() throws UsernameAlreadyTakenException {
            // GIVEN
            playerManager.addPlayer(player1);

            // WHEN
            PlayerInfo[] players = playerManager.getPlayers();

            // THEN
            assertNotNull(players, "Das zurückgegebene Array sollte nicht null sein.");
            assertEquals(4, players.length, "Das Array sollte die maximale Größe von 4 haben.");
            assertEquals(player1, players[0], "Der erste Platz sollte den hinzugefügten Spieler enthalten.");
            assertNull(players[1], "Der zweite Platz sollte null sein.");
        }
    }

    @Nested
    @DisplayName("getPlayerStates Tests")
    public class getPlayerStates_test {
        @Test
        void getPlayerStates_shouldReturnArrayOfPlayerStates() {
            // WHEN
            PlayerState[] playerStates = playerManager.getPlayerStates();

            // THEN
            assertNotNull(playerStates, "Das zurückgegebene Array sollte nicht null sein.");
            assertEquals(4, playerStates.length, "Das Array sollte die maximale Größe von 4 haben.");
            // Standardmäßig sollte das Array mit null initialisiert sein, bevor
            // initializePlayerStates aufgerufen wird.
            assertTrue(Arrays.stream(playerStates).allMatch(state -> state == null),
                    "Alle Zustände sollten anfangs null sein.");
        }
    }

    @Nested
    @DisplayName("initializePlayerStates Tests")
    public class initializePlayerStates_test {
        @Test
        void initializePlayerStates_shouldInitializeStatesForExistingPlayers() throws UsernameAlreadyTakenException {
            // GIVEN
            playerManager.addPlayer(player1); // Index 0 (0, 0)
            playerManager.addPlayer(player3); // Index 1 (0, cols-1)

            // Erwartete Koordinaten basierend auf setUp(): Reihen=7, Spalten=7
            Coordinates expectedPos1 = new Coordinates(0, 0); // Oben links
            Coordinates expectedPos2 = new Coordinates(6, 0); // Oben rechts (0, 6)

            when(mockBoard.getSize()).thenReturn(new BoardSize(7, 7));
            when(mockBoard.getRows()).thenReturn(7);
            when(mockBoard.getCols()).thenReturn(7);
            // WHEN
            playerManager.initializePlayerStates(mockBoard);

            // THEN
            PlayerState[] states = playerManager.getPlayerStates();

            // Überprüfung von Spieler 1 (Index 0)
            assertNotNull(states[0], "Der Zustand für Spieler 1 sollte initialisiert sein.");
            assertEquals(player1, states[0].getPlayer(), "Die PlayerInfo des Zustands sollte Spieler 1 sein.");
            assertEquals(expectedPos1.getColumn(), states[0].getCurrentPosition().getColumn(),
                    "Die X Koordinate sollte 0 sein.");
            assertEquals(expectedPos1.getRow(), states[0].getCurrentPosition().getRow(),
                    "Die Y Koordinate sollte 0 sein.");

            // Überprüfung von Spieler 3 (Index 1)
            assertNotNull(states[1], "Der Zustand für Spieler 3 sollte initialisiert sein.");
            assertEquals(player3, states[1].getPlayer(), "Die PlayerInfo des Zustands sollte Spieler 3 sein.");
            assertEquals(expectedPos2.getColumn(), states[1].getCurrentPosition().getColumn(),
                    "Die X Koordinate sollte 0 sein.");
            assertEquals(expectedPos2.getRow(), states[1].getCurrentPosition().getRow(),
                    "Die Y Koordinate sollte 6 sein.");

            // Überprüfung von leeren Plätzen
            assertNull(states[2], "Der leere Platz 2 sollte null bleiben.");
            assertNull(states[3], "Der leere Platz 3 sollte null bleiben.");
        }

        @Test
        void initializePlayerStates_shouldAssignCorrectCornerCoordinates() throws UsernameAlreadyTakenException {
            // GIVEN
            playerManager.addPlayer(player1); // Index 0 (0, 0)
            playerManager.addPlayer(player2); // Index 1 (0, cols-1)
            playerManager.addPlayer(player3); // Index 2 (rows-1, 0)
            playerManager.addPlayer(player4); // Index 3 (rows-1, cols-1)

            // Erwartete Koordinaten basierend auf setUp(): Reihen=7, Spalten=7
            Coordinates expectedPos1 = new Coordinates(0, 0); // Oben links
            Coordinates expectedPos2 = new Coordinates(6, 0); // Oben rechts (0, 6)
            Coordinates expectedPos3 = new Coordinates(6, 6); // Unten rechts (6, 6)
            Coordinates expectedPos4 = new Coordinates(0, 6); // Unten links (0, 6)

            when(mockBoard.getSize()).thenReturn(new BoardSize(7, 7));
            when(mockBoard.getRows()).thenReturn(7);
            when(mockBoard.getCols()).thenReturn(7);

            // WHEN
            playerManager.initializePlayerStates(mockBoard);

            // THEN
            PlayerState[] states = playerManager.getNonNullPlayerStates();

            // Überprüfung der erwarteten Startpositionen
            assertEquals(expectedPos1.getColumn(), states[0].getCurrentPosition().getColumn());
            assertEquals(expectedPos1.getRow(), states[0].getCurrentPosition().getRow());

            assertEquals(expectedPos2.getColumn(), states[1].getCurrentPosition().getColumn());
            assertEquals(expectedPos2.getRow(), states[1].getCurrentPosition().getRow());

            assertEquals(expectedPos3.getColumn(), states[2].getCurrentPosition().getColumn());
            assertEquals(expectedPos3.getRow(), states[2].getCurrentPosition().getRow());

            assertEquals(expectedPos4.getColumn(), states[3].getCurrentPosition().getColumn());
            assertEquals(expectedPos4.getRow(), states[3].getCurrentPosition().getRow());
        }

        @Test
        void initializePlayerStates_shouldInitializeEmptyArraysAndPoints() throws UsernameAlreadyTakenException {
            playerManager.addPlayer(mockPlayer1);

            when(mockBoard.getSize()).thenReturn(new BoardSize(7, 7));
            when(mockBoard.getRows()).thenReturn(7);
            when(mockBoard.getCols()).thenReturn(7);

            playerManager.initializePlayerStates(mockBoard);

            // THEN
            PlayerState state = playerManager.getNonNullPlayerStates()[0];
            assertNotNull(state.getTreasuresFound(), "Die gesammelten Schätze sollten nicht null sein.");
            assertEquals(0, state.getTreasuresFound().size(), "Die gesammelten Schätze sollten leer sein.");

            assertNotNull(state.getAchievements(), "Die Achievements sollten nicht null sein.");
            assertEquals(0, state.getAchievements().length, "Die Achievements sollten leer sein.");

        }
    }
}