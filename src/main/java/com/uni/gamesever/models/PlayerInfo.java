package com.uni.gamesever.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class PlayerInfo {
    private String id;
    private String name;
    @JsonIgnore
    private boolean isAdmin;

    public PlayerInfo() {}
    public PlayerInfo(String id) {
        this.id = id;
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

    public int setName(String name) {
        if(name.length() < 1 || name.length() > 50){
            return -1;
        }
        this.name = name;
        return 1;
    }
}
