package com.uni.gamesever.interfaces.Websocket.messages.client;

public class RotateTileRequest extends Message {

    public RotateTileRequest() {
        super("ROTATE_TILE");
    }

    public void rotateTile() {
        System.out.println("Tile rotated");
    }

}