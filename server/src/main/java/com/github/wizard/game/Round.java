package com.github.wizard.game;

import com.github.wizard.api.Card;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import org.tinylog.Logger;

public final class Round {

    private final Game game;
    private final Player.Players players;
    private final Trick cardsInTheMiddle;
    private final int number;

    private static final Random random = new Random();

    public static Round create(Game game, int number) {

        Card trump = game.deck.draw();

        return new Round(game, new Trick(trump), number);
    }

    public void start() {
        game.deck.shuffle();

        // reset cheating-Flags for all players
        for (Player player : players) {
            player.iHaveCHeatedFlag = false;
        }

        players.handoutCards(number, game.deck);

        players.tellAllTrumpSelected(cardsInTheMiddle.trump);

        players.updateGAmeBoard(cardsInTheMiddle.getCards(), "");
        players.getAllEstimates();
    }

    public Round(Game game, Trick trick, int roundNumber) {
        this.cardsInTheMiddle = trick;
        this.game = game;
        this.players = game.getPlayers();
        this.number = roundNumber;
    }

    protected Card getTrump() {
        return getCardsInTheMiddle().trump;
    }

    protected Trick getCardsInTheMiddle() {
        return cardsInTheMiddle;
    }

    protected int getNumber() {
        return number;
    }

    public void playCard(Card card, Player player) {
        cardsInTheMiddle.playCard(card, player);

        if (cardsInTheMiddle.getCardsPlayed() == players.size()) {
            Player winner = cardsInTheMiddle.getWinningPlayer();
            players.updateGAmeBoard(
                    cardsInTheMiddle.getCards(), ""); // round finished, no next player

            new Timer()
                    .schedule(
                            new TimerTask() {
                                @Override
                                public void run() {

                                    players.finishTrick(winner); // notify other players
                                    if (winner.cardsLeft() == 0) {
                                        players.notifyAboutPointsAndRound(number);

                                        // quit game if it was the last round
                                        if (players.size() * (game.getRoundNr() + 1)
                                                >= game.deck.getCardsAvailable()) {
                                            game.endGame();
                                            return;
                                        }

                                        game.setCurrentRound(Round.create(game, number + 1));
                                        game.setNextPlayer(winner);
                                        game.proceed();
                                        return;
                                    } else {
                                        cardsInTheMiddle.reset();
                                        players.updateGAmeBoard(cardsInTheMiddle.getCards(), "");
                                        winner.playCardRequestWithTimeout(); // request to start
                                        // next trick
                                    }
                                }
                            },
                            3000);
        } else {
            Player nextPlayer = players.getNextPlayer(player);
            players.updateGAmeBoard(cardsInTheMiddle.getCards(), nextPlayer.getName());
            nextPlayer.playCardRequestWithTimeout();
            Logger.info("asking player {} to play", nextPlayer.getPlayerId());
        }
    }
}
