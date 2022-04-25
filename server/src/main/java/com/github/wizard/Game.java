package com.github.wizard;

import static java.lang.Math.abs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import org.tinylog.Logger;

public class Game {
    public final int gameId;
    public boolean ready = false;

    private final ArrayList<Player> players = new ArrayList<>(Server.MAX_PLAYERS);

    private ArrayList<Round> rounds = new ArrayList<>();
    private final Queue<Card> cardsStack = new LinkedList<>();

    private static final Random random = new Random();

    public Game(int i) {
        this.gameId = i;
    }

    public ArrayList<Player> getPlayers() {
        return players;
    }

    /** creates a new card stack, consisting of all available cards */
    protected void initializeCardStack() {
        cardsStack.clear();

        List<Card> deckCopy = new ArrayList<>(Card.deck);
        Collections.shuffle(deckCopy);

        cardsStack.addAll(deckCopy);
    }

    /**
     * adds a player to the current game. This does not include the subscription for updates
     *
     * @param player
     * @return the playerid of whoever was added
     */
    public int addPlayer(Player player) {
        int numberOfPlayers = getNrPlayers();

        // TODO: throw error when playerlist is full
        if (numberOfPlayers >= Server.MAX_PLAYERS) return numberOfPlayers;

        players.add(player);
        player.playerId = (byte) numberOfPlayers;
        player.game = this;

        return player.playerId;
    }

    public void startNewRound() {
        switch (random.nextInt(4)) { // choose Trumpf randomly
            case 0:
                rounds.add(new Round(Color.RED));
                break;
            case 1:
                rounds.add(new Round(Color.BLUE));
                break;
            case 2:
                rounds.add(new Round(Color.GREEN));
                break;
            case 3:
                rounds.add(new Round(Color.YELLOW));
                break;
            default:
                throw new IndexOutOfBoundsException("no more than 4 colors available");
        }
        tellAllTrumpfSelected();
        initializeCardStack(); // collect all cards from previous round
        handoutCards(); // handout cards
        updateGAmeBoard(); // notify all players that they got their cards and wake em up
        getAllEstimates(); // take in estimates from all players
    }

    /** will hand out random cards to all players, according to the round */
    private void handoutCards() {
        for (Player p : players) {
            List<Card> cards = new ArrayList<>(getRoundNr());

            for (int i = 0; i < getRoundNr(); i++) {
                cards.add(cardsStack.poll());
            }

            p.giveMeCards(cards);
        }
    }

    public Round getCurrentRound() {
        return rounds.get(rounds.size() - 1);
    }

    public int getRoundNr() {
        return rounds.size();
    }

    /**
     * the host is the last to join. Always
     *
     * @return true if everyone is ready. That means host has clicked next button
     */
    public boolean allPlayersSubscribed() {
        for (Player player : players) {
            if (!player.isSubscribed()) return false;
        }
        return true;
    }

    public int getNrPlayers() {
        return players.size();
    }

    private Player getNextPlayer(Player currentPlayer) {
        return players.get((currentPlayer.playerId + 1) % getNrPlayers());
    }

    /** calculates the points for each player in respect to his/her estimate */
    private void countPointsForThisRound() {
        for (Player p : players) {
            if (getCurrentRound().estimates[p.playerId]
                    == getCurrentRound()
                            .wonStiche[p.playerId]) { // the player correctly estimated his stiche
                p.addPoints(
                        20
                                + getCurrentRound().wonStiche[p.playerId]
                                        * 10); // 20 points for correct estimate+10 points for each
                // stich
            } else { // he/she did not correctly estimate
                p.subtractPoints(
                        abs(
                                        getCurrentRound().estimates[p.playerId]
                                                - getCurrentRound().wonStiche[p.playerId])
                                * 10); // he/she will lose 10 points for every stich above or below
                // his/her estimate
            }
        }
    }

    /** will ask all players to notify about their points and current round nr */
    private void notifyAboutPointsAndRound() {
        int roundNumber = getRoundNr();
        players.forEach(p -> p.OnRoundFinished(roundNumber));
    }

    private void updateGAmeBoard() {
        Round currentRound = getCurrentRound();
        players.forEach(p -> p.OnGameBoardUpdate(currentRound));
    }

    /** politely asks every player for his/her estimates for the upcoming round */
    private void getAllEstimates() {
        for (Player player : players) {
            player.GetEstimate();
        }
    }

    /** politely asks every player for his/her estimates for the upcoming round */
    private void tellAllTrumpfSelected() {
        for (Player player : players) {
            player.OnTrumpfSelected(getCurrentRound().getTrumpf());
        }
    }

    /** needed for tests */
    public ArrayList<Card> getCardsStack() {
        return new ArrayList<>(cardsStack);
    }

    /** needed for tests */
    public void setRounds(ArrayList<Round> rounds) {
        this.rounds = rounds;
    }

    public void playCard(Card card, Player player) {
        getCurrentRound().playCard(card, player);
    }

    public final class Round {

        public final int[] estimates = new int[getNrPlayers()];
        public final int[] wonStiche = new int[getNrPlayers()];
        public final int[] valuesOfStiche = new int[getNrPlayers()];

        private final Stich cardsInTheMiddle;

        public Round(Stich stich) {
            cardsInTheMiddle = stich;
        }

        public Round(Color trumpColor) {
            cardsInTheMiddle = new Stich(trumpColor);
        }

        public Color getTrumpf() {
            return getCardsInTheMiddle().trumpf;
        }

        public Stich getCardsInTheMiddle() {
            return cardsInTheMiddle;
        }

        /**
         * play the gicen card
         *
         * @param card
         * @param cardsToPlay how many cards there should be to call the stich complete
         * @return true if the stich is done
         */
        public boolean PlayCard(Card card, byte cardsToPlay, Player player) {
            cardsInTheMiddle.playCard(card, player);
            return cardsInTheMiddle.getCardsPlayed() == cardsToPlay;
        }

        private void finishStich(Player winningPlayer, int value) {
            for (Player p : players) {
                p.OnStichMade(winningPlayer, value);

                wonStiche[winningPlayer.playerId]++;
                valuesOfStiche[winningPlayer.playerId] +=
                        value; // add the points to his total stiche for this round, we will see
                // later
                // if he/she estimated correctly
            }
        }

        public void playCard(Card card, Player player) {
            player.playCard(card); // remove card from the player's hand

            cardsInTheMiddle.playCard(card, player);

            if (cardsInTheMiddle.getCardsPlayed() == getNrPlayers()) {
                Player winner = cardsInTheMiddle.getWinningPlayer();
                finishStich(winner, cardsInTheMiddle.getValue()); // notify other players
                winner.CardPlayRequest(); // request to start next stich

                if (winner.carsLeft() == 0) {
                    countPointsForThisRound();
                    notifyAboutPointsAndRound();

                    // TODO: quit game if it was the last round
                    startNewRound();
                } else {
                    cardsInTheMiddle.reset();
                }
            } else {
                Player nextPlayer = getNextPlayer(player);
                nextPlayer.CardPlayRequest();
                Logger.info("asking player {} to play", nextPlayer.playerId);
            }
            updateGAmeBoard();
        }
    }
}
