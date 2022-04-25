package com.github.wizard;

import static java.lang.Math.abs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import org.tinylog.Logger;

public class Game {
    public final int gameId;
    public boolean ready = false;
    private byte nrPlayers = 0;

    private final ArrayList<Player> playerArrayList = new ArrayList<>(Server.MAX_PLAYERS);

    private ArrayList<GameRound> rounds = new ArrayList<>();
    private final ArrayList<Card> cardsStack = new ArrayList<>();

    private static final Random random = new Random();

    public Game(int i) {
        this.gameId = i;
    }

    public ArrayList<Player> getPlayerArrayList() {
        return playerArrayList;
    }

    /** creates a new card stack, consisting of all available cards */
    protected void initializeCardStack() {
        cardsStack.addAll(Arrays.asList(Server.cards));
    }

    /**
     * adds a player to the current game. This does not include the subscription for updates
     *
     * @param player
     * @return the playerid of whoever was added
     */
    public int addPlayer(Player player) {
        if (nrPlayers >= Server.MAX_PLAYERS) return nrPlayers;

        playerArrayList.add(player);
        player.playerId = nrPlayers;
        player.game = this;
        nrPlayers++;

        // TODO: throw error when playerlist is full
        return player.playerId;
    }

    public void startNewRound() {
        switch (random.nextInt(4)) { // choose Trumpf randomly
            case 0:
                rounds.add(new GameRound(Color.RED));
                break;
            case 1:
                rounds.add(new GameRound(Color.BLUE));
                break;
            case 2:
                rounds.add(new GameRound(Color.GREEN));
                break;
            case 3:
                rounds.add(new GameRound(Color.YELLOW));
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
        for (int i = 0;
                i < getNrPlayers();
                i++) { // every player gets nrOfRound random cards, no card can exist twice
            Card[] cardsForPlayerX =
                    new Card[getRoundNr()]; // we will take the cards, place them in this array and
            // then hand them over to the player when we are done
            // picking his/her cards
            for (int j = 0; j < cardsForPlayerX.length; j++) {
                int cardIndex = random.nextInt(cardsStack.size()); // pick some random card
                cardsForPlayerX[j] =
                        cardsStack.get(cardIndex); // place them next to the stack on the desk
                cardsStack.remove(
                        cardsStack.get(
                                cardIndex)); // remove it from stack so nobody gets the same card
                // again
            }
            playerArrayList.get(i).giveMeCards(cardsForPlayerX); // hand em to over to the player
        }
    }

    public GameRound getCurrentRound() {
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
        for (Player player : playerArrayList) {
            if (!player.isSubscribed()) return false;
        }
        return true;
    }

    public int getNrPlayers() {
        return nrPlayers;
    }

    public void playCard(Card card, Player player) {
        player.playCard(card); // remove card from the player's hand
        if (getCurrentRound()
                .PlayCard(
                        card,
                        (byte) getNrPlayers(),
                        player)) { // returns true if we played the last card
            Player winner = getCurrentRound().cardsInTheMiddle.getWinningPlayer();
            finnishStich(
                    winner,
                    getCurrentRound()
                            .cardsInTheMiddle
                            .getValue()); // notify peers that I finished this
            // Play next first card as we have won this stich
            winner.CardPlayRequest();
            if (winner.carsLeft() == 0) { // see if we played out the last card
                countPointsForThisRound(); // see how the estimates were :D
                notifyAboutPointsAndRound(); // tell em how much points they have
                // todo quit game if it was the last round
                startNewRound();
            } else { // there are still cards left so ask for new Cards
                getCurrentRound().cardsInTheMiddle =
                        new Stich(getCurrentRound().trumpf); // remove my stich from
            }
        } else {
            // ask next player to play card
            playerArrayList.get((player.playerId + 1) % nrPlayers).CardPlayRequest();
            Logger.info("asking player {} to play", (player.playerId + 1) % nrPlayers);
            updateGAmeBoard();
        }
    }

    /** calculates the points for each player in respect to his/her estimate */
    private void countPointsForThisRound() {
        for (Player p : playerArrayList) {
            if (getCurrentRound().estimates[p.playerId]
                    == getCurrentRound()
                            .stiche[p.playerId]) { // the player correctly estimated his stiche
                p.addPoints(
                        20
                                + getCurrentRound().stiche[p.playerId]
                                        * 10); // 20 points for correct estimate+10 points for each
                // stich
            } else { // he/she did not correctly estimate
                p.subtractPoints(
                        abs(
                                        getCurrentRound().estimates[p.playerId]
                                                - getCurrentRound().stiche[p.playerId])
                                * 10); // he/she will lose 10 points for every stich above or below
                // his/her estimate
            }
        }
    }

    /** will ask all players to notify about their points and current round nr */
    private void notifyAboutPointsAndRound() {
        int roundNumber = getRoundNr();
        playerArrayList.forEach(p -> p.OnRoundFinished(roundNumber));
    }

    private void updateGAmeBoard() {
        GameRound currentRound = getCurrentRound();
        playerArrayList.forEach(p -> p.OnGameBoardUpdate(currentRound));
    }

    private void finnishStich(Player winningPlayer, int value) {
        for (Player p : playerArrayList) {
            p.OnStichMade(winningPlayer, value);
            getCurrentRound().stiche[winningPlayer.playerId]++;
            getCurrentRound().valuesOfStiche[winningPlayer.playerId] +=
                    value; // add the points to his total stiche for this round, we will see later
            // if he/she estimated correctly
        }
    }

    /** politely asks every player for his/her estimates for the upcoming round */
    private void getAllEstimates() {
        for (Player player : playerArrayList) {
            player.GetEstimate();
        }
    }

    /** politely asks every player for his/her estimates for the upcoming round */
    private void tellAllTrumpfSelected() {
        for (Player player : playerArrayList) {
            player.OnTrumpfSelected(getCurrentRound().trumpf);
        }
    }

    /** needed for tests */
    public ArrayList<Card> getCardsStack() {
        return cardsStack;
    }

    /** needed for tests */
    public void setRounds(ArrayList<GameRound> rounds) {
        this.rounds = rounds;
    }
}
