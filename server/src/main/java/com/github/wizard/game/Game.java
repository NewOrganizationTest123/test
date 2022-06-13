package com.github.wizard.game;

import com.github.wizard.Server;
import com.github.wizard.api.Card;
import org.tinylog.Logger;

public class Game {
    private Player nextPlayer = null;
    public final int gameId;
    public boolean ready = false;

    public final Player.Players players = new Player.Players(this);
    public final Deck deck = new Deck();

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

    public Player getNextPlayer() {
        return nextPlayer;
    }

    public void setNextPlayer(Player nextPlayer) {
        this.nextPlayer = nextPlayer;
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
        players.getAllPlayers(); // return all players for cheating list and points
    }

    public void proceed() {
        currentRound.start();
    }
    /**
     * @return true if all players in the game have submitted their estimates, false if we are still
     *     waiting for some
     */
    public boolean allEstimatesSubmitted() {
        for (Player p : players) if (!p.estimateSubmitted()) return false;
        return true;
    }

    public void playFirstCard() {
        if (nextPlayer == null)
            nextPlayer =
                    players.getNextPlayer(players.get(0)); // the host shall start the first round
        nextPlayer.playCardRequestWithTimeout();
        Logger.info(
                "asking player {} to play the first card after all estimates were received",
                nextPlayer.getPlayerId());
    }

    public void cheatDiscoverySubmitted(Player cheater, Player petze) {
        if (cheater.iHaveCHeatedFlag) {
            // if there really was a cheating
            petze.addPoints(30);
            cheater.subtractPoints(10);
            cheater.iHaveCHeatedFlag = false;
        } else { // if there was no cheating
            petze.subtractPoints(10);
            petze.iHaveCHeatedFlag = true; // this counts as cheating :D
        }
        players.onCHeatingDiscovered(cheater);
    }

    public void endGame() {
        players.tellAllEndGame(); // this will cause the players to show the final screen
        players.unsubscribeAllPlayers(); // this will close all connections.
        Server.removeGame(
                this); // remove the reference from server so garbage collector can clean us up
        Logger.info("game " + gameId + " has been ended successfully");
    }
}
