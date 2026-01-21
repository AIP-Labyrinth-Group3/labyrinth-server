package com.uni.gamesever.domain.ai;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Zentrale Konfiguration für alle AI-Services
 * Alle AI-bezogenen Einstellungen an einem Ort!
 */
public class AIConfig {

    // Verwende Environment-Variable: export OPENAI_API_KEY="sk-your-key"
    // Oder nutze die .env Datei (aus Client oder Server)

    // ========== OPENAI MODEL KONFIGURATION ==========
    private static final String AI_MODEL = "gpt-5.1";        // Identisch zum Client
    private static final double AI_TEMPERATURE = 0.5;         // Niedrig = konsistenter, 0.7 = kreativer
    private static final int AI_MAX_TOKENS = 300;             // Maximale Response-Länge

    // ========== AI VERHALTEN ==========
    private static final int AI_DELAY_MS = 500;               // Verzögerung vor AI-Zug (ms)
    private static final int AI_ROTATION_DELAY_MS = 40;       // Verzögerung zwischen Rotationen
    private static final int AI_PUSH_DELAY_MS = 50;           // Verzögerung vor Push
    private static final int AI_MOVE_WAIT_MS = 150;           // Warten auf Server nach Push

    // ========== CACHE FÜR API-KEY ==========
    private static String cachedApiKey = null;

    // ========== PUBLIC API ==========

    /**
     * Holt den OpenAI API-Key aus Environment-Variable oder .env Datei
     * Priorität: 1. Environment-Variable, 2. .env Datei (Server), 3. .env Datei (Client)
     */
    public static String getApiKey() {
        // Wenn bereits gecached, wiederverwenden
        if (cachedApiKey != null) {
            return cachedApiKey;
        }

        // 1. Versuche Environment-Variable
        String apiKey = System.getenv("OPENAI_API_KEY");

        // 2. Wenn nicht vorhanden, versuche .env Datei zu laden
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = loadFromEnvFile();
        }

        // 3. Wenn immer noch nicht vorhanden, zeige Fehler
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("\n❌ FEHLER: OPENAI_API_KEY nicht gesetzt!");
            System.err.println("┌────────────────────────────────────────────┐");
            System.err.println("│ AI-Modus ist nicht verfügbar!             │");
            System.err.println("├────────────────────────────────────────────┤");
            System.err.println("│ Lösung:                                    │");
            System.err.println("│ 1. Setze Environment-Variable:             │");
            System.err.println("│    export OPENAI_API_KEY=\"sk-your-key\"   │");
            System.err.println("│                                            │");
            System.err.println("│ 2. ODER nutze .env Datei vom Client        │");
            System.err.println("│    (wird automatisch gefunden)             │");
            System.err.println("└────────────────────────────────────────────┘\n");
            return null;
        }

        // Cache für später
        cachedApiKey = apiKey;
        return apiKey;
    }

    /**
     * Lädt API-Key aus .env Datei
     * Sucht zuerst im Server-Verzeichnis, dann im Client-Verzeichnis
     */
    private static String loadFromEnvFile() {
        // 1. Versuche .env im Server-Projekt zu finden
        String apiKey = tryLoadFromPath(findProjectRoot());
        if (apiKey != null) {
            return apiKey;
        }

        // 2. Versuche .env im Client-Projekt zu finden (sibling directory)
        String apiKeyFromClient = tryLoadFromClientEnv();
        if (apiKeyFromClient != null) {
            System.out.println("✅ API-Key aus Client .env Datei geladen");
            return apiKeyFromClient;
        }

        return null;
    }

    /**
     * Versucht .env Datei vom Client-Projekt zu laden
     * Navigiert zu ../Client_Current/labyrinth-client/.env
     */
    private static String tryLoadFromClientEnv() {
        try {
            Path projectRoot = findProjectRoot();
            if (projectRoot == null) {
                return null;
            }

            // Von Server-Root zu Client navigieren:
            // /Server_Current/labyrinth-server/ -> /Client_Current/labyrinth-client/
            Path serverParent = projectRoot.getParent(); // /Server_Current/
            if (serverParent != null) {
                Path advancedProjekt = serverParent.getParent(); // /AdvancedProjekt/
                if (advancedProjekt != null) {
                    Path clientEnv = advancedProjekt
                            .resolve("Client_Current")
                            .resolve("labyrinth-client")
                            .resolve(".env");

                    File envFile = clientEnv.toFile();
                    if (envFile.exists() && envFile.isFile()) {
                        String key = readApiKeyFromFile(envFile);
                        if (key != null) {
                            System.out.println("✅ API-Key aus Client .env gefunden: " + clientEnv);
                            return key;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Stille Fehlerbehandlung
        }

        return null;
    }

    /**
     * Versucht API-Key aus .env Datei im gegebenen Pfad zu laden
     */
    private static String tryLoadFromPath(Path projectRoot) {
        if (projectRoot == null) {
            return null;
        }

        File envFile = projectRoot.resolve(".env").toFile();

        // Prüfe ob .env Datei existiert
        if (!envFile.exists() || !envFile.isFile()) {
            return null;
        }

        String key = readApiKeyFromFile(envFile);
        if (key != null) {
            System.out.println("✅ API-Key aus Server .env Datei geladen");
        }
        return key;
    }

    /**
     * Liest OPENAI_API_KEY aus .env Datei
     */
    private static String readApiKeyFromFile(File envFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Ignoriere Kommentare und leere Zeilen
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Suche nach OPENAI_API_KEY
                if (line.startsWith("OPENAI_API_KEY=")) {
                    String key = line.substring("OPENAI_API_KEY=".length()).trim();
                    // Entferne Anführungszeichen falls vorhanden
                    if ((key.startsWith("\"") && key.endsWith("\"")) ||
                        (key.startsWith("'") && key.endsWith("'"))) {
                        key = key.substring(1, key.length() - 1);
                    }
                    if (!key.isEmpty()) {
                        return key;
                    }
                }
            }
        } catch (IOException e) {
            // Stille Fehlerbehandlung
        }

        return null;
    }

    /**
     * Findet das Projekt-Root-Verzeichnis (wo pom.xml liegt)
     */
    private static Path findProjectRoot() {
        try {
            // Starte vom aktuellen Arbeitsverzeichnis
            Path currentPath = Paths.get(System.getProperty("user.dir")).toAbsolutePath();

            // Suche nach pom.xml
            Path searchPath = currentPath;
            int maxDepth = 10; // Maximal 10 Ebenen hoch suchen

            for (int i = 0; i < maxDepth; i++) {
                File pomFile = searchPath.resolve("pom.xml").toFile();
                if (pomFile.exists() && pomFile.isFile()) {
                    return searchPath;
                }

                // Eine Ebene höher
                Path parent = searchPath.getParent();
                if (parent == null || parent.equals(searchPath)) {
                    break;
                }
                searchPath = parent;
            }
        } catch (Exception e) {
            // Bei Fehler, versuche aktuelles Verzeichnis
        }

        // Fallback: aktuelles Verzeichnis
        return Paths.get(System.getProperty("user.dir")).toAbsolutePath();
    }

    /**
     * Prüft ob AI verfügbar ist (API-Key gesetzt)
     */
    public static boolean isAvailable() {
        String key = getApiKey();
        return key != null && !key.isEmpty();
    }

    /**
     * OpenAI Model Name (z.B. "gpt-5.1", "gpt-4")
     */
    public static String getModel() {
        return AI_MODEL;
    }

    /**
     * Temperature für OpenAI (0.0 - 1.0)
     * Niedrig = konsistent, Hoch = kreativ
     */
    public static double getTemperature() {
        return AI_TEMPERATURE;
    }

    /**
     * Maximale Token-Anzahl für AI-Response
     */
    public static int getMaxTokens() {
        return AI_MAX_TOKENS;
    }

    /**
     * Verzögerung vor AI-Zug Start (ms)
     */
    public static int getAiDelay() {
        return AI_DELAY_MS;
    }

    /**
     * Verzögerung zwischen Tile-Rotationen (ms)
     */
    public static int getRotationDelay() {
        return AI_ROTATION_DELAY_MS;
    }

    /**
     * Verzögerung vor Push-Aktion (ms)
     */
    public static int getPushDelay() {
        return AI_PUSH_DELAY_MS;
    }

    /**
     * Wartezeit auf Server nach Push (ms)
     */
    public static int getMoveWaitTime() {
        return AI_MOVE_WAIT_MS;
    }
}
