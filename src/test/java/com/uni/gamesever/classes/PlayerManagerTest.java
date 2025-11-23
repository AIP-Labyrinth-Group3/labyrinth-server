package com.uni.gamesever.classes;

import com.uni.gamesever.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
    @InjectMocks
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

    // Gemocktes GameBoard Objekt für initializePlayerStates
    @Mock
    private GameBoard mockBoard;
    @Mock
    private BoardSize mockSize;

    // Setzt den Manager vor jedem Test in einen sauberen Zustand zurück.
    // Da @InjectMocks eine neue Instanz pro Test erstellt, sind die internen Arrays leer (null-initialisiert).
    @BeforeEach
    void setUp() {
        // Mock-Board-Setup für initializePlayerStates
        when(mockBoard.getSize()).thenReturn(mockSize);
        when(mockSize.getRows()).thenReturn(7); // z.B. 7 Reihen
        when(mockSize.getCols()).thenReturn(9); // z.B. 9 Spalten

        // Username für removePlayer
        when(mockPlayer1.getName()).thenReturn("TestPlayer1");
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
        void getAmountOfPlayers_shouldReturnCorrectCount() {
            // GIVEN
            playerManager.addPlayer(mockPlayer1);
            playerManager.addPlayer(mockPlayer2);

            // WHEN
            int amount = playerManager.getAmountOfPlayers();

            // THEN
            assertEquals(2, amount, "Die Anzahl der Spieler sollte 2 sein.");
        }

        @Test
        void getAmountOfPlayers_shouldReturnCorrectCountWhenNewPlayerIsNull() {
            // GIVEN
            playerManager.addPlayer(mockPlayer1);
            playerManager.addPlayer(null);
            playerManager.addPlayer(mockPlayer2);
            playerManager.addPlayer(mockPlayer3);

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
        void addPlayer_shouldAddNewPlayerSuccessfully() {
            // WHEN
            boolean result = playerManager.addPlayer(mockPlayer1);

            // THEN
            assertTrue(result, "Das Hinzufügen des Spielers sollte erfolgreich sein.");
            assertEquals(1, playerManager.getAmountOfPlayers(), "Nach dem Hinzufügen sollte die Anzahl 1 sein.");
        }

        @Test
        void addPlayer_shouldAddPlayersUpToMaxCapacity() {
            // GIVEN
            playerManager.addPlayer(mockPlayer1);
            playerManager.addPlayer(mockPlayer2);
            playerManager.addPlayer(mockPlayer3);

            // WHEN
            boolean result = playerManager.addPlayer(mockPlayer4);

            // THEN
            assertTrue(result, "Der vierte Spieler sollte erfolgreich hinzugefügt werden.");
            assertEquals(4, playerManager.getAmountOfPlayers(), "Die Anzahl der Spieler sollte 4 sein.");
        }

        @Test
        void addPlayer_shouldFailWhenGameIsFull() {
            // GIVEN
            playerManager.addPlayer(mockPlayer1);
            playerManager.addPlayer(mockPlayer2);
            playerManager.addPlayer(mockPlayer3);
            playerManager.addPlayer(mockPlayer4);

            // WHEN
            boolean result = playerManager.addPlayer(mockPlayer5);

            // THEN
            assertFalse(result, "Das Hinzufügen des fünften Spielers sollte fehlschlagen.");
            assertEquals(4, playerManager.getAmountOfPlayers(), "Die Anzahl der Spieler sollte immer noch 4 sein.");
        }

        @Test
        void addPlayer_shouldFailWhenNewPlayerIsNull() {
            // WHEN
            boolean result = playerManager.addPlayer(null);

            // THEN
            assertFalse(result, "Das Hinzufügen von null sollte fehlschlagen.");
            assertEquals(0, playerManager.getAmountOfPlayers(), "Die Anzahl der Spieler sollte immer noch 0 sein.");
        }

        @Test
        void addPlayer_shouldAddNewPlayerAsAdmin() {
            // WHEN
            playerManager.addPlayer(mockPlayer1);

            // THEN
            assertTrue(playerManager.getPlayers()[0].isAdmin(), "Nach dem Hinzufügen des ersten Spielers sollte dieser Admin sein.");
        }
    }

    @Nested
    @DisplayName("removePlayer Tests")
    public class removePlayer_test {
        @Test
        void removePlayer_shouldRemovePlayerSuccessfully() {
            // GIVEN
            playerManager.addPlayer(mockPlayer1);

            // WHEN
            boolean result = playerManager.removePlayer(mockPlayer1.getName());

            // THEN
            assertTrue(result, "Das Entfernen des Spielers sollte erfolgreich sein.");
            assertEquals(0, playerManager.getAmountOfPlayers(), "Nach dem Entfernen sollte die Anzahl 1 sein.");
        }

        @Test
        void removePlayer_shouldFailWhenRemovePlayerTwice() {
            // GIVEN
            playerManager.addPlayer(mockPlayer1);
            playerManager.removePlayer(mockPlayer1.getName());

            // WHEN
            boolean result = playerManager.removePlayer(mockPlayer1.getName());

            // THEN
            assertFalse(result, "Das zweimalige Entfernen des Spielers sollte fehlschlagen.");
            assertEquals(0, playerManager.getAmountOfPlayers(), "Nach dem zweimaligen Entfernen sollte die Anzahl 1 sein.");
        }

        @Test
        void removePlayer_shouldFailWhenRemovePlayerWithNull() {
            // GIVEN
            playerManager.addPlayer(mockPlayer1);

            // WHEN
            boolean result = playerManager.removePlayer(null);

            // THEN
            assertFalse(result, "Das Entfernen des Spielers mit Username null sollte fehlschlagen.");
            assertEquals(1, playerManager.getAmountOfPlayers(), "Die Anzahl der Spieler sollte immer noch 1 sein.");
        }

        @Test
        void removePlayer_shouldRemoveAdminSuccessfully() {
            // GIVEN
            playerManager.addPlayer(mockPlayer1);
            playerManager.addPlayer(mockPlayer2);

            // WHEN
            boolean result = playerManager.removePlayer(mockPlayer1.getName());

            // THEN
            assertTrue(result, "Das Entfernen des Spielers sollte erfolgreich sein.");
            assertEquals(1, playerManager.getAmountOfPlayers(), "Nach dem Entfernen sollte die Anzahl 1 sein.");
            assertTrue(playerManager.getPlayers()[1].isAdmin(), "Nach dem Entfernen von Spieler 1 sollte Spieler 2 Admin sein.");
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
            // Standardmäßig sollte das Array mit null initialisiert sein, bevor initializePlayerStates aufgerufen wird.
            assertTrue(Arrays.stream(players).allMatch(playerInfo -> playerInfo == null), "Alle Spieler sollten anfangs null sein.");
        }

        @Test
        void getPlayers_shouldReturnArrayOfPlayerInfos() {
            // GIVEN
            playerManager.addPlayer(mockPlayer1);

            // WHEN
            PlayerInfo[] players = playerManager.getPlayers();

            // THEN
            assertNotNull(players, "Das zurückgegebene Array sollte nicht null sein.");
            assertEquals(4, players.length, "Das Array sollte die maximale Größe von 4 haben.");
            assertEquals(mockPlayer1, players[0], "Der erste Platz sollte den hinzugefügten Spieler enthalten.");
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
            // Standardmäßig sollte das Array mit null initialisiert sein, bevor initializePlayerStates aufgerufen wird.
            assertTrue(Arrays.stream(playerStates).allMatch(state -> state == null), "Alle Zustände sollten anfangs null sein.");
        }
    }

    @Nested
    @DisplayName("initializePlayerStates Tests")
    public class initializePlayerStates_test {
        @Test
        void initializePlayerStates_shouldInitializeStatesForExistingPlayers() {
            // GIVEN
            playerManager.addPlayer(mockPlayer1); // Index 0 (0, 0)
            playerManager.addPlayer(mockPlayer3); // Index 1 (0, cols-1)

            // Erwartete Koordinaten basierend auf setUp(): Reihen=7, Spalten=9
            Coordinates expectedPos1 = new Coordinates(0, 0); // Oben links
            Coordinates expectedPos2 = new Coordinates(0, 9 - 1); // Oben rechts (0, 8)

            // WHEN
            playerManager.initializePlayerStates(mockBoard);

            // THEN
            PlayerState[] states = playerManager.getPlayerStates();

            // Überprüfung von Spieler 1 (Index 0)
            assertNotNull(states[0], "Der Zustand für Spieler 1 sollte initialisiert sein.");
            assertEquals(mockPlayer1, states[0].getPlayer(), "Die PlayerInfo des Zustands sollte Spieler 1 sein.");
            assertEquals(expectedPos1.getX(), states[0].getPosition().getX(), "Die X Koordinate sollte 0 sein.");
            assertEquals(expectedPos1.getY(), states[0].getPosition().getY(), "Die Y Koordinate sollte 0 sein.");

            // Überprüfung von Spieler 3 (Index 1)
            assertNotNull(states[1], "Der Zustand für Spieler 3 sollte initialisiert sein.");
            assertEquals(mockPlayer3, states[1].getPlayer(), "Die PlayerInfo des Zustands sollte Spieler 3 sein.");
            assertEquals(expectedPos2.getX(), states[1].getPosition().getX(), "Die X Koordinate sollte 0 sein.");
            assertEquals(expectedPos2.getY(), states[1].getPosition().getY(), "Die Y Koordinate sollte 8 sein.");

            // Überprüfung von leeren Plätzen
            assertNull(states[2], "Der leere Platz 2 sollte null bleiben.");
            assertNull(states[3], "Der leere Platz 3 sollte null bleiben.");
        }

        @Test
        void initializePlayerStates_shouldAssignCorrectCornerCoordinates() {
            // GIVEN
            playerManager.addPlayer(mockPlayer1); // Index 0 (0, 0)
            playerManager.addPlayer(mockPlayer2); // Index 1 (0, cols-1)
            playerManager.addPlayer(mockPlayer3); // Index 2 (rows-1, 0)
            playerManager.addPlayer(mockPlayer4); // Index 3 (rows-1, cols-1)

            // Erwartete Koordinaten basierend auf setUp(): Reihen=7, Spalten=9
            Coordinates expectedPos1 = new Coordinates(0, 0); // Oben links
            Coordinates expectedPos2 = new Coordinates(0, 9 - 1); // Oben rechts (0, 8)
            Coordinates expectedPos3 = new Coordinates(7 - 1, 0); // Unten links (6, 0)
            Coordinates expectedPos4 = new Coordinates(7 - 1, 9 - 1); // Unten rechts (6, 8)

            // WHEN
            playerManager.initializePlayerStates(mockBoard);

            // THEN
            PlayerState[] states = playerManager.getPlayerStates();

            // Überprüfung der erwarteten Startpositionen
            assertEquals(expectedPos1.getX(), states[0].getPosition().getX());
            assertEquals(expectedPos1.getY(), states[0].getPosition().getY());

            assertEquals(expectedPos2.getX(), states[1].getPosition().getX());
            assertEquals(expectedPos2.getY(), states[1].getPosition().getY());

            assertEquals(expectedPos3.getX(), states[2].getPosition().getX());
            assertEquals(expectedPos3.getY(), states[2].getPosition().getY());

            assertEquals(expectedPos4.getX(), states[3].getPosition().getX());
            assertEquals(expectedPos4.getY(), states[3].getPosition().getY());
        }

        @Test
        void initializePlayerStates_shouldInitializeEmptyArraysAndPoints() {
            // GIVEN
            playerManager.addPlayer(mockPlayer1);

            // WHEN
            playerManager.initializePlayerStates(mockBoard);

            // THEN
            PlayerState state = playerManager.getPlayerStates()[0];
            assertNotNull(state.getTreasuresFound(), "Die gesammelten Schätze sollten nicht null sein.");
            assertEquals(0, state.getTreasuresFound().length, "Die gesammelten Schätze sollten leer sein.");

            assertNotNull(state.getAchievements(), "Die Achievements sollten nicht null sein.");
            assertEquals(0, state.getAchievements().length, "Die Achievements sollten leer sein.");

            assertEquals(0, state.getPoints(), "Die Punkte sollten 0 sein.");
        }
    }
}