package com.github.wizard.game;

import com.github.wizard.Server;
import com.github.wizard.Updater;
import com.github.wizard.api.Card;
import com.github.wizard.api.GrpcPlayer;
import com.github.wizard.api.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import org.tinylog.Logger;

public class Player {
    private final String name;
    private byte playerId;
    Game game;
    boolean iHaveCHeatedFlag = false;

    private Updater updater;
    private final ArrayList<Card> cards = new ArrayList<>();
    private Timer timer = new Timer(playerId + "");
    private int points;

    private int estimate = -1;
    private int takenTricks = 0;

    public void unsubscribe() {
        updater.responseStreamObserver().onCompleted();
    }

    public Player(String name) {
        this.name = name;
    }

    public boolean estimateSubmitted() {
        return estimate != -1;
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
        timer.cancel();
        timer = new Timer(playerId + ""); // we need a new timer after cancel();
    }

    public void takeTrick(int value) {
        takenTricks++;
    }

    public int getTakeTrick() {
        return takenTricks;
    }

    public void playCardRequestWithTimeout() {
        updateWithTimeout(
                Updater.newCardPlayRequestResponse(),
                new TimerTask() {
                    public void run() {
                        playRandomCard();
                    }
                },
                Server.GAME_MOVE_TIMEOUT);
    }

    private void playRandomCard() {
        ArrayList<Card> possibleCards = new ArrayList<>();
        // cards.stream().filter(card -> {card.getColor().equals()})

        Random random = new Random();
        if (random.nextInt(15) > 10) { // randomly select if we should cheat
            // cheat
            if (game.getCurrentRound().getCardsInTheMiddle().getCards().size() == 0)
                // we can play any color we want, we cant cheat:(
                possibleCards = cards;
            else {
                for (Card card : cards) {
                    if (!card.getColor()
                            .equals(
                                    game.getCurrentRound()
                                            .getCardsInTheMiddle()
                                            .getCards()
                                            .get(0)
                                            .getColor())) {
                        possibleCards.add(card);
                    }
                }
            }

        } else {
            // do not cheat

            if (game.getCurrentRound().getCardsInTheMiddle().getCards().size() == 0) {
                // we can play any color we want
                possibleCards = cards;
            } else {
                // we can only play cards with the same color
                for (Card card1 : cards) {
                    if (card1.getColor()
                            .equals(
                                    game.getCurrentRound()
                                            .getCardsInTheMiddle()
                                            .getCards()
                                            .get(0)
                                            .getColor())) {
                        possibleCards.add(card1);
                    }
                }

                // wizzards and jesters always allowed
                possibleCards.addAll(
                        cards.stream()
                                .filter(
                                        card ->
                                                card.getColor()
                                                        .equals(
                                                                game.getCurrentRound()
                                                                        .getCardsInTheMiddle()
                                                                        .trump
                                                                        .getColor()))
                                .toList());

                if (possibleCards.size() == 0)
                    possibleCards = cards; // if we do not have a color we can play any card we want
            }
        }
        updater.update(
                Updater.newRandomCardPlayedResponse()); // inform client to disable the card play
        possibleCards.sort(
                (o1, o2) -> {
                    // card is lower if it has lower value or is not trump and the other card is
                    if (((o1.getValue().getNumber() < o2.getValue().getNumber())
                                    || (!o1.getColor()
                                                    .equals(
                                                            game.getCurrentRound()
                                                                    .getTrump()
                                                                    .getColor())
                                            && o2.getColor()
                                                    .equals(
                                                            game.getCurrentRound()
                                                                    .getTrump()
                                                                    .getColor())))
                            && (!(!o2.getColor()
                                            .equals(game.getCurrentRound().getTrump().getColor())
                                    && o1.getColor()
                                            .equals(game.getCurrentRound().getTrump().getColor()))))
                        return -1;
                    else if (o1.getValue().getNumber() > o2.getValue().getNumber()
                            || (!o2.getColor().equals(game.getCurrentRound().getTrump().getColor())
                                    && o1.getColor()
                                            .equals(game.getCurrentRound().getTrump().getColor())))
                        return 1;
                    else // must be equal
                    return 0;
                });
        int indexOfCardToPlay;
        if (estimate - takenTricks
                > 1) // there are still more than one trick left to make-> select highest card
        indexOfCardToPlay = possibleCards.size() - 1;
        else if (estimate - takenTricks == 1) // if there is only one trick left randomly select it
        indexOfCardToPlay = random.nextInt(possibleCards.size());
        else
            indexOfCardToPlay =
                    0; // more tricks made than estimated or perfectly right just now ->select
        // lowest card

        timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        playCard(indexOfCardToPlay);
                    }
                },
                100);

        // updater.update(Updater.newOnGameBoardUpdate(cards,game.getCurrentRound().getCardsInTheMiddle().getCards()));
        Logger.info("Playing random card for " + getName());
    }

    public void clearCardPlayTimeout() {
        timer.cancel();
        timer = new Timer();
    }

    public void updatePoints() {
        Logger.debug("estimate for player {}: {}", playerId, estimate);
        Logger.debug("taken tricks for player {}: {}", playerId, takenTricks);

        if (estimate == -1) {
            takenTricks = 0;
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
        clearCardPlayTimeout();
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

    public void updateWithTimeout(Response response, TimerTask timerTask, long timeout) {
        update(response);
        timer.schedule(
                timerTask,
                timeout
                        + playerId * 10
                        + 5); // to avoid race conditions if multiple players forget to play and
        // account for round-trip time
    }

    /** Will create some number according to some criterion that might be the estimate */
    public void makeRandomEstimate() {
        int estimate = 0;
        // count High cards
        for (Card c : cards) {
            if (c.getValue().getNumber()
                    > 10) { // we should be able to make a stich with anything greater 8, this
                // includes wizzards
                estimate++;
                continue;
            }
            if (game.getCurrentRound().getTrump().getColor().equals(c.getColor())
                    && c.getValue().getNumber() > 7) { // should also be able to win with a trumpf
                estimate++;
                continue;
            }
        }
        Random rand = new Random();
        estimate +=
                rand.nextInt(
                        game.getRoundNr() - estimate - 1 > 0
                                ? game.getRoundNr() - estimate - 1
                                : 1); // add some random amount to make it interesting

        this.estimate = estimate;

        updater.update(Updater.newRandomEstimateCalcuatedResponse(estimate + ""));
        Logger.info("Estimate for " + name + " was chosen randomly");
        if (game.allEstimatesSubmitted()) game.playFirstCard(); // continue the gameplay
    }

    public static class Players extends ArrayList<Player> {

        private final Game game;

        public Players(Game game) {
            super(Server.MAX_PLAYERS);
            this.game = game;
        }

        public boolean add(Player player) {
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
            // forEach(p -> p.update(Updater.newOnRoundFinishedResponse(p.getPoints(),
            // roundNumber)));
            forEach(
                    p ->
                            p.update(
                                    Updater.newOnRoundFinishedResponse(
                                            getGrpcPlayerList(), roundNumber)));
        }

        public void updateGAmeBoard(List<Card> tableCards) {
            Round round = game.getCurrentRound();
            forEach(p -> p.update(Updater.newOnGameBoardUpdate(p.getCards(), tableCards)));
        }

        public void finishTrick(Player winningPlayer, int value) {
            winningPlayer.takeTrick(value);
            forEach(p -> p.update(Updater.newOnTrickTakenResponse(winningPlayer)));
        }

        /**
         * politely asks every player for his/her estimates for the upcoming round and resets
         * existing estimates
         */
        public void getAllEstimates() {
            forEach(p -> p.estimate = -1); // reset all estimates
            forEach(
                    p ->
                            p.updateWithTimeout(
                                    Updater.newGetEstimateResponse(),
                                    new TimerTask() {
                                        public void run() {
                                            p.makeRandomEstimate();
                                        }
                                    },
                                    Server.GAME_MOVE_TIMEOUT));
        }

        public void getAllPlayers() {
            forEach(p -> p.update(Updater.newGetPlayersResponse(this)));
        }

        /** politely asks every player for his/her estimates for the upcoming round */
        public void tellAllTrumpSelected(Card trump) {
            forEach(p -> p.update(Updater.newOnTrumpSelectedResponse(trump)));
        }

        public void tellAllEndGame() {
            forEach(p -> p.update(Updater.newEndGameResponse()));
        }

        /** This will close all connections to the players. No more calls are allowed after this! */
        public void unsubscribeAllPlayers() {
            forEach(p -> p.unsubscribe());
        }

        public Player getNextPlayer(Player currentPlayer) {
            return super.get((currentPlayer.playerId + 1) % size());
        }

        public ArrayList<GrpcPlayer> getGrpcPlayerList() {
            ArrayList<GrpcPlayer> playerArrayList = new ArrayList<>();
            for (Player p : game.players) {
                playerArrayList.add(
                        GrpcPlayer.newBuilder()
                                .setPlayerId(p.getPlayerId() + "")
                                .setPlayerName(p.getName())
                                .setPoints(p.getPoints() + "")
                                .build());
            }
            return playerArrayList;
        }

        public void onCHeatingDiscovered(Player cheater) {
            forEach(
                    p -> {
                        p.update(
                                Updater.newOnCheatingSubmittedResponse(
                                        cheater, cheater.iHaveCHeatedFlag, getGrpcPlayerList()));
                    });
        }
        /** will hand out random cards to all players, according to the round */
        public void handoutCards(int roundNumber, Deck deck) {
            forEach(player -> player.giveMeCards(deck.draw(roundNumber)));
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
