package com.uni.gamesever.interfaces.Websocket;

import com.uni.gamesever.domain.exceptions.ConnectionRejectedException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SocketConnectionHandlerTest {
    private final PrintStream STANDARD_OUT = System.out;
    private final ByteArrayOutputStream OUTPUT_STREAM = new ByteArrayOutputStream();
    private final String SESSION_ID = "SESSION_XYZ";

    @InjectMocks
    private SocketConnectionHandler socketConnectionHandler;

    @Mock
    private SocketMessageService socketMessageService;
    @Mock
    private MessageHandler messageHandler;
    @Mock
    private WebSocketSession mockSession;

    @BeforeEach
    void setUp() {
        when(mockSession.getId()).thenReturn(SESSION_ID);

        System.setOut(new PrintStream(OUTPUT_STREAM));
        when(mockSession.isOpen()).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
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
            doThrow(new RuntimeException("Simulated Service Error")).when(socketMessageService).addIncomingSession(any());

            // WHEN / THEN
            assertThrows(RuntimeException.class, () -> socketConnectionHandler.afterConnectionEstablished(mockSession));
            assertEquals("", OUTPUT_STREAM.toString(), "Die Konsolenausgabe sollte leer sein, da die Exception davor geworfen werden sollte.");
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
            doThrow(new RuntimeException("Simulated Removal Error")).when(socketMessageService).removeDisconnectedSession(any());

            // WHEN / THEN
            assertThrows(RuntimeException.class, () -> socketConnectionHandler.afterConnectionClosed(mockSession, mockStatus));
            assertEquals("", OUTPUT_STREAM.toString(), "Die Konsolenausgabe sollte leer sein, da die Exception davor geworfen werden sollte.");
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
            // WHEN
            socketConnectionHandler.handleMessage(mockSession, mockTextMessage);

            // THEN
            verify(messageHandler, times(1)).handleClientMessage(eq(RAW_PAYLOAD), eq(SESSION_ID));
        }

        @Test
        void handleMessage_shouldCloseSession_onConnectionRejected() throws Exception {
            doThrow(new ConnectionRejectedException("Rejected")).when(messageHandler).handleClientMessage(anyString(), anyString());

            socketConnectionHandler.handleMessage(mockSession, mockTextMessage);

            verify(mockSession).close(CloseStatus.POLICY_VIOLATION);
        }
    }
}