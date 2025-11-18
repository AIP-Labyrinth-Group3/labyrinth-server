package com.uni.gamesever.models;

public class PlayerState {
    private PlayerInfo player;
    private Coordinates position;
    private Treasure[] treasuresFound;
    private int points;
    private String[] achievements;

    public PlayerState(PlayerInfo player, Coordinates position, Treasure[] treasuresFound, int points, String[] achievements) {
        this.player = player;
        this.position = position;
        this.treasuresFound = treasuresFound;
        this.points = points;
        this.achievements = achievements;
    }
    public Coordinates getPosition() {
        return position;
    }
    public Treasure[] getTreasuresFound() {
        return treasuresFound;
    }
    public int getPoints() {
        return points;
    }
    public String[] getAchievements() {
        return achievements;
    }

    public PlayerInfo getPlayer() {
        return player;
    }

    public void setPosition(Coordinates position) {
        this.position = position;
    }
    public void setTreasuresFound(Treasure[] treasuresFound) {
        this.treasuresFound = treasuresFound;
    }
    public void setPoints(int points) {
        this.points = points;
    }
    public void setAchievements(String[] achievements) {
        this.achievements = achievements;
    }
    public void setPlayer(PlayerInfo player) {
        this.player = player;
    }
}
