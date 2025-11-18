package com.uni.gamesever.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class PlayerInfo {
    private String id;
    private String name;
    @JsonIgnore
    private boolean isAdmin;

    public PlayerInfo() {}
    public PlayerInfo(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    
    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public void setName(String name) {
        this.name = name;
    }
}
