package com.github.wizard.game;

import com.github.wizard.Updater;
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
        players.getAllPlayers(); // return all players for cheating list
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
        nextPlayer.update(Updater.newCardPlayRequestResponse());
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
}
