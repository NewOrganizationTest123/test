package com.github.wizard;

import java.util.Random;
import org.tinylog.Logger;

public class Game {
    public final int gameId;
    public boolean ready = false;

    private final Players players = new Players(this);

    private Round currentRound;

    private static final Random random = new Random();

    public Game(int i) {
        this.gameId = i;
    }

    public Players getPlayers() {
        return players;
    }

    /**
     * adds a player to the current game. This does not include the subscription for updates
     *
     * @param player
     * @return the playerid of whoever was added
     */
    public int addPlayer(Player player) {
        players.add(player);
        return player.playerId;
    }

    public Round getCurrentRound() {
        return currentRound;
    }

    public int getRoundNr() {
        return currentRound.number;
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
    }

    public void proceed() {
        currentRound.start();
    }

    public static final class Round {

        private final Game game;
        private final Players players;
        private final Stich cardsInTheMiddle;

        public final int number;

        public static Round create(Game game, int number) {

            Color trump = Color.values()[random.nextInt(4)];

            Round round = new Round(game, new Stich(trump), number);

            return round;
        }

        public void start() {
            players.tellAllTrumpfSelected(getTrumpf());
            players.handoutCards(number, Card.getShuffledDeck());
            players.updateGAmeBoard();
            players.getAllEstimates();
        }

        public Round(Game game, Stich stich, int roundNumber) {
            this.cardsInTheMiddle = stich;
            this.game = game;
            this.players = game.players;
            this.number = roundNumber;
        }

        public Color getTrumpf() {
            return getCardsInTheMiddle().trumpf;
        }

        public Stich getCardsInTheMiddle() {
            return cardsInTheMiddle;
        }

        public void playCard(Card card, Player player) {
            cardsInTheMiddle.playCard(card, player);

            if (cardsInTheMiddle.getCardsPlayed() == players.size()) {
                Player winner = cardsInTheMiddle.getWinningPlayer();
                players.finishStich(winner, cardsInTheMiddle.getValue()); // notify other players
                winner.CardPlayRequest(); // request to start next stich

                if (winner.cardsLeft() == 0) {
                    // players.updatePoints();
                    players.notifyAboutPointsAndRound(number);

                    // TODO: quit game if it was the last round
                    game.currentRound = Round.create(game, number + 1);
                    game.proceed();
                } else {
                    cardsInTheMiddle.reset();
                }
            } else {
                Player nextPlayer = players.getNextPlayer(player);
                nextPlayer.CardPlayRequest();
                Logger.info("asking player {} to play", nextPlayer.playerId);
            }

            players.updateGAmeBoard();
        }
    }
}
