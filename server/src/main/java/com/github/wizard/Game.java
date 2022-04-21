package com.github.wizard;

import java.util.ArrayList;

public class Game {
    public final int gameId;
    public boolean ready=false;
    private ArrayList<Player> playerArrayList=new ArrayList<>();

    public Game(int i) {
        this.gameId = i;
    }

    public ArrayList<Player> getPlayerArrayList() {
        return playerArrayList;
    }
    public void addPlayer(Player player){
        playerArrayList.add(player);
    }
}
