package com.uni.gamesever.domain.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.uni.gamesever.domain.enums.BonusType;
import com.uni.gamesever.domain.enums.DirectionType;
import com.uni.gamesever.domain.exceptions.NotPlayersTurnException;
import com.uni.gamesever.domain.exceptions.PushNotValidException;
import com.uni.gamesever.domain.exceptions.UserNotFoundException;
import com.uni.gamesever.domain.game.GameManager;
import com.uni.gamesever.domain.game.PlayerManager;
import com.uni.gamesever.domain.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server AI Manager - Manages AI instances for disconnected players
 * Handles AI activation, deactivation, and decision execution
 */
@Service
public class ServerAIManager {
    private final Map<String, AIServiceSafe> aiInstances = new ConcurrentHashMap<>();
    private final Map<String, Boolean> aiActiveFlags = new ConcurrentHashMap<>();
    private final PlayerManager playerManager;
    private final GameManager gameManager;

    @Autowired
    public ServerAIManager(PlayerManager playerManager, @Lazy GameManager gameManager) {
        this.playerManager = playerManager;
        this.gameManager = gameManager;
    }

    /**
     * Activates AI for a player when they disconnect
     * @param identifierToken Der fixe Token des Spielers
     */
    public void activateAI(String identifierToken) {
        if (!aiActiveFlags.getOrDefault(identifierToken, false)) {
            aiActiveFlags.put(identifierToken, true);
            // Lazy-initialize AI instance
            if (!aiInstances.containsKey(identifierToken)) {
                String apiKey = AIConfig.getApiKey();
                if (apiKey != null) {
                    aiInstances.put(identifierToken, new AIServiceSafe(apiKey));
                    System.out.println("🤖 AI instance created for player: " + identifierToken);
                } else {
                    System.err.println("❌ Cannot create AI: OpenAI API key not available");
                }
            }
        }
    }

    /**
     * Deactivates AI when player reconnects
     * @param identifierToken Der fixe Token des Spielers
     */
    public void deactivateAI(String identifierToken) {
        aiActiveFlags.put(identifierToken, false);
        // Keep AI instance cached for potential future use
        System.out.println("🤖 AI deactivated for player: " + identifierToken);
    }

    /**
     * Checks if AI should control this player
     * @param identifierToken Der fixe Token des Spielers
     */
    public boolean isAIActive(String identifierToken) {
        return aiActiveFlags.getOrDefault(identifierToken, false);
    }

    /**
     * Executes AI turn for disconnected player
     * @param identifierToken Der fixe Token des Spielers
     */
    public void executeAITurn(String identifierToken) throws Exception {
        if (!isAIActive(identifierToken)) {
            System.out.println("⚠️ AI not active for player: " + identifierToken);
            return;
        }

        AIServiceSafe ai = aiInstances.get(identifierToken);
        if (ai == null) {
            System.err.println("❌ No AI instance for player: " + identifierToken);
            executeRandomFallback(identifierToken);
            return;
        }

        try {
            // Build GameState from server data
            GameState gameState = buildGameStateForPlayer(identifierToken);

            // Get AI decision
            AIDecision decision = ai.getNextMove(gameState);
            System.out.println("🤖 AI Decision: " + decision.getReasoning());

            // Execute decision through GameManager
            executeDecision(identifierToken, decision, gameState);

        } catch (Exception e) {
            System.err.println("❌ AI execution failed for player " + identifierToken + ": " + e.getMessage());
            e.printStackTrace();
            // Fallback: skip turn (GameManager will handle this)
            throw e;
        }
    }

    /**
     * Builds GameState from server models
     * @param identifierToken Der fixe Token des Spielers (ändert sich nicht bei Reconnect)
     */
    private GameState buildGameStateForPlayer(String identifierToken) {
        GameState gameState = new GameState();

        // Set board
        gameState.setBoard(gameManager.getCurrentBoard().getTiles());

        // Set players
        PlayerState[] serverPlayers = playerManager.getNonNullPlayerStates();
        List<PlayerState> playerList = new ArrayList<>();
        for (PlayerState player : serverPlayers) {
            if (player != null) {
                playerList.add(player);
            }
        }
        gameState.setPlayers(playerList);

        // Set my player - WICHTIG: Suche nach identifierToken, nicht nach ID!
        PlayerState myPlayer = playerManager.getPlayerStateByIdentifierToken(identifierToken);
        gameState.setMyPlayerState(myPlayer);
        gameState.setMyPlayerId(identifierToken);

        // Set turn state
        gameState.setCurrentTurnState(gameManager.getTurnInfo().getTurnState());

        // Set last push (convert PushActionInfo to LastPush)
        PushActionInfo serverLastPush = gameManager.getCurrentBoard().getLastPush();
        if (serverLastPush != null) {
            LastPush lastPush = new LastPush(
                serverLastPush.getRowOrColIndex(),
                serverLastPush.getDirection()
            );
            gameState.setLastPush(lastPush);
        }

        return gameState;
    }

    /**
     * Executes AI decision through GameManager
     * @param identifierToken Der fixe Token des Spielers
     */
    private void executeDecision(String identifierToken, AIDecision decision, GameState gameState) throws Exception {
        TurnState currentPhase = gameState.getCurrentTurnState();

        // Konvertiere identifierToken → aktuelle Session ID (GameManager braucht aktuelle ID!)
        PlayerInfo player = playerManager.getPlayerByIdentifierToken(identifierToken);
        if (player == null) {
            System.err.println("❌ Player not found for identifierToken: " + identifierToken);
            return;
        }
        String currentSessionId = player.getId();

        if (currentPhase == TurnState.WAITING_FOR_PUSH) {
            executePushPhase(identifierToken, currentSessionId, decision, gameState);
        } else if (currentPhase == TurnState.WAITING_FOR_MOVE) {
            executeMovePhase(identifierToken, currentSessionId, decision, gameState);
        }
    }

    /**
     * Executes PUSH phase of AI decision
     * @param identifierToken Der fixe Token (für AI-Status-Check)
     * @param currentSessionId Die aktuelle Session ID (für GameManager calls)
     */
    private void executePushPhase(String identifierToken, String currentSessionId, AIDecision decision, GameState gameState) throws Exception {
        // Check if player reconnected mid-execution
        if (!isAIActive(identifierToken)) {
            System.out.println("🔄 Player reconnected during PUSH phase, aborting AI execution");
            return;
        }

        // Check if using PUSH_TWICE or PUSH_FIXED bonuses
        if (decision.getUseBonus() == BonusType.PUSH_TWICE) {
            try {
                gameManager.handleUsePushTwice(currentSessionId);
                System.out.println("🎁 AI used PUSH_TWICE bonus");
            } catch (Exception e) {
                System.err.println("⚠️ PUSH_TWICE failed: " + e.getMessage());
            }
        }

        if (decision.getUseBonus() == BonusType.PUSH_FIXED) {
            try {
                gameManager.handleUsePushFixedTile(
                    decision.getPushDirection(),
                    decision.getPushRowOrCol(),
                    currentSessionId
                );
                System.out.println("🎁 AI used PUSH_FIXED bonus");
                // Push Fixed does rotation + push in one call, then move to MOVE phase
                executeMovePhaseAfterPush(identifierToken);
                return;
            } catch (Exception e) {
                System.err.println("⚠️ PUSH_FIXED failed: " + e.getMessage());
            }
        }

        // Execute rotations
        for (int i = 0; i < decision.getRotations(); i++) {
            if (!isAIActive(identifierToken)) return; // Check reconnection
            try {
                gameManager.handleRotateTile(currentSessionId);
                System.out.println("🔄 AI rotated spare tile (" + (i + 1) + "/" + decision.getRotations() + ")");
            } catch (Exception e) {
                System.err.println("⚠️ Rotation failed: " + e.getMessage());
            }
        }

        // Execute push
        try {
            gameManager.handlePushTile(
                decision.getPushRowOrCol(),
                decision.getPushDirection(),
                currentSessionId,
                false // not using push fixed
            );
            System.out.println("⬆️ AI pushed: Index=" + decision.getPushRowOrCol() + ", Dir=" + decision.getPushDirection());
        } catch (PushNotValidException | NotPlayersTurnException e) {
            System.err.println("⚠️ Push failed: " + e.getMessage());
            throw e;
        }

        // After push, wait a bit then execute move
        Thread.sleep(200);
        executeMovePhaseAfterPush(identifierToken);
    }

    /**
     * Executes MOVE phase after completing PUSH phase
     */
    private void executeMovePhaseAfterPush(String identifierToken) throws Exception {
        // Check if player reconnected
        if (!isAIActive(identifierToken)) {
            System.out.println("🔄 Player reconnected during transition, aborting AI execution");
            return;
        }

        // Rebuild GameState (board updated after push)
        GameState updatedState = buildGameStateForPlayer(identifierToken);

        // Get AI instance
        AIServiceSafe ai = aiInstances.get(identifierToken);
        if (ai == null) {
            System.err.println("❌ No AI instance for move phase");
            return;
        }

        // Get move decision for updated board state
        AIDecision moveDecision = ai.getNextMove(updatedState);
        System.out.println("🤖 AI Move Decision: " + moveDecision.getReasoning());

        // Convert identifierToken to currentSessionId for GameManager calls
        PlayerInfo player = playerManager.getPlayerByIdentifierToken(identifierToken);
        if (player == null) {
            System.err.println("❌ Player not found for identifierToken: " + identifierToken);
            return;
        }
        String currentSessionId = player.getId();

        executeMovePhase(identifierToken, currentSessionId, moveDecision, updatedState);
    }

    /**
     * Executes MOVE phase of AI decision
     * @param identifierToken Der fixe Token (für AI-Status-Check)
     * @param currentSessionId Die aktuelle Session ID (für GameManager calls)
     */
    private void executeMovePhase(String identifierToken, String currentSessionId, AIDecision decision, GameState gameState) throws Exception {
        // Check if player reconnected
        if (!isAIActive(identifierToken)) {
            System.out.println("🔄 Player reconnected during MOVE phase, aborting AI execution");
            return;
        }

        // Check if using BEAM or SWAP bonuses
        if (decision.getUseBonus() == BonusType.BEAM) {
            try {
                gameManager.handleUseBeam(
                    decision.getBeamTarget(),
                    currentSessionId
                );
                System.out.println("🎁 AI used BEAM bonus to " + decision.getBeamTarget());
                return; // GameManager handles turn transition
            } catch (Exception e) {
                System.err.println("⚠️ BEAM failed: " + e.getMessage());
            }
        }

        if (decision.getUseBonus() == BonusType.SWAP) {
            try {
                gameManager.handleUseSwap(
                    decision.getSwapTargetPlayerId(),
                    currentSessionId
                );
                System.out.println("🎁 AI used SWAP bonus with player " + decision.getSwapTargetPlayerId());
                return; // GameManager handles turn transition
            } catch (Exception e) {
                System.err.println("⚠️ SWAP failed: " + e.getMessage());
            }
        }

        // Normal move
        try {
            gameManager.handleMovePawn(
                decision.getMoveTarget(),
                currentSessionId,
                false // not using beam
            );
            System.out.println("🚶 AI moved to: (" + decision.getMoveTarget().getColumn() + "," + decision.getMoveTarget().getRow() + ")");

            // GameManager automatically:
            // - Switches to next player
            // - Resets turn state to WAITING_FOR_PUSH
            // - Broadcasts game state
        } catch (Exception e) {
            System.err.println("⚠️ Move failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Fallback: execute random valid move when AI fails
     * @param identifierToken Der fixe Token des Spielers
     */
    private void executeRandomFallback(String identifierToken) {
        System.out.println("🎲 Executing random fallback for player: " + identifierToken);
        try {
            // Convert identifierToken to currentSessionId for GameManager calls
            PlayerInfo playerInfo = playerManager.getPlayerByIdentifierToken(identifierToken);
            if (playerInfo == null) {
                System.err.println("❌ Player not found for identifierToken: " + identifierToken);
                return;
            }
            String currentSessionId = playerInfo.getId();

            // Simple fallback: rotate 0 times, push random valid row/col
            gameManager.handleRotateTile(currentSessionId);
            gameManager.handlePushTile(1, DirectionType.DOWN, currentSessionId, false);

            Thread.sleep(200);

            // Move to current position (stay in place)
            PlayerState player = playerManager.getPlayerStateByIdentifierToken(identifierToken);
            gameManager.handleMovePawn(player.getCurrentPosition(), currentSessionId, false);

        } catch (Exception e) {
            System.err.println("❌ Even fallback failed: " + e.getMessage());
        }
    }
}
