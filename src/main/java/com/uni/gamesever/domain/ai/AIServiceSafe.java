package com.uni.gamesever.domain.ai;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.uni.gamesever.domain.enums.BonusType;
import com.uni.gamesever.domain.model.*;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * SICHERE AI - Macht NUR garantiert gültige Züge!
 * Keine Simulation, nur aktuell erreichbare Felder
 * Mit Bonus-Support und Home-Return-Logik
 */
public class AIServiceSafe {

    private final OpenAiService openAiService;
    private final SafeMoveStrategy strategy;
    private final PathfindingService pathfinding;
    private final Gson gson = new Gson();
    private final Random random = new Random();

    public AIServiceSafe(String apiKey) {
        this.openAiService = new OpenAiService(apiKey);
        this.strategy = new SafeMoveStrategy();
        this.pathfinding = new PathfindingService();
    }

    public AIDecision getNextMove(GameState gameState) {
        // 1. Ermittle Ziel (Schatz oder Home)
        SafeMoveStrategy.TargetInfo target = strategy.determineTarget(gameState);

        // 2. Prüfe ob Bonus genutzt werden soll (aggressiv)
        AIDecision bonusDecision = evaluateBonusUsage(gameState, target);
        if (bonusDecision != null) {
            System.out.println("🎁 AI nutzt Bonus: " + bonusDecision.getUseBonus());
            return bonusDecision;
        }

        // 3. Finde AKTUELL erreichbare Felder
        List<SafeMoveStrategy.SafeMoveOption> safeOptions = strategy.findSafeMoves(gameState);

        if (safeOptions.isEmpty()) {
            // Sollte nie passieren (aktuelle Position ist immer erreichbar)
            return createFallbackDecision(gameState);
        }

        // 4. Finde sicheren Push
        SafeMoveStrategy.PushInfo safePush = strategy.findSafePush(gameState);

        // 5. AI wählt bestes Ziel aus AKTUELL erreichbaren Feldern
        SafeMoveStrategy.SafeMoveOption bestMove = chooseBestMove(safeOptions, gameState);

        // 6. Erstelle Entscheidung
        AIDecision decision = new AIDecision();
        decision.setRotations(random.nextInt(3)); // Zufällige Rotation 0-2
        decision.setPushRowOrCol(safePush.index);
        decision.setPushDirection(safePush.direction);
        decision.setMoveTarget(bestMove.targetPosition);
        decision.setGoingHome(bestMove.isGoingHome);

        String targetType = bestMove.isGoingHome ? "HOME" : "SCHATZ";
        decision.setReasoning(String.format("Sichere Bewegung zu (%d,%d), Distanz zum %s: %d",
            bestMove.targetPosition.getColumn(), bestMove.targetPosition.getRow(), targetType, bestMove.distanceToTarget));

        return decision;
    }

    /**
     * Evaluiert ob ein Bonus genutzt werden soll (aggressiv)
     * Gibt AIDecision mit Bonus zurück oder null wenn kein Bonus sinnvoll
     *
     * WICHTIG: Boni können nur in bestimmten Phasen genutzt werden!
     * - BEAM, SWAP: Nur während WAITING_FOR_MOVE
     * - PUSH_FIXED, PUSH_TWICE: Nur während WAITING_FOR_PUSH
     */
    private AIDecision evaluateBonusUsage(GameState gameState, SafeMoveStrategy.TargetInfo target) {
        // Convert String[] to List<BonusType>
        String[] bonusArray = gameState.getMyPlayerState().getAvailableBonuses();
        if (bonusArray == null || bonusArray.length == 0) {
            return null;
        }

        List<BonusType> bonuses = new ArrayList<>();
        for (String bonusName : bonusArray) {
            try {
                bonuses.add(BonusType.valueOf(bonusName));
            } catch (IllegalArgumentException e) {
                // Ignore invalid bonus names
            }
        }

        if (target == null || target.position == null) {
            return null;
        }

        // WICHTIG: Prüfe aktuelle Spielphase!
        TurnState currentPhase = gameState.getCurrentTurnState();
        boolean isInPushPhase = (currentPhase == TurnState.WAITING_FOR_PUSH);
        boolean isInMovePhase = (currentPhase == TurnState.WAITING_FOR_MOVE);

        System.out.println("🔍 Bonus-Evaluation:");
        System.out.println("   Aktuelle Phase: " + currentPhase);
        System.out.println("   Verfügbare Boni: " + bonuses);

        Coordinates myPos = gameState.getMyPlayerState().getCurrentPosition();
        Coordinates currentPos = new Coordinates(myPos.getColumn(), myPos.getRow());

        // Finde erreichbare Felder
        Set<Coordinates> occupiedPositions = getOtherPlayerPositions(gameState);
        Set<Coordinates> reachable = pathfinding.findReachableFields(
            gameState.getBoard(), currentPos, occupiedPositions);

        // Prüfe ob Ziel erreichbar ist
        boolean targetReachable = isCoordinateInSet(target.position, reachable);
        int distanceToTarget = pathfinding.calculateDistance(currentPos, target.position);

        System.out.println("   Ziel erreichbar: " + targetReachable);
        System.out.println("   Distanz zum Ziel: " + distanceToTarget);

        // === PUSH-PHASE BONI (nur während WAITING_FOR_PUSH) ===
        if (isInPushPhase) {
            // PUSH_TWICE - Wenn sehr weit entfernt (Distanz > 4)
            if (bonuses.contains(BonusType.PUSH_TWICE) && distanceToTarget > 4) {
                System.out.println("   → PUSH_TWICE ausgewählt: Große Distanz (" + distanceToTarget + ")");
                AIDecision decision = new AIDecision();
                decision.setUseBonus(BonusType.PUSH_TWICE);
                decision.setGoingHome(target.isHome);
                decision.setReasoning("PUSH_TWICE für bessere Board-Positionierung bei Distanz " + distanceToTarget);

                SafeMoveStrategy.PushInfo safePush = strategy.findSafePush(gameState);
                decision.setRotations(random.nextInt(3));
                decision.setPushRowOrCol(safePush.index);
                decision.setPushDirection(safePush.direction);

                return decision;
            }

            // PUSH_FIXED - Könnte nützlich sein (für späteren Ausbau)
            // TODO: Intelligentere Logik für PUSH_FIXED
        }

        // === MOVE-PHASE BONI (nur während WAITING_FOR_MOVE) ===
        if (isInMovePhase) {
            // BEAM - Wenn Ziel nicht erreichbar
            if (bonuses.contains(BonusType.BEAM) && !targetReachable) {
                System.out.println("   → BEAM ausgewählt: Ziel nicht erreichbar, teleportiere direkt!");
                AIDecision decision = new AIDecision();
                decision.setUseBonus(BonusType.BEAM);
                decision.setBeamTarget(target.position);
                decision.setGoingHome(target.isHome);
                decision.setReasoning("BEAM zum " + (target.isHome ? "Heimfeld" : "Schatz") +
                    " - Ziel war nicht erreichbar");
                return decision;
            }

            // SWAP - Wenn Gegner auf Zielfeld steht
            if (bonuses.contains(BonusType.SWAP)) {
                String playerOnTarget = findPlayerAtPosition(gameState, target.position);
                if (playerOnTarget != null) {
                    System.out.println("   → SWAP ausgewählt: Gegner " + playerOnTarget + " steht auf Zielfeld!");
                    AIDecision decision = new AIDecision();
                    decision.setUseBonus(BonusType.SWAP);
                    decision.setSwapTargetPlayerId(playerOnTarget);
                    decision.setGoingHome(target.isHome);
                    decision.setReasoning("SWAP mit Gegner auf " + (target.isHome ? "Heimfeld" : "Schatzfeld"));
                    return decision;
                }
            }
        }

        System.out.println("   → Kein Bonus in dieser Phase sinnvoll");
        return null; // Kein Bonus sinnvoll
    }

    /**
     * Findet Spieler an einer bestimmten Position (für SWAP)
     */
    private String findPlayerAtPosition(GameState gameState, Coordinates position) {
        String myPlayerId = gameState.getMyPlayerState().getPlayerInfo().getId();

        for (PlayerState player : gameState.getPlayers()) {
            if (player.getPlayerInfo() != null &&
                !player.getPlayerInfo().getId().equals(myPlayerId)) {
                Coordinates pos = player.getCurrentPosition();
                if (pos != null &&
                    pos.getColumn() == position.getColumn() &&
                    pos.getRow() == position.getRow()) {
                    return player.getPlayerInfo().getId();
                }
            }
        }
        return null;
    }

    /**
     * Sammelt Positionen anderer Spieler
     */
    private Set<Coordinates> getOtherPlayerPositions(GameState gameState) {
        Set<Coordinates> occupied = new HashSet<>();
        String myPlayerId = gameState.getMyPlayerState().getPlayerInfo().getId();

        for (PlayerState player : gameState.getPlayers()) {
            if (player.getPlayerInfo() != null &&
                !player.getPlayerInfo().getId().equals(myPlayerId)) {
                Coordinates pos = player.getCurrentPosition();
                if (pos != null) {
                    occupied.add(new Coordinates(pos.getColumn(), pos.getRow()));
                }
            }
        }
        return occupied;
    }

    /**
     * Prüft ob Koordinate in Set enthalten ist (equals-Problem umgehen)
     */
    private boolean isCoordinateInSet(Coordinates coord, Set<Coordinates> set) {
        for (Coordinates c : set) {
            if (c.getColumn() == coord.getColumn() && c.getRow() == coord.getRow()) {
                return true;
            }
        }
        return false;
    }

    private SafeMoveStrategy.SafeMoveOption chooseBestMove(
            List<SafeMoveStrategy.SafeMoveOption> options, GameState gameState) {

        // OPTIMIERUNG: Wenn wir nach Hause gehen und das beste Feld Distanz 0 hat,
        // oder wenn die beste Option sehr klar ist (Distanz 0-1), nimm sie direkt!
        SafeMoveStrategy.SafeMoveOption bestOption = options.get(0);

        if (bestOption.isGoingHome) {
            // HOME-MODUS: Priorisiere direkten Weg nach Hause
            if (bestOption.distanceToTarget == 0) {
                System.out.println("🏠 HOME DIREKT ERREICHBAR! Gehe sofort nach Hause.");
                return bestOption;
            }
            if (bestOption.distanceToTarget <= 2) {
                System.out.println("🏠 HOME NAHE (Distanz=" + bestOption.distanceToTarget + ") - nehme kürzesten Weg.");
                return bestOption;
            }
        } else {
            // SCHATZ-MODUS: Wenn Schatz direkt erreichbar, nimm ihn!
            if (bestOption.distanceToTarget == 0) {
                System.out.println("💎 SCHATZ DIREKT ERREICHBAR! Gehe sofort hin.");
                return bestOption;
            }
        }

        // Für komplexere Situationen: Frage AI
        String prompt = buildPrompt(options, gameState);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", getSystemPrompt(bestOption.isGoingHome)));
        messages.add(new ChatMessage("user", prompt));

        try {
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(AIConfig.getModel())
                    .messages(messages)
                    .temperature(AIConfig.getTemperature())
                    .maxTokens(AIConfig.getMaxTokens())
                    .build();

            String response = openAiService.createChatCompletion(request)
                    .getChoices().get(0).getMessage().getContent();

            return parseChoice(response, options);

        } catch (Exception e) {
            System.err.println("AI Fehler, verwende beste Option: " + e.getMessage());
            return options.get(0); // Fallback: Nächste zum Ziel
        }
    }

    private String getSystemPrompt(boolean isGoingHome) {
        String ziel = isGoingHome ? "HEIMATPOSITION" : "Schatz";
        String priorität = isGoingHome
            ? "WICHTIG: Du musst nach Hause! Wähle IMMER das Feld mit der KLEINSTEN Distanz zur Heimatposition. Die erste Option (⭐) ist bereits die beste!"
            : "Wähle das Feld mit der kleinsten Distanz zum Schatz. Die erste Option (⭐) ist bereits die beste!";

        return String.format("""
            Du bist eine KI für Labyrinth.

            Du bekommst eine Liste von AKTUELL ERREICHBAREN Feldern.
            ALLE Optionen sind GARANTIERT GÜLTIG!

            Ziel: %s
            %s

            Antworte mit JSON:
            {
              "chosenOption": <Nummer 1-N>,
              "reasoning": "<1 Satz>"
            }
            """, ziel, priorität);
    }

    private String buildPrompt(List<SafeMoveStrategy.SafeMoveOption> options, GameState gameState) {
        Coordinates myPos = gameState.getMyPlayerState().getCurrentPosition();
        Treasure treasure = gameState.getMyPlayerState().getCurrentTreasure();
        boolean goingHome = options.size() > 0 && options.get(0).isGoingHome;

        StringBuilder prompt = new StringBuilder();
        prompt.append("Position: (").append(myPos.getColumn()).append(",").append(myPos.getRow()).append(")\n");

        if (goingHome) {
            Coordinates home = gameState.getMyPlayerState().getHomePosition();
            prompt.append("Ziel: HEIMATPOSITION (").append(home.getColumn()).append(",").append(home.getRow()).append(")\n");
            prompt.append("ALLE SCHÄTZE GESAMMELT! Kehre nach Hause zurück!\n\n");
        } else {
            prompt.append("Ziel: Schatz #").append(treasure != null ? treasure.getId() : "?").append("\n\n");
        }

        prompt.append("AKTUELL ERREICHBARE FELDER:\n\n");

        for (int i = 0; i < Math.min(options.size(), 10); i++) {
            SafeMoveStrategy.SafeMoveOption opt = options.get(i);
            prompt.append(i + 1).append(". Position (")
                  .append(opt.targetPosition.getColumn()).append(",").append(opt.targetPosition.getRow())
                  .append(") - Distanz: ").append(opt.distanceToTarget);

            if (opt.isCurrentPosition) {
                prompt.append(" (BLEIBE STEHEN)");
            }
            if (i == 0) {
                prompt.append(" ⭐");
            }
            prompt.append("\n");
        }

        prompt.append("\nWelche Option?");
        return prompt.toString();
    }

    private SafeMoveStrategy.SafeMoveOption parseChoice(String response, List<SafeMoveStrategy.SafeMoveOption> options) {
        try {
            int jsonStart = response.indexOf("{");
            int jsonEnd = response.lastIndexOf("}") + 1;

            if (jsonStart != -1 && jsonEnd > jsonStart) {
                String jsonStr = response.substring(jsonStart, jsonEnd);
                JsonObject json = gson.fromJson(jsonStr, JsonObject.class);
                int choice = json.get("chosenOption").getAsInt();

                if (choice >= 1 && choice <= options.size()) {
                    return options.get(choice - 1);
                }
            }
        } catch (Exception e) {
            // Ignoriere Parse-Fehler
        }

        return options.get(0); // Fallback
    }

    private AIDecision createFallbackDecision(GameState gameState) {
        Coordinates myPos = gameState.getMyPlayerState().getCurrentPosition();
        SafeMoveStrategy.PushInfo safePush = strategy.findSafePush(gameState);

        AIDecision decision = new AIDecision();
        decision.setRotations(0);
        decision.setPushRowOrCol(safePush.index);
        decision.setPushDirection(safePush.direction);
        decision.setMoveTarget(new Coordinates(myPos.getColumn(), myPos.getRow()));
        decision.setReasoning("Fallback: Bleibe an aktueller Position");
        return decision;
    }
}
