package com.github.wizard;

public class ClientPlayer {
    private String id;
    private String name;
    private String points;

    public ClientPlayer(String id, String name, String points) {
        this.id = id;
        this.name = name;
        this.points = points;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPoints() {
        return points;
    }

    public void setPoints(String points) {
        this.points = points;
    }
}
