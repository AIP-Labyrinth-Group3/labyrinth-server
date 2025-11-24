package com.uni.gamesever.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SocketBroadcastServiceTest {
    private final String TEST_MESSAGE = "Test Message";

    // Die zu testende Klasse
    @InjectMocks
    private SocketMessageService service;

    // Hilfsmethode zur Erstellung einer gemockten Session
    private WebSocketSession mockSession(boolean isOpen) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(isOpen);
        return session;
    }

    @BeforeEach
    void setUp() {
        // Der Service wird durch @InjectMocks vor jedem Test neu instanziiert,
        // was die interne webSocketSessions Liste automatisch leert.
    }

    @Nested
    @DisplayName("addIncomingSession Tests")
    class addIncomingSession_test {
        @Test
        void addIncomingSession_shouldAddSession() throws IOException {
            // GIVEN
            WebSocketSession session = mockSession(true);

            // WHEN
            service.addIncomingSession(session);
            service.broadcastMessage(TEST_MESSAGE);

            // THEN
            // Indirekte Überprüfung: Die Session sollte die Nachricht empfangen haben
            verify(session, times(1)).sendMessage(eq(new TextMessage(TEST_MESSAGE)));
        }

        @Test
        void addIncomingSession_shouldAddMultipleSessions() throws IOException {
            // GIVEN
            WebSocketSession session1 = mockSession(true);
            WebSocketSession session2 = mockSession(true);

            // WHEN
            service.addIncomingSession(session1);
            service.addIncomingSession(session2);
            service.broadcastMessage(TEST_MESSAGE);

            // THEN
            verify(session1, times(1)).sendMessage(eq(new TextMessage(TEST_MESSAGE)));
            verify(session2, times(1)).sendMessage(eq(new TextMessage(TEST_MESSAGE)));
        }
    }

    @Nested
    @DisplayName("removeDisconnectedSession Tests")
    class removeDisconnectedSession_test {
        @Test
        void removeDisconnectedSession_shouldRemoveExistingSession() throws IOException {
            // GIVEN
            WebSocketSession sessionToRemove = mockSession(true);
            WebSocketSession sessionToKeep = mockSession(true);

            service.addIncomingSession(sessionToRemove);
            service.addIncomingSession(sessionToKeep);

            // WHEN
            service.removeDisconnectedSession(sessionToRemove);
            service.broadcastMessage(TEST_MESSAGE);

            // THEN
            // Die entfernte Session sollte KEINE Nachricht empfangen
            verify(sessionToRemove, never()).sendMessage(eq(new TextMessage(TEST_MESSAGE)));
            // Die behaltene Session sollte die Nachricht empfangen
            verify(sessionToKeep, times(1)).sendMessage(eq(new TextMessage(TEST_MESSAGE)));
        }

        @Test
        void removeDisconnectedSession_shouldHandleNonExistingSession() {
            // GIVEN
            WebSocketSession existingSession = mockSession(true);
            WebSocketSession nonExistingSession = mockSession(true);

            service.addIncomingSession(existingSession);

            // WHEN / THEN
            assertDoesNotThrow(() -> service.removeDisconnectedSession(nonExistingSession), "Das Entfernen einer nicht existierenden Session sollte keine Exception auslösen.");
        }
    }

    @Nested
    @DisplayName("broadcastMessage Tests")
    class broadcastMessage_test {
        @Test
        void broadcastMessage_shouldSendToAllOpenSessions() throws IOException {
            // GIVEN
            WebSocketSession session1 = mockSession(true);
            WebSocketSession session2 = mockSession(true);

            service.addIncomingSession(session1);
            service.addIncomingSession(session2);

            // WHEN
            service.broadcastMessage(TEST_MESSAGE);

            // THEN
            verify(session1, times(1)).sendMessage(eq(new TextMessage(TEST_MESSAGE)));
            verify(session2, times(1)).sendMessage(eq(new TextMessage(TEST_MESSAGE)));
        }

        @Test
        void broadcastMessage_shouldSkipClosedSessions() throws IOException {
            // GIVEN
            WebSocketSession openSession = mockSession(true);
            WebSocketSession closedSession = mockSession(false);

            service.addIncomingSession(openSession);
            service.addIncomingSession(closedSession);

            // WHEN
            service.broadcastMessage(TEST_MESSAGE);

            // THEN
            // Offene Session erhält Nachricht
            verify(openSession, times(1)).sendMessage(eq(new TextMessage(TEST_MESSAGE)));
            // Geschlossene Session erhält KEINE Nachricht
            verify(closedSession, never()).sendMessage(eq(new TextMessage(TEST_MESSAGE)));
        }

        @Test
        void broadcastMessage_shouldSkipBroadcastingWhenTrowException() throws IOException {
            // GIVEN
            WebSocketSession throwingSession = mockSession(true);
            WebSocketSession goodSession = mockSession(true);

            // Simuliere, dass diese Session beim Senden eine IOException wirft
            doThrow(new IOException("Simulierter Sendefehler")).when(throwingSession).sendMessage(eq(new TextMessage(TEST_MESSAGE)));

            service.addIncomingSession(throwingSession);
            service.addIncomingSession(goodSession);

            // WHEN / THEN
            // Die Methode sollte KEINE Exception nach außen werfen (dank try-catch)
            assertDoesNotThrow(() -> service.broadcastMessage(TEST_MESSAGE));
            // Die gute Session muss ihre Nachricht dennoch erhalten
            verify(goodSession, times(1)).sendMessage(eq(new TextMessage(TEST_MESSAGE)));
            // Die fehlerhafte Session wurde versucht aufzurufen
            verify(throwingSession, times(1)).sendMessage(eq(new TextMessage(TEST_MESSAGE)));
        }
    }
}