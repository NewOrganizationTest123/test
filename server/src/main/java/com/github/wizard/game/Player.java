package com.github.wizard.game;

import com.github.wizard.Server;
import com.github.wizard.Updater;
import com.github.wizard.api.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import org.tinylog.Logger;

public class Player {
    private final String name;
    private byte playerId;
    Game game;
    boolean iHaveCHeatedFlag = false; // TODO: implement cheating, set to true if I have cheated

    private Updater updater;
    private final ArrayList<Card> cards = new ArrayList<>();
    private int points;

    private int estimate = -1;
    private int takenTricks = 0;
    private int trickValue = 0;

    public Player(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public byte getPlayerId() {
        return playerId;
    }

    public void setUpdater(Updater updater) {
        this.updater = updater;
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

    public void takeTrick(int value) {
        takenTricks++;
        trickValue += value;
    }

    public void updatePoints() {
        Logger.debug("estimate for player {}: {}", playerId, estimate);
        Logger.debug("taken tricks for player {}: {}", playerId, takenTricks);

        if (estimate == -1) {
            takenTricks = 0;
            trickValue = 0;
            return;
        }

        int roundPoints;
        if (estimate == takenTricks) {
            roundPoints = 20 + takenTricks * 10;
        } else {
            roundPoints = (takenTricks - estimate) * 10;
        }
        addPoints(roundPoints);
        Logger.debug("roundPoints for player {}: {}", playerId, roundPoints);

        estimate = -1;
        takenTricks = 0;
        trickValue = 0;
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
        try {
            Card card = cards.remove(index);
            game.playCard(card, this);
        } catch (IndexOutOfBoundsException i) {
            throw new IllegalArgumentException("I wanted to play a card I did not have!");
        }
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

    public static class Players extends ArrayList<Player> {

        private final Game game;

        public Players(Game game) {
            super(Server.MAX_PLAYERS);
            this.game = game;
        }

        public boolean add(Player player) {
            // TODO: throw error when player list is full
            if (size() >= Server.MAX_PLAYERS) return false;

            int playerId = size();

            super.add(player);
            player.playerId = (byte) playerId;
            player.game = game;

            return true;
        }

        /** will ask all players to notify about their points and current round nr */
        public void notifyAboutPointsAndRound(int roundNumber) {
            forEach(Player::updatePoints);
            forEach(p -> p.update(Updater.newOnRoundFinishedResponse(p.getPoints(), roundNumber)));
        }

        public void updateGAmeBoard(List<Card> tableCards) {
            Round round = game.getCurrentRound();
            forEach(p -> p.update(Updater.newOnGameBoardUpdate(p.getCards(), tableCards)));
        }

        public void finishTrick(Player winningPlayer, int value) {
            winningPlayer.takeTrick(value);
            forEach(p -> p.update(Updater.newOnTrickTakenResponse(winningPlayer, value)));
        }

        /** politely asks every player for his/her estimates for the upcoming round */
        public void getAllEstimates() {
            forEach(p -> p.update(Updater.newGetEstimateResponse()));
        }

        /** politely asks every player for his/her estimates for the upcoming round */
        public void tellAllTrumpSelected(Color trump) {
            forEach(p -> p.update(Updater.newOnTrumpSelectedResponse(trump)));
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
}
