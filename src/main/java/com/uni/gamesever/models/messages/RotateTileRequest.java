package com.uni.gamesever.models.messages;

public class RotateTileRequest extends Message {


    public RotateTileRequest() {
        super("ROTATE_TILE");
    }



    public void rotateTile() {
        System.out.println("Tile rotated");
    }

}