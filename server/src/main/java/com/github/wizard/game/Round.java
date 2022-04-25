package com.github.wizard.game;

import com.github.wizard.Updater;
import java.util.Random;
import org.tinylog.Logger;

public final class Round {

    private final Game game;
    private final Player.Players players;
    private final Stich cardsInTheMiddle;
    private final int number;

    private static final Random random = new Random();

    public static Round create(Game game, int number) {

        Color trump = Color.values()[random.nextInt(4)];

        Round round = new Round(game, new Stich(trump), number);

        return round;
    }

    public void start() {
        players.tellAllTrumpfSelected(getTrumpf());
        players.handoutCards(number, Card.getShuffledDeck());
        players.updateGAmeBoard(cardsInTheMiddle.getCards());
        players.getAllEstimates();
    }

    public Round(Game game, Stich stich, int roundNumber) {
        this.cardsInTheMiddle = stich;
        this.game = game;
        this.players = game.getPlayers();
        this.number = roundNumber;
    }

    protected Color getTrumpf() {
        return getCardsInTheMiddle().trumpf;
    }

    protected Stich getCardsInTheMiddle() {
        return cardsInTheMiddle;
    }

    protected int getNumber() {
        return number;
    }

    public void playCard(Card card, Player player) {
        cardsInTheMiddle.playCard(card, player);

        if (cardsInTheMiddle.getCardsPlayed() == players.size()) {
            Player winner = cardsInTheMiddle.getWinningPlayer();
            players.finishStich(winner, cardsInTheMiddle.getValue()); // notify other players

            winner.update(Updater.newCardPlayRequestResponse()); // request to start next stich

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
