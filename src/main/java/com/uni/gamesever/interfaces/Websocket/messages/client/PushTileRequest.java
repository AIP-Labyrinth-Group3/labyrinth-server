package com.uni.gamesever.interfaces.Websocket.messages.client;

import com.uni.gamesever.domain.enums.DirectionType;

public class PushTileRequest extends Message {
    private int rowOrColIndex;
    private DirectionType direction;

    public PushTileRequest() {
        super("PUSH_TILE");
        this.rowOrColIndex = -1;
        this.direction = null;
    }

    public PushTileRequest(int rowOrColIndex, DirectionType direction) {
        super("PUSH_TILE");
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
        if (rowOrColIndex < 0) {
            throw new IllegalArgumentException("Keine gültige Zeilen- oder Spaltenindex für Push angegeben");
        }
        this.rowOrColIndex = rowOrColIndex;
    }

    public void setDirection(DirectionType direction) {
        if (direction == null) {
            throw new IllegalArgumentException("Keine gültige Richtung für Push angegeben");
        }
        this.direction = direction;
    }
}
