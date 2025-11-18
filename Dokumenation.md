# GameServer — Dokumentation

**Projekt:** Server für das Spiel „Verrücktes Labyrinth"

**Kurzbeschreibung** Dies ist ein Spring Boot WebSocket‑Server, der die Spiellogik für das Spiel „Verrücktes Labyrinth" übernimmt. Clients verbinden sich per WebSocket, senden Aktionen (z. B. CONNECT, START\_GAME, etc.) und erhalten Broadcast‑Nachrichten vom Server.

---

## 1. Wichtige Projektdateien / Ordnerstruktur (Kurzüberblick)

```
src/
  main/
    java/
      com/uni/gamesever/
        GameseverApplication.java                # Spring Boot Einstiegs-Klasse
        config/
          WebSocketConfig.java                    # Registrierung der WebSocket-Handler
          ActionHandlerConfig.java                # Registrierung von Beans (Handler)
        controller/
          SocketConnectionHandlerActions.java    # WebSocket-Handler für Client-Aktionen
          SocketConnectionHandlerBroadcast.java  # WebSocket-Handler für Broadcast-Verbindungen
        classes/
          ConnectionHandler.java                  # Logik für Connect / Lobby
          MessageHandler.java                     # Entgegennahme/Dispatch von Aktionen
          GameBoardHandler.java                   # Spielbrett-Generierung / Spielstart
          PlayerManager.java                      # Verwaltung der Spieler im Spiel
        models/                                   # POJOs / DTOs
          boardSize.java
          bonus.java
          gameBoard.java
          gameStarted.java
          gameStateUpdate.java
          lobbyState.java
          playerInfo.java
          tile.java
          treasure.java
          messages/                               # verschiedene Message-Typen (action payloads)
            message.java
            connectRequest.java
            startGameAction.java
        services/
          SocketBroadcastService.java            # Broadcast- / Session-Verwaltung

pom.xml
```

---

## 2. Architektur & Komponenten (High-Level)

- **Spring Boot**: App wird über `@SpringBootApplication` gestartet. Spring Initialisiert den ApplicationContext, injiziert Beans und startet die Anwendung.
- **WebSocket**: Implementiert mit Spring `WebSocketConfigurer`. Zwei Handlers werden registriert:
  - `/client/actions` — für eingehende clientseitige Aktionen (z. B. CONNECT, START\_GAME)
  - `/server/broadcast` — für Broadcast (Server kann hier Nachrichten an alle Sessions senden)
- **Services / Beans**:
  - `SocketBroadcastService` (`@Service`): verwaltet aktive WebSocketSessions und bietet `broadcastMessage(...)`, `addIncomingSession(...)`, `removeDisconnectedSession(...)`.
  - `MessageHandler` (`@Service`): deserialisiert eingehende JSON‑Nachrichten, routet Aktionen an spezifische Handler (z. B. ConnectionHandler, GameBoardHandler).
  - `ConnectionHandler` (`@Service`): nimmt CONNECT‑Requests entgegen, verwaltet Lobby/Player-Objekte (nutzt `PlayerManager`).
  - `GameBoardHandler` (`@Service`): erzeugt `gameBoard`, initialisiert Spielstart, sendet `gameStarted` Event.
  - `PlayerManager`: einfache Verwaltung (Array) der Spieler.

---

## 3. Was ist eine "Bean" in Spring? (kurz)

Eine *Bean* ist ein Objekt, das von Spring verwaltet wird. Beans werden z. B. durch Annotationen wie `@Component`, `@Service`, `@Repository`, `@Controller` oder durch `@Bean`-Methoden in `@Configuration`-Klassen erzeugt. Der Vorteil ist Dependency Injection (DI): Spring injiziert abhängige Objekte automatisch in Konstruktoren oder Felder.

Beispiele im Projekt:

- `SocketBroadcastService` ist mit `@Service` annotiert → wird automatisch als Bean registriert.
- `ActionHandlerConfig` deklariert `@Bean`-Methoden für `SocketConnectionHandlerActions` und `SocketConnectionHandlerBroadcast`.

---

## 4. Wo kommt was hin? (Guidelines für Entwickler)

- Neue WebSocket‑Aktionen: Implementiere ein neues `message` subtype in `models/messages/` (z. B. `moveAction.java`) und ergänze `MessageHandler` um das Routing auf eine neue Methode/Handler.
- Spiellogik / Board-Änderungen: `classes/GameBoardHandler.java` erweitern. Die Kommunikation mit Clients läuft über `SocketBroadcastService.broadcastMessage(json)`.



Konventionen:

- Konstruktorinjektion (wie bereits verwendet) bevorzugen.
- Services mit `@Service` annotieren.
- Reine DTOs ohne Logik in `models/` ablegen.
- Falls etwas mit new-Deklariert wird, denke nach eine BEAN zu verwenden

---

## 6. Klassendiagram&#x20;
![image](Klassendiagram.png)