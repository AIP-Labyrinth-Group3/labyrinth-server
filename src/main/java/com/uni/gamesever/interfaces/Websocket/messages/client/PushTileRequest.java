package com.uni.gamesever.interfaces.Websocket.messages.client;

import java.util.List;

import com.uni.gamesever.domain.enums.DirectionType;

public class PushTileRequest extends Message {
    private int rowOrColIndex;
    private DirectionType direction;
    private List<String> entrances;

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

    public List<String> getEntrances() {
        return entrances;
    }

    public void setRowOrColIndex(int rowOrColIndex) {
        if (rowOrColIndex < 0) {
            throw new IllegalArgumentException("No valid row or column index provided for push");
        }
        this.rowOrColIndex = rowOrColIndex;
    }

    public void setEntrances(List<String> entrances) {
        // iterate through entrances and validate
        for (String entrance : entrances) {
            try {
                DirectionType.valueOf(entrance);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Invalid direction: " + entrance + ". Valid directions are: UP, DOWN, LEFT, RIGHT");
            }
        }
        this.entrances = entrances;
    }
}
