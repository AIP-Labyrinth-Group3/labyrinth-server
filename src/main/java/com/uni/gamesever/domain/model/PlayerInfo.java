package com.uni.gamesever.domain.model;

public class PlayerInfo {
    private String id;
    private String name;
    private String color;
    private boolean isAdmin;
    private boolean isAiControlled;
    private String[] availableColors = { "RED", "BLUE", "GREEN", "YELLOW" };

    public PlayerInfo() {
    }

    public PlayerInfo(String id) {
        this.id = id;
        this.isAiControlled = false;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
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
            throw new IllegalArgumentException("Name must be between 1 and 50 characters.");
        }
        this.name = name;
    }

    public void setColor(String color) throws IllegalArgumentException {
        for (String availableColor : availableColors) {
            if (availableColor.equalsIgnoreCase(color)) {
                this.color = color.toUpperCase();
                return;
            }
        }
        throw new IllegalArgumentException("Invalid color. Available colors are: RED, BLUE, GREEN, YELLOW.");
    }

    public void setIsAiControlled(boolean isAiControlled) {
        this.isAiControlled = isAiControlled;
    }
}
