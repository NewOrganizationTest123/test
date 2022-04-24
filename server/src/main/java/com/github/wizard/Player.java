package com.github.wizard;

import com.github.wizard.api.Response;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import org.tinylog.Logger;

public class Player implements GameUpdate {
    String name;
    byte playerId;
    Game game;
    boolean iHaveCHeatedFlag = false; // set to true if I have cheated
    StreamObserver<Response> responseObserver;
    private ArrayList<Card> cards = new ArrayList<>();
    private int points;

    public Player(String name) {
        this.name = name;
    }

    public int carsLeft() {
        return cards.size();
    }

    public Card getCard(int index) {
        return cards.get(index);
    }

    public void addPoints(int value) {
        this.points += value;
    }

    public void subsractPoints(int value) {
        this.points -= value;
    }

    public void giveMeCards(Card[] cards) {
        if (cards.length != game.getRoundNr()) // in round 1 u get 1 card and 2 in round 2 and so on
        throw new IndexOutOfBoundsException(
                    "You gave me too many or to few cards. Current round is "
                            + game.getRoundNr()
                            + " and you gave me "
                            + cards.length
                            + " cards");
        for (Card c : cards) this.cards.add(c);
        StringBuilder cardsString =
                new StringBuilder(); // concatenate the cards on hand firs as following example:
        // |1-RED|2-BLUE.....
        for (Card c : cards) cardsString.append("|").append(c.toString());

        Logger.info("{} has received the following cards: {}", this.name, cardsString.toString());
    }

    /** @return true if subscription is still valid */
    public boolean isSubscribed() {
        return responseObserver != null;
    }

    @Override
    public void OnGameBoardUpdate(GameRound round) {
        Logger.debug(
                "OnGameBoardUpdate called"); // sent back the cards that are in the middle so the
        // player
        // can decide which card to play
        if (responseObserver != null) { // nothing to do if nobody has subscribed for updates
            StringBuilder cardsString =
                    new StringBuilder(); // concatenate the cards on hand firs as following example:
            // |1*RED|2*BLUE.....
            for (Card c : cards) cardsString.append("/").append(c.toString());
            cardsString.append(
                    "//"); // concatenating the cards on the table as follows: //3*YELLOW/4*GREEN...
            for (Card c : game.getCurrentRound().cardsInTheMiddle.cards) {
                if (c == null)
                    break; // if not all cards are on the table yet or there are less than 6 people
                // playing
                cardsString.append(c.toString()).append("/");
            }

            Logger.info("sending out cards: {}", cardsString);

            responseObserver.onNext(
                    Response.newBuilder()
                            .setType("3")
                            .setData(cardsString.toString())
                            .build()); // 3 is request to update game board
        }
    }

    @Override
    public void CardPlayRequest() {
        Logger.debug("CardPlayRequest called");

        if (responseObserver != null) { // nothing to do if nobody has subscribed for updates
            responseObserver.onNext(
                    Response.newBuilder()
                            .setType("2")
                            .setData("Please play a card")
                            .build()); // 2 is request to update game board
        }
    }

    /**
     * The client should then display a popup displaying who made the stich and how much it was
     * worth
     *
     * @param player
     * @param value
     */
    @Override
    public void OnStichMade(Player player, int value) {
        Logger.debug("OnStichMade called");
        if (responseObserver != null) { // nothing to do if nobody has subscribed for updates
            Logger.debug("player name: {}", player.name);
            responseObserver.onNext(
                    Response.newBuilder()
                            .setType("1")
                            .setData(
                                    "Player "
                                            + player.name
                                            + " has made this stich with value "
                                            + value)
                            .build()); // 1 is display who has won
        }
    }

    /**
     * notifies the players that color c was selected as trumpf
     *
     * @param c
     */
    @Override
    public void OnTrumpfSelected(Color c) {
        Logger.debug("OnTrumpSelected called");
        if (responseObserver != null) { // nothing to do if nobody has subscribed for updates
            responseObserver.onNext(
                    Response.newBuilder()
                            .setType("4")
                            .setData(c.name())
                            .build()); // 4 means show them the trumpf
        }
    }

    @Override
    public void GetEstimate() {
        responseObserver.onNext(
                Response.newBuilder().setType("5").build()); // 5 means ask him/her for estimate
    }

    @Override
    public void OnRoundFinished(int round) {
        Logger.debug("OnRoundFinished called");
        responseObserver.onNext(
                Response.newBuilder()
                        .setType("6")
                        .setData(points + "/" + round)
                        .build()); // 5 means tell him/her the points and round nr
    }

    /**
     * removes the card from the player's hand
     *
     * @param c
     */
    public void playCard(Card c) {
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).equals(c)) {
                cards.remove(cards.get(i));
                return;
            }
        }
        throw new IllegalArgumentException("I wanted to play a card I did not have!");
    }

    public ArrayList<Card> getCards() {
        return cards;
    }

    /** needed for test cases */
    public int getPoints() {
        return points;
    }
}
