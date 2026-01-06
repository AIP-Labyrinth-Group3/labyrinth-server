package com.uni.gamesever.models.messages;

import com.uni.gamesever.models.DirectionType;

public class PushFixedTileCommand extends Message {
    public int rowOrColIndex;
    public DirectionType direction;

    public PushFixedTileCommand() {
        super("USE_PUSHED_FIXED");
        this.rowOrColIndex = -1;
        this.direction = DirectionType.NONE;
    }

    public PushFixedTileCommand(int rowOrColIndex, DirectionType direction) {
        super("USE_PUSHED_FIXED");
        this.rowOrColIndex = rowOrColIndex;
        this.direction = direction;
    }

    public int getRowOrColIndex() {
        return rowOrColIndex;
    }

    public DirectionType getDirection() {
        return direction;
    }

    public void setRowOrColIndex(int rowOrColIndex) {
        this.rowOrColIndex = rowOrColIndex;
    }

    public void setDirection(DirectionType direction) {
        this.direction = direction;
    }
}
