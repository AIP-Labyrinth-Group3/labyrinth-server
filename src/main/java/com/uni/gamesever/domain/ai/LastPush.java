package com.uni.gamesever.domain.ai;

import com.uni.gamesever.domain.enums.DirectionType;

/**
 * Wrapper for last push information
 * Used by AI to avoid push-back
 */
public class LastPush {
    private int rowOrColumnIndex;
    private DirectionType direction;

    public LastPush(int rowOrColumnIndex, DirectionType direction) {
        this.rowOrColumnIndex = rowOrColumnIndex;
        this.direction = direction;
    }

    public int getRowOrColumnIndex() {
        return rowOrColumnIndex;
    }

    public void setRowOrColumnIndex(int rowOrColumnIndex) {
        this.rowOrColumnIndex = rowOrColumnIndex;
    }

    public DirectionType getDirection() {
        return direction;
    }

    public void setDirection(DirectionType direction) {
        this.direction = direction;
    }
}
