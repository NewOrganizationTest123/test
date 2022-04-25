package com.github.wizard;

import com.github.wizard.api.Response;
import java.util.ArrayList;
import java.util.List;
import org.tinylog.Logger;

public class Player {
    String name;
    byte playerId;
    Game game;
    boolean iHaveCHeatedFlag = false; // TODO: implement cheating, set to true if I have cheated

    Updater updater;
    private final ArrayList<Card> cards = new ArrayList<>();
    private int points;

    private int estimate = -1;
    private int wonStiche = 0;
    private int stichValue = 0;

    public Player(String name) {
        this.name = name;
    }

    public int cardsLeft() {
        return cards.size();
    }

    public void addPoints(int value) {
        this.points += value;
    }

    public void subtractPoints(int value) {
        this.points -= value;
    }

    public void makeEstimate(int estimate) {
        this.estimate = estimate;
    }

    public void winStich(int value) {
        wonStiche++;
        stichValue += value;
    }

    public void updatePoints() {
        Logger.debug("estimate for player {}: {}", playerId, estimate);
        Logger.debug("wonStiche for player {}: {}", playerId, wonStiche);

        if (estimate == -1) return;
        int roundPoints;
        if (estimate == wonStiche) {
            roundPoints = 20 + wonStiche * 10;
        } else {
            roundPoints = (wonStiche - estimate) * 10;
        }
        addPoints(roundPoints);
        Logger.debug("roundPoints for player {}: {}", playerId, roundPoints);

        estimate = -1;
        wonStiche = 0;
        stichValue = 0;
    }

    public void giveMeCards(List<Card> newCards) {
        Logger.debug("giving {} to player {}", newCards, playerId);
        if (newCards.size()
                != game.getRoundNr()) { // in round 1 u get 1 card and 2 in round 2 and so on
            throw new IndexOutOfBoundsException(
                    "You gave me too many or to few cards. Current round is "
                            + game.getRoundNr()
                            + " and you gave me "
                            + newCards.size()
                            + " cards");
        }

        cards.clear();
        cards.addAll(newCards);

        StringBuilder cardsString =
                new StringBuilder(); // concatenate the cards on hand firs as following example:
        // |1-RED|2-BLUE.....
        for (Card c : cards) cardsString.append("|").append(c.toString());

        Logger.info("{} has received the following cards: {}", this.name, cardsString.toString());
    }

    /** @return true if subscription is still valid */
    public boolean isSubscribed() {
        return updater != null;
    }

    /**
     * removes the card from the player's hand
     *
     * @param index
     */
    public void playCard(int index) {
        Card card = cards.get(index);

        if (!cards.remove(card))
            throw new IllegalArgumentException("I wanted to play a card I did not have!");

        game.playCard(card, this);
    }

    public ArrayList<Card> getCards() {
        return cards;
    }

    /** needed for test cases */
    public int getPoints() {
        return points;
    }

    public void update(Response response) {
        Logger.debug(response);
        updater.update(response);
    }
}
