package com.uni.gamesever.models.messages;

import java.util.List;

import com.uni.gamesever.models.DirectionType;

public class PushTileCommand extends Message{
    private int rowOrColIndex;
    private String direction;
    private List<String> entrances;

    public PushTileCommand() {}
    public PushTileCommand(int rowOrColIndex, String direction)
    {
        super("PUSH_TILE");
        this.rowOrColIndex = rowOrColIndex;
        this.direction = direction;
    }

    public int getRowOrColIndex() {
        return rowOrColIndex;
    }
    public String getDirection() {
        return direction;
    }
    public List<String> getEntrances() {
        return entrances;
    }

    public void setEntrances(List<String> entrances) {
        //iterate through entrances and validate
        for(String entrance : entrances) {
            try {
                DirectionType.valueOf(entrance);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid direction: " + entrance + ". Valid directions are: UP, DOWN, LEFT, RIGHT");
            }
        }
        this.entrances = entrances;
    }
}
