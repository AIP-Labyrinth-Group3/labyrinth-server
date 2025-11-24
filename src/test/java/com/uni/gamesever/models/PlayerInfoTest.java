package com.uni.gamesever.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerInfoTest {
    private final String TEST_ID = "P_12345";
    private final String TEST_NAME = "Max Mustermann";
    private PlayerInfo playerInfo;

    @BeforeEach
    void setUp() {
        // Für Tests, die eine initialisierte Instanz benötigen
        playerInfo = new PlayerInfo(TEST_ID);
        playerInfo.setName(TEST_NAME);
    }

    @Nested
    @DisplayName("PlayerInfo Tests")
    public class PlayerInfo_test {
        @Test
        void PlayerInfo_shouldInitializeNullsAndFalse() {
            // WHEN
            PlayerInfo p = new PlayerInfo();

            // THEN
            assertNull(p.getId(), "ID sollte null sein.");
            assertNull(p.getName(), "Name sollte null sein.");
            assertFalse(p.getIsAdmin(), "isAdmin sollte standardmäßig auf false gesetzt sein.");
        }

        @Test
        void PlayerInfo_shouldInitializeFields() {
            // WHEN
            PlayerInfo p = new PlayerInfo(TEST_ID);
            p.setName(TEST_NAME);

            // THEN
            assertEquals(TEST_ID, p.getId(), "Der ID sollte korrekt gesetzt sein.");
            assertEquals(TEST_NAME, p.getName(), "Der Name sollte korrekt gesetzt sein.");
            assertFalse(p.getIsAdmin(), "isAdmin sollte standardmäßig auf false gesetzt sein.");
        }
    }

    @Nested
    @DisplayName("getter Tests")
    public class getter_test {
        @Test
        void getId_shouldReturnCorrectId() {
            // WHEN
            String result = playerInfo.getId();

            // THEN
            assertEquals(TEST_ID, result, "getId sollte die initialisierte ID zurückgeben.");
        }

        @Test
        void getName_shouldReturnCorrectName() {
            // WHEN
            String result = playerInfo.getName();

            // THEN
            assertEquals(TEST_NAME, result, "getName sollte den initialisierten Namen zurückgeben.");
        }

        @Test
        void isAdmin_shouldReturnFalseByDefault() {
            // WHEN
            boolean result = playerInfo.getIsAdmin();

            // THEN
            assertFalse(result, "isAdmin sollte nach Initialisierung false sein.");
        }
    }

    @Nested
    @DisplayName("setter Tests")
    public class setter_test {

        @Test
        void setName_shouldUpdateName() {
            // GIVEN
            String newName = "ErikaMustermann";

            // WHEN
            playerInfo.setName(newName);

            // THEN
            assertEquals(newName, playerInfo.getName(), "setName sollte den Namen erfolgreich aktualisieren.");
        }

        @Test
        void setAdmin_shouldSetTrue() {
            // GIVEN
            assertFalse(playerInfo.getIsAdmin());

            // WHEN
            playerInfo.setAdmin(true);

            // THEN
            assertTrue(playerInfo.getIsAdmin(), "setAdmin(true) sollte isAdmin auf true setzen.");
        }

        @Test
        void setAdmin_shouldSetFalse() {
            // GIVEN
            playerInfo.setAdmin(true);
            assertTrue(playerInfo.getIsAdmin());

            // WHEN
            playerInfo.setAdmin(false);

            // THEN
            assertFalse(playerInfo.getIsAdmin(), "setAdmin(false) sollte isAdmin auf false setzen.");
        }
    }
}