package com.uni.gamesever.interfaces.Websocket.messages.client;

import com.uni.gamesever.domain.enums.DirectionType;

public class PushFixedTileRequest extends Message {
    public int rowOrColIndex;
    public DirectionType direction;

    public PushFixedTileRequest() {
        super("USE_PUSHED_FIXED");
        this.rowOrColIndex = -1;
        this.direction = DirectionType.NONE;
    }

    public PushFixedTileRequest(int rowOrColIndex, DirectionType direction) {
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
