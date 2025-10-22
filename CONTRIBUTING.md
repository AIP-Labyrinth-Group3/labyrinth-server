# Contributing Guide - Das verrückte Labyrinth

## Git Workflow

### Branch-Strategie

Wir verwenden Git Flow:

- **`main`**: Produktiv-Branch, nur vollständig getesteter und freigegebener Code
- **`develop`**: Entwicklungs-Branch, Integration aller Features
- **`feature/*`**: Feature-Branches für neue Funktionen
- **`bugfix/*`**: Bugfix-Branches für Fehlerbehebungen in develop
- **`hotfix/*`**: Hotfix-Branches für kritische Produktiv-Fixes

### Feature entwickeln
```bash
# 1. Aktuellen Stand holen
git checkout develop
git pull origin develop

# 2. Feature-Branch erstellen
git checkout -b feature/mein-feature-name

# 3. Entwickeln und committen
git add .
git commit -m "feat: beschreibung der änderung"

# 4. Regelmäßig pushen
git push -u origin feature/mein-feature-name

# 5. Pull Request auf develop erstellen
# Gehe zu GitHub und erstelle einen PR
```

### Bugfix entwickeln
```bash
git checkout develop
git pull origin develop
git checkout -b bugfix/bug-beschreibung
# ... entwickeln ...
git commit -m "fix: beschreibung des bugfixes"
git push -u origin bugfix/bug-beschreibung
```

### Hotfix für Production
```bash
git checkout main
git pull origin main
git checkout -b hotfix/kritischer-fix
# ... fixen ...
git commit -m "fix: kritischer produktiv-fix"
git push -u origin hotfix/kritischer-fix
# PR auf main UND develop erstellen
```

## Commit Messages

Wir verwenden **Conventional Commits**:
```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- **feat**: Neue Funktion
- **fix**: Bugfix
- **docs**: Nur Dokumentation
- **style**: Code-Formatierung (keine funktionale Änderung)
- **refactor**: Code-Refactoring
- **test**: Tests hinzufügen oder korrigieren
- **chore**: Build-Prozess, Abhängigkeiten, etc.
- **perf**: Performance-Verbesserung

### Beispiele
```bash
feat(server): add player timeout mechanism

Implement automatic player removal after 30 seconds of inactivity
with a 3-second countdown warning.

Closes #42
```
```bash
fix(client): correct board rendering on move

The board was not updating correctly when a player moved.
Fixed the state synchronization between server and client.

Fixes #38
```
```bash
docs: update API documentation

Add examples for WebSocket communication between
client and server.
```

## Code-Qualität Standards

### Java Code Conventions

- Folge den [Java Code Conventions](https://www.oracle.com/java/technologies/javase/codeconventions-contents.html)
- Verwende aussagekräftige Variablen- und Methodennamen
- Maximale Zeilenlänge: 120 Zeichen
- Maximale Methodenlänge: 150 Zeilen

### JavaDoc

Alle public und protected Methoden/Klassen benötigen JavaDoc:
```java
/**
 * Verschiebt eine Reihe des Spielfelds in die angegebene Richtung.
 *
 * @param row Die zu verschiebende Reihe (0-basiert)
 * @param direction Die Richtung (LEFT oder RIGHT)
 * @return true wenn erfolgreich, false bei ungültiger Operation
 * @throws IllegalArgumentException wenn row außerhalb des gültigen Bereichs
 */
public boolean pushRow(int row, Direction direction) {
    // Implementation
}
```

### Test Coverage

- Minimum: **80% Code Coverage**
- Unit Tests für alle Business-Logik
- Integrationstests für Server-Client-Kommunikation
- Tests müssen vor jedem Commit lokal laufen
```bash
# Tests lokal ausführen
mvn clean test

# Mit Coverage Report
mvn clean test jacoco:report

# Coverage prüfen
mvn jacoco:check
```

### Checkstyle

Alle Checkstyle-Prüfungen müssen bestehen:
```bash
mvn checkstyle:check
```

## Pull Request Prozess

### 1. Vor dem PR
```bash
# Code aktualisieren
git checkout develop
git pull origin develop
git checkout feature/mein-feature
git rebase develop

# Tests ausführen
mvn clean verify

# Checkstyle prüfen
mvn checkstyle:check

# Bei Erfolg pushen
git push origin feature/mein-feature
```

### 2. PR erstellen

1. Gehe zu GitHub
2. Klicke "Compare & pull request"
3. Wähle Base: `develop` (bei Features)
4. Fülle die PR-Vorlage aus:
```markdown
## Beschreibung
Was wurde geändert und warum?

## Art der Änderung
- [ ] Bugfix (nicht-breaking change)
- [ ] Neue Funktion (nicht-breaking change)
- [ ] Breaking Change
- [ ] Dokumentation

## Checklist
- [ ] Code folgt den Style Guidelines
- [ ] Self-review durchgeführt
- [ ] Code kommentiert (komplexe Stellen)
- [ ] Dokumentation aktualisiert
- [ ] Keine neuen Warnings
- [ ] Tests hinzugefügt
- [ ] Alle Tests bestehen lokal
- [ ] Abhängige Änderungen wurden gemerged

## Tests
Welche Tests wurden hinzugefügt/geändert?

## Screenshots (falls UI-Änderungen)

## Related Issues
Closes #(issue)
```

### 3. Code Review

- Mindestens 1 Approval erforderlich
- Alle CI-Checks müssen grün sein
- Alle Kommentare müssen resolved sein
- Keine Merge-Konflikte

### 4. Mergen

- Verwende "Squash and merge" für Feature-Branches
- Verwende "Merge commit" für Releases
- Lösche Branch nach dem Merge

## Entwicklungsumgebung Setup

### Erforderliche Software

- JDK 17 oder höher
- Maven 3.8+
- Git 2.30+
- IDE (IntelliJ IDEA empfohlen)

### Projekt Setup
```bash
# Repository klonen
git clone https://github.com/dein-username/labyrinth-server.git
cd labyrinth-server

# Dependencies installieren
mvn clean install

# Tests ausführen
mvn test

# Server starten
mvn spring-boot:run
```

### IDE Konfiguration

#### IntelliJ IDEA

1. Import Project → Maven
2. Settings → Editor → Code Style → Import: `checkstyle.xml`
3. Settings → Build → Compiler → Annotation Processors → Enable
4. Install Plugins: Checkstyle-IDEA, SonarLint

#### Eclipse

1. Import → Existing Maven Projects
2. Preferences → Java → Code Style → Formatter → Import
3. Preferences → Checkstyle → New Check Configuration

## Häufige Probleme

### "Tests schlagen fehl"
```bash
# Dependency-Cache löschen
mvn clean

# Neuinstallation
mvn clean install -U
```

### "Checkstyle Fehler"
```bash
# Checkstyle Report erstellen
mvn checkstyle:checkstyle

# Report ansehen
open target/site/checkstyle.html
```

### "Merge-Konflikt"
```bash
# Develop Branch aktualisieren
git checkout develop
git pull origin develop

# Zurück zum Feature Branch
git checkout feature/mein-feature

# Rebase
git rebase develop

# Konflikte lösen und fortfahren
git add .
git rebase --continue

# Force push (bei bereits gepushtem Branch)
git push --force-with-lease
```

## Kontakt & Hilfe

Bei Fragen:
- Erstelle ein Issue auf GitHub
- Siehe README.md für weitere Dokumentation

## Lizenz

Dieses Projekt ist Teil der Lehrveranstaltung "Advanced Integrative Project"
am MCI Innsbruck.