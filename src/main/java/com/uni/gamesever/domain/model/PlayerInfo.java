package com.uni.gamesever.domain.model;

import com.uni.gamesever.domain.enums.Color;

public class PlayerInfo {
    private String id;
    private String name;
    private Color color;
    private boolean isAdmin;
    private boolean isAiControlled;
    private boolean isConnected;

    public PlayerInfo() {
    }

    public PlayerInfo(String id) {
        this.id = id;
        this.isAiControlled = false;
        this.isConnected = true;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Color getColor() {
        return color;
    }

    public boolean getIsAiControlled() {
        return isAiControlled;
    }

    public boolean getIsAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public void setName(String name) throws IllegalArgumentException {
        if (name.length() < 1 || name.length() > 50) {
            throw new IllegalArgumentException("Der Benutzername muss zwischen 1 und 50 Zeichen lang sein.");
        }
        this.name = name;
    }

    public void setColor(Color color) throws IllegalArgumentException {
        if (color == null) {
            throw new IllegalArgumentException("Ungültige Farbe.");
        }
        this.color = color;
    }

    public void setIsAiControlled(boolean isAiControlled) {
        this.isAiControlled = isAiControlled;
    }

    public boolean getIsConnected() {
        return isConnected;
    }

    public void setIsConnected(boolean isConnected) {
        this.isConnected = isConnected;
    }
}
