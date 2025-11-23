package com.uni.gamesever.models;

public class PlayerInfo {
    private String id;
    private String name;
    private String color;
    private boolean isAdmin;
    private boolean isReady;
    private String[] availableColors = {"RED", "BLUE", "GREEN", "YELLOW"};

    public PlayerInfo() {}
    public PlayerInfo(String id) {
        this.id = id;
        this.isReady = false;
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
    public boolean getIsReady() {
        return isReady;
    }
    public boolean getIsAdmin() {
        return isAdmin;
    }
    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }
    public int setName(String name) {
        if(name.length() < 1 || name.length() > 50){
            return -1;
        }
        this.name = name;
        return 1;
    }

    public int setColor(String color) {
        for (String availableColor : availableColors) {
            if (availableColor.equalsIgnoreCase(color)) {
                this.color = color.toUpperCase();
                return 1;
            }
        }
        return -1;
    }
    public void setReady(boolean isReady) {
        this.isReady = isReady;
    }
}
