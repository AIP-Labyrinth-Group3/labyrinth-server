# Das verrückte Labyrinth - Server

![Build Status](https://github.com/dein-username/labyrinth-server/workflows/Server%20CI/badge.svg)
![Coverage](https://img.shields.io/badge/coverage-80%25-green)
![Java](https://img.shields.io/badge/Java-17-blue)
![License](https://img.shields.io/badge/license-Educational-lightgrey)

Spielserver für das digitale Brettspiel "Das verrückte Labyrinth" - entwickelt im Rahmen des Advanced Integrative Project am MCI Innsbruck.

## 📋 Inhaltsverzeichnis

- [Über das Projekt](#über-das-projekt)
- [Features](#features)
- [Technologie-Stack](#technologie-stack)
- [Installation](#installation)
- [Verwendung](#verwendung)
- [API Dokumentation](#api-dokumentation)
- [Entwicklung](#entwicklung)
- [Testing](#testing)
- [Deployment](#deployment)
- [Contributing](#contributing)
- [Team](#team)

## 🎮 Über das Projekt

Dieses Projekt implementiert einen verteilten Spielserver für "Das verrückte Labyrinth". Der Server verwaltet die Spiellogik, koordiniert mehrere Clients und stellt sicher, dass alle Spielregeln eingehalten werden.

### Projektziele

- Client-Server-Architektur mit WebSocket-Kommunikation
- Unterstützung von 2-4 Spielern gleichzeitig
- Regelkonforme Spiellogik nach Ravensburger-Original
- Kompatibilität zwischen verschiedenen Implementierungen
- Robuste Fehlerbehandlung und Timeout-Management

## ✨ Features

- ✅ Verwaltung mehrerer paralleler Spielsessions
- ✅ WebSocket-basierte Echtzeit-Kommunikation
- ✅ Automatische Spieler-Timeouts (30 Sekunden)
- ✅ Konfigurierbares Spielfeld (n×m)
- ✅ Boni-System (Beamen, Tauschen, etc.)
- ✅ Achievement-System
- ✅ KI-Spieler Unterstützung
- ✅ Umfassendes Logging und Monitoring
- ✅ REST-API für Server-Discovery

## 🛠 Technologie-Stack

- **Java 17** - Programmiersprache
- **Spring Boot 3.2** - Application Framework
- **Spring WebSocket** - Echtzeit-Kommunikation
- **Maven** - Build-Tool
- **JUnit 5** - Testing Framework
- **Mockito** - Mocking Framework
- **JaCoCo** - Code Coverage
- **Checkstyle** - Code Quality
- **Docker** - Containerisierung

## 📦 Installation

### Voraussetzungen

- JDK 17 oder höher
- Maven 3.8+
- Git

### Repository klonen
```bash
git clone https://github.com/dein-username/labyrinth-server.git
cd labyrinth-server
```

### Dependencies installieren
```bash
mvn clean install
```

### Build
```bash
mvn clean package
```

## 🚀 Verwendung

### Server starten (Development)
```bash
mvn spring-boot:run
```

### Server starten (Production JAR)
```bash
java -jar target/labyrinth-server.jar
```

### Mit Docker
```bash
# Image bauen
docker build -t labyrinth-server .

# Container starten
docker run -p 8080:8080 labyrinth-server
```

### Konfiguration

Erstelle `application.properties` oder `application.yml`:
```yaml
server:
  port: 8080

labyrinth:
  board:
    default-size: 7x7
  game:
    max-players: 4
    min-players: 2
    timeout-seconds: 30
    treasure-count: 12
  management-server:
    url: http://localhost:8081
```

## 📚 API Dokumentation

### REST Endpoints (Verwaltungsserver)

#### Server registrieren
```http
POST /api/v1/servers
Content-Type: application/json

{
  "name": "Mein Server",
  "uri": "ws://localhost:8080",
  "max_players": 4
}
```

#### Verfügbare Server abrufen
```http
GET /api/v1/servers
```

### WebSocket Events (Spielserver)

#### Client verbinden
```json
{
  "type": "CONNECT",
  "payload": {
    "username": "Spieler1"
  }
}
```

#### Tile schieben
```json
{
  "type": "PUSH_TILE",
  "payload": {
    "position": {"x": 0, "y": 3},
    "direction": "RIGHT"
  }
}
```

Vollständige API-Dokumentation: [API.md](docs/API.md)

## 👨‍💻 Entwicklung

### Projekt-Struktur
```
labyrinth-server/
├── .github/
│   └── workflows/        # CI/CD Pipelines
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/labyrinth/server/
│   │   │       ├── config/       # Konfigurationen
│   │   │       ├── controller/   # REST & WebSocket Controller
│   │   │       ├── model/        # Domain Models
│   │   │       ├── service/      # Business Logic
│   │   │       ├── repository/   # Data Access
│   │   │       └── util/         # Utilities
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/
│           └── com/labyrinth/server/
│               ├── controller/
│               ├── service/
│               └── integration/
├── docs/                 # Dokumentation
├── checkstyle.xml
├── pom.xml
├── Dockerfile
└── README.md
```

### Development Workflow

Siehe [CONTRIBUTING.md](CONTRIBUTING.md) für detaillierte Informationen.
```bash
# Feature Branch erstellen
git checkout develop
git checkout -b feature/mein-feature

# Entwickeln und testen
mvn test

# Code Quality prüfen
mvn checkstyle:check

# Committen
git commit -m "feat: beschreibung"

# Push und PR erstellen
git push -u origin feature/mein-feature
```

## 🧪 Testing

### Unit Tests ausführen
```bash
mvn test
```

### Integration Tests ausführen
```bash
mvn verify
```

### Code Coverage
```bash
mvn jacoco:report
open target/site/jacoco/index.html
```

### Code Quality Check
```bash
mvn checkstyle:check
```

### Alle Tests + Quality Checks
```bash
mvn clean verify checkstyle:check
```

## 🐳 Deployment

### Docker Compose
```yaml
version: '3.8'

services:
  labyrinth-server:
    image: labyrinth-server:latest
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - LABYRINTH_MANAGEMENT_SERVER_URL=http://management-server:8081
    restart: unless-stopped
    
  management-server:
    image: labyrinth-management:latest
    ports:
      - "8081:8081"
    restart: unless-stopped
```

### Kubernetes Deployment
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: labyrinth-server
spec:
  replicas: 3
  selector:
    matchLabels:
      app: labyrinth-server
  template:
    metadata:
      labels:
        app: labyrinth-server
    spec:
      containers:
      - name: labyrinth-server
        image: labyrinth-server:latest
        ports:
        - containerPort: 8080
```

## 🤝 Contributing

Contributions sind willkommen! Bitte lies [CONTRIBUTING.md](CONTRIBUTING.md) für Details.

### Quick Start

1. Fork das Repository
2. Erstelle einen Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Committe deine Änderungen (`git commit -m 'feat: Add some AmazingFeature'`)
4. Push zum Branch (`git push origin feature/AmazingFeature`)
5. Öffne einen Pull Request

## 👥 Team

**Gruppe 3**

- Clemens Siebers
- Rene Stockinger
- Andreas Rofner
- Mario Gottwald
- Simon Raass
- Manuel Kirchebner
- David Strauß

## 📄 Lizenz

Dieses Projekt ist Teil der Lehrveranstaltung "Advanced Integrative Project" am MCI Innsbruck (WS 2025/26).

## 🔗 Links

