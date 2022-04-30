package com.github.wizard.game;

import com.github.wizard.Updater;
import java.util.Random;
import org.tinylog.Logger;

public final class Round {

    private final Game game;
    private final Player.Players players;
    private final Trick cardsInTheMiddle;
    private final int number;

    private static final Random random = new Random();

    public static Round create(Game game, int number) {

        Color trump = Color.values()[random.nextInt(4)];

        return new Round(game, new Trick(trump), number);
    }

    public void start() {
        players.tellAllTrumpSelected(getTrump());
        players.handoutCards(number, Card.getShuffledDeck());
        players.updateGAmeBoard(cardsInTheMiddle.getCards());
        players.getAllEstimates();
    }

    public Round(Game game, Trick trick, int roundNumber) {
        this.cardsInTheMiddle = trick;
        this.game = game;
        this.players = game.getPlayers();
        this.number = roundNumber;
    }

    protected Color getTrump() {
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
            players.finishTrick(winner, cardsInTheMiddle.getValue()); // notify other players

            winner.update(Updater.newCardPlayRequestResponse()); // request to start next trick

            if (winner.cardsLeft() == 0) {
                players.notifyAboutPointsAndRound(number);

                // TODO: quit game if it was the last round
                game.setCurrentRound(Round.create(game, number + 1));
                game.proceed();
                return;
            } else {
                cardsInTheMiddle.reset();
            }
        } else {
            Player nextPlayer = players.getNextPlayer(player);
            nextPlayer.update(Updater.newCardPlayRequestResponse());
            Logger.info("asking player {} to play", nextPlayer.getPlayerId());
        }

        players.updateGAmeBoard(cardsInTheMiddle.getCards());
    }
}
