package com.github.wizard.game;

public class Game {
    public final int gameId;
    public boolean ready = false;

    private final Player.Players players = new Player.Players(this);

    private Round currentRound;

    public Game(int i) {
        this.gameId = i;
    }

    public Player.Players getPlayers() {
        return players;
    }

    /**
     * adds a player to the current game. This does not include the subscription for updates
     *
     * @param player
     * @return the playerId of whoever was added
     */
    public int addPlayer(Player player) {
        players.add(player);
        return player.getPlayerId();
    }

    protected void setCurrentRound(Round round) {
        this.currentRound = round;
    }

    public Round getCurrentRound() {
        return currentRound;
    }

    public int getRoundNr() {
        return currentRound.getNumber();
    }

    public int getNrPlayers() {
        return players.size();
    }

    public void playCard(Card card, Player player) {
        currentRound.playCard(card, player);
    }

    public boolean allPlayersSubscribed() {
        return players.areSubscribed();
    }

    public void start() {
        currentRound = Round.create(this, 1);
        proceed();
    }

    public void proceed() {
        currentRound.start();
    }
}
