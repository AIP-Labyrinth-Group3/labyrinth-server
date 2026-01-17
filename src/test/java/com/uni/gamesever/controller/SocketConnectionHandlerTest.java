package com.uni.gamesever.controller;

import com.uni.gamesever.interfaces.Websocket.SocketConnectionHandler;
import com.uni.gamesever.services.SocketMessageService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.uni.gamesever.domain.game.GameManager;
import com.uni.gamesever.domain.model.TurnInfo;
import com.uni.gamesever.interfaces.Websocket.MessageHandler;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SocketConnectionHandlerTest {
    private final PrintStream STANDARD_OUT = System.out;
    private final ByteArrayOutputStream OUTPUT_STREAM = new ByteArrayOutputStream();
    private final String SESSION_ID = "SESSION_XYZ";

    // Die zu testende Klasse
    @InjectMocks
    private SocketConnectionHandler socketConnectionHandler;

    // Abhängigkeiten, die gemockt werden
    @Mock
    private SocketMessageService socketMessageService;
    @Mock
    private MessageHandler messageHandler;
    @Mock
    private WebSocketSession mockSession;

    @Mock
    private GameManager gameManager;

    @Mock
    private TurnInfo turnInfo;

    @BeforeEach
    void setUp() {
        // Allgemeine Mock-Setup für die Session (z.B. die ID, falls benötigt)
        when(mockSession.getId()).thenReturn(SESSION_ID);

        // System.out Umleitung
        System.setOut(new PrintStream(OUTPUT_STREAM));
    }

    @AfterEach
    void tearDown() {
        // Stellt den ursprünglichen System.out Stream wieder her
        System.setOut(STANDARD_OUT);
    }

    @Nested
    @DisplayName("afterConnectionEstablished Tests")
    class afterConnectionEstablished_test {
        @Test
        void afterConnectionEstablished_shouldAddSessionAndPrint() throws Exception {
            // WHEN
            socketConnectionHandler.afterConnectionEstablished(mockSession);

            // THEN
            verify(socketMessageService, times(1)).addIncomingSession(eq(mockSession));
        }

        @Test
        void afterConnectionEstablished_shouldThrowException() {
            // GIVEN
            doThrow(new RuntimeException("Simulated Service Error")).when(socketMessageService)
                    .addIncomingSession(any());

            // WHEN / THEN
            assertThrows(RuntimeException.class, () -> socketConnectionHandler.afterConnectionEstablished(mockSession));
            assertEquals("", OUTPUT_STREAM.toString(),
                    "Die Konsolenausgabe sollte leer sein, da die Exception davor geworfen werden sollte.");
        }
    }

    @Nested
    @DisplayName("afterConnectionClosed Tests")
    class afterConnectionClosed_test {
        @Mock
        private CloseStatus mockStatus;

        @Test
        void afterConnectionClosed_shouldThrowException() {
            // GIVEN
            doThrow(new RuntimeException("Simulated Removal Error")).when(socketMessageService)
                    .removeDisconnectedSession(any());

            // WHEN / THEN
            assertThrows(RuntimeException.class,
                    () -> socketConnectionHandler.afterConnectionClosed(mockSession, mockStatus));
            assertEquals("", OUTPUT_STREAM.toString(),
                    "Die Konsolenausgabe sollte leer sein, da die Exception davor geworfen werden sollte.");
        }
    }

    @Nested
    @DisplayName("handleMessage Tests")
    class handleMessage_test {
        private final String RAW_PAYLOAD = "{\"action\":\"move\"}";
        @Mock
        private TextMessage mockTextMessage;

        @BeforeEach
        void setupMessage() {
            when(mockTextMessage.getPayload()).thenReturn(RAW_PAYLOAD);
        }

        @Test
        void handleMessage_shouldSuccessAndPrint() throws Exception {
            // GIVEN
            String expectedLog = "Message Received from user " + SESSION_ID + ": " + RAW_PAYLOAD
                    + System.lineSeparator();

            // WHEN
            socketConnectionHandler.handleMessage(mockSession, mockTextMessage);

            // THEN
            verify(messageHandler, times(1)).handleClientMessage(eq(RAW_PAYLOAD), eq(SESSION_ID));
        }

        @Test
        void handleMessage_shouldThrowExceptionAndPrint() throws Exception {
            // GIVEN
            doThrow(new IllegalStateException("Simulated Error")).when(messageHandler).handleClientMessage(anyString(),
                    anyString());
            String expectedLogPart = "Message Received from user " + SESSION_ID;

            // WHEN / THEN
            assertThrows(IllegalStateException.class,
                    () -> socketConnectionHandler.handleMessage(mockSession, mockTextMessage));
            assertTrue(OUTPUT_STREAM.toString().contains(expectedLogPart),
                    "Der 'Message Received' Log sollte trotz der Ausnahme des Handlers gedruckt werden.");
        }
    }
}