package com.uni.gamesever.domain.ai;

import com.uni.gamesever.domain.enums.BonusType;
import com.uni.gamesever.domain.enums.DirectionType;
import com.uni.gamesever.domain.model.Coordinates;

public class AIDecision {
    private int rotations;
    private int pushRowOrCol;
    private DirectionType pushDirection;
    private Coordinates moveTarget;
    private String reasoning;

    // Bonus-Felder
    private BonusType useBonus;          // Welcher Bonus soll genutzt werden (null = keiner)
    private Coordinates beamTarget;       // Ziel für BEAM
    private String swapTargetPlayerId;    // Ziel-Spieler für SWAP
    private boolean isGoingHome;          // True wenn AI zum Startfeld navigiert

    // Getters und Setters
    public int getRotations() {
        return rotations;
    }

    public void setRotations(int rotations) {
        this.rotations = rotations;
    }

    public int getPushRowOrCol() {
        return pushRowOrCol;
    }

    public void setPushRowOrCol(int pushRowOrCol) {
        this.pushRowOrCol = pushRowOrCol;
    }

    public DirectionType getPushDirection() {
        return pushDirection;
    }

    public void setPushDirection(DirectionType direction) {
        this.pushDirection = direction;
    }

    public Coordinates getMoveTarget() {
        return moveTarget;
    }

    public void setMoveTarget(Coordinates target) {
        this.moveTarget = target;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    // Bonus Getter und Setter
    public BonusType getUseBonus() {
        return useBonus;
    }

    public void setUseBonus(BonusType useBonus) {
        this.useBonus = useBonus;
    }

    public Coordinates getBeamTarget() {
        return beamTarget;
    }

    public void setBeamTarget(Coordinates beamTarget) {
        this.beamTarget = beamTarget;
    }

    public String getSwapTargetPlayerId() {
        return swapTargetPlayerId;
    }

    public void setSwapTargetPlayerId(String swapTargetPlayerId) {
        this.swapTargetPlayerId = swapTargetPlayerId;
    }

    public boolean isGoingHome() {
        return isGoingHome;
    }

    public void setGoingHome(boolean goingHome) {
        isGoingHome = goingHome;
    }
}
