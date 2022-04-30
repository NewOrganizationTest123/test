package com.github.wizard.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TrickTests {
    Trick redTrumpTrick;

    Card wizard = new Card(null, Integer.MAX_VALUE);
    Card red3 = new Card(Color.RED, 3);
    Card red13 = new Card(Color.RED, 13);
    Card yellow10 = new Card(Color.YELLOW, 10);
    Card green2 = new Card(Color.GREEN, 2);
    Card green5 = new Card(Color.GREEN, 5);
    Card jester = new Card(null, -1);

    Player player1 = new Player("1");
    Player player2 = new Player("2");
    Player player3 = new Player("3");

    @BeforeEach
    public void init() {
        redTrumpTrick = new Trick(Color.RED);
    }

    @Test
    public void firstWizardWins() {
        redTrumpTrick.playCard(wizard, player1);
        redTrumpTrick.playCard(wizard, player2);

        assertEquals(player1, redTrumpTrick.getWinningPlayer());
    }

    @Test
    public void highestTrumpWins() {
        redTrumpTrick.playCard(yellow10, player1);
        redTrumpTrick.playCard(red13, player2);
        redTrumpTrick.playCard(red3, player3);

        assertEquals(player2, redTrumpTrick.getWinningPlayer());
    }

    @Test
    public void trumpWins() {
        redTrumpTrick.playCard(yellow10, player1);
        redTrumpTrick.playCard(red3, player2);

        assertEquals(player2, redTrumpTrick.getWinningPlayer());
    }

    @Test
    public void higherValueWins() {
        redTrumpTrick.playCard(green5, player1);
        redTrumpTrick.playCard(green2, player2);

        assertEquals(player1, redTrumpTrick.getWinningPlayer());
    }

    @Test
    public void jesterLoses() {
        redTrumpTrick.playCard(green2, player1);
        redTrumpTrick.playCard(jester, player2);
        redTrumpTrick.playCard(yellow10, player3);

        assertNotEquals(player2, redTrumpTrick.getWinningPlayer());
    }

    @Test
    public void playCard() {
        assertEquals(0, redTrumpTrick.getCardsPlayed());

        redTrumpTrick.playCard(jester, player1);

        assertEquals(1, redTrumpTrick.getCardsPlayed());
    }
}
