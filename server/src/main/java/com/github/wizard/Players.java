package com.github.wizard;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import org.tinylog.Logger;

public class Players extends ArrayList<Player> {

    private final Game game;

    public Players(Game game) {
        super(Server.MAX_PLAYERS);
        this.game = game;
    }

    public boolean add(Player player) {
        // TODO: throw error when playerlist is full
        if (size() >= Server.MAX_PLAYERS) return false;

        int playerId = size();

        super.add(player);
        player.playerId = (byte) playerId;
        player.game = game;

        return true;
    }

    /** will ask all players to notify about their points and current round nr */
    public void notifyAboutPointsAndRound(int roundNumber) {
        forEach(p -> p.OnRoundFinished(roundNumber));
    }

    public void updateGAmeBoard(Game.Round round) {
        forEach(p -> p.OnGameBoardUpdate(round));
    }

    public void finishStich(Player winningPlayer, int value) {
        winningPlayer.winStich(value);
        forEach(p -> p.OnStichMade(winningPlayer, value));
    }

    /** politely asks every player for his/her estimates for the upcoming round */
    public void getAllEstimates() {
        forEach(Player::GetEstimate);
    }

    /** politely asks every player for his/her estimates for the upcoming round */
    public void tellAllTrumpfSelected(Color trumpf) {
        forEach(p -> p.OnTrumpfSelected(trumpf));
    }

    public Player getNextPlayer(Player currentPlayer) {
        return super.get((currentPlayer.playerId + 1) % size());
    }

    /** will hand out random cards to all players, according to the round */
    public void handoutCards(int roundNumber, Queue<Card> cardsStack) {
        for (Player p : this) {
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
        return stream().allMatch(Player::isSubscribed);
    }
}
