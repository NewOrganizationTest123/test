package com.github.wizard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import org.tinylog.Logger;

public class Players implements Iterable<Player> {

    private final List<Player> players = new ArrayList<>(Server.MAX_PLAYERS);

    public void add(Player player, Game game) {
        // TODO: throw error when playerlist is full
        if (players.size() >= Server.MAX_PLAYERS) return;

        int playerId = players.size();

        players.add(player);
        player.playerId = (byte) playerId;
        player.game = game;
    }

    public int size() {
        return players.size();
    }

    /** will ask all players to notify about their points and current round nr */
    public void notifyAboutPointsAndRound(int roundNumber) {
        players.forEach(p -> p.OnRoundFinished(roundNumber));
    }

    public void updateGAmeBoard(Game.Round round) {
        players.forEach(p -> p.OnGameBoardUpdate(round));
    }

    public void finishStich(Player winningPlayer, int value) {
        winningPlayer.winStich(value);
        players.forEach(p -> p.OnStichMade(winningPlayer, value));
    }

    /** politely asks every player for his/her estimates for the upcoming round */
    public void getAllEstimates() {
        for (Player player : players) {
            player.GetEstimate();
        }
    }

    /** politely asks every player for his/her estimates for the upcoming round */
    public void tellAllTrumpfSelected(Color trumpf) {
        for (Player player : players) {
            player.OnTrumpfSelected(trumpf);
        }
    }

    public Player getNextPlayer(Player currentPlayer) {
        return players.get((currentPlayer.playerId + 1) % players.size());
    }

    /** will hand out random cards to all players, according to the round */
    public void handoutCards(int roundNumber, Queue<Card> cardsStack) {
        for (Player p : players) {
            List<Card> cards = new ArrayList<>(roundNumber);

            for (int i = 0; i < roundNumber; i++) {
                cards.add(cardsStack.poll());
            }

            Logger.debug("cards for player {}: {}", p.playerId, cards);

            p.giveMeCards(cards);
        }
    }

    /**
     * the host is the last to join. Always
     *
     * @return true if everyone is ready. That means host has clicked next button
     */
    public boolean areSubscribed() {
        for (Player player : players) {
            if (!player.isSubscribed()) return false;
        }
        return true;
    }

    public Player get(int id) {
        return players.get(id);
    }

    @Override
    public Iterator<Player> iterator() {
        return players.iterator();
    }
}
