package com.github.wizard.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.wizard.api.Card;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TrickTests {
    Trick redTrumpTrick;

    Card wizard = Card.newBuilder().setColor(Card.Color.RED).setValue(Card.Value.WIZARD).build();

    Card red1 = Card.newBuilder().setColor(Card.Color.RED).setValue(Card.Value.ONE).build();

    Card red3 = Card.newBuilder().setColor(Card.Color.RED).setValue(Card.Value.THREE).build();

    Card red13 = Card.newBuilder().setColor(Card.Color.RED).setValue(Card.Value.THIRTEEN).build();

    Card yellow10 = Card.newBuilder().setColor(Card.Color.YELLOW).setValue(Card.Value.TEN).build();

    Card green2 = Card.newBuilder().setColor(Card.Color.GREEN).setValue(Card.Value.TWO).build();

    Card green5 = Card.newBuilder().setColor(Card.Color.GREEN).setValue(Card.Value.FIVE).build();

    Card jester = Card.newBuilder().setColor(Card.Color.RED).setValue(Card.Value.JESTER).build();

    Player player1 = new Player("1");
    Player player2 = new Player("2");
    Player player3 = new Player("3");

    Game mock;

    @BeforeEach
    void init() {
        redTrumpTrick = new Trick(red1);
        mock = mock(Game.class);
    }

    @Test
    void firstWizardWins() {
        redTrumpTrick.playCard(wizard, player1);
        redTrumpTrick.playCard(wizard, player2);

        assertEquals(player1, redTrumpTrick.getWinningPlayer());
    }

    @Test
    void highestTrumpWins() {
        redTrumpTrick.playCard(yellow10, player1);
        redTrumpTrick.playCard(red13, player2);
        redTrumpTrick.playCard(red3, player3);

        assertEquals(player2, redTrumpTrick.getWinningPlayer());
    }

    @Test
    void trumpWins() {
        redTrumpTrick.playCard(yellow10, player1);
        redTrumpTrick.playCard(red3, player2);

        assertEquals(player2, redTrumpTrick.getWinningPlayer());
    }

    @Test
    void higherValueWins() {
        redTrumpTrick.playCard(green5, player1);
        redTrumpTrick.playCard(green2, player2);

        assertEquals(player1, redTrumpTrick.getWinningPlayer());
    }

    @Test
    void jesterLoses() {
        redTrumpTrick.playCard(green2, player1);
        redTrumpTrick.playCard(jester, player2);
        redTrumpTrick.playCard(yellow10, player3);

        assertNotEquals(player2, redTrumpTrick.getWinningPlayer());
    }

    @Test
    void iHaveCheatedFlagTEst_cheated() {
        redTrumpTrick.playCard(
                green2,
                player1); // initial card, other players must now play green if they have some
        ArrayList<Card> cards = new ArrayList<>();
        when(mock.getRoundNr())
                .thenReturn(2); // for avoiding the error checking while handing out cards
        player2.game = mock;
        cards.add(green5);
        cards.add(yellow10);
        player2.giveMeCards(cards);
        redTrumpTrick.playCard(
                yellow10, player2); // player two plays yellow card despite having green
        assertEquals(true, player2.iHaveCHeatedFlag);
        assertEquals(false, player1.iHaveCHeatedFlag);
    }

    @Test
    void iHaveCheatedFlagTEst_noCheating() {
        redTrumpTrick.playCard(
                green2,
                player1); // initial card, other players must now play green if they have some
        ArrayList<Card> cards = new ArrayList<>();
        when(mock.getRoundNr())
                .thenReturn(2); // for avoiding the error checking while handing out cards
        player2.game = mock;
        cards.add(green5);
        cards.add(yellow10);
        player2.giveMeCards(cards);
        redTrumpTrick.playCard(
                green5, player2); // player two plays yellow card despite having green
        assertEquals(false, player2.iHaveCHeatedFlag);
        assertEquals(false, player1.iHaveCHeatedFlag);
    }

    @Test
    void iHaveCheatedFlagTEst_noCheating_singlePlayer() {
        ArrayList<Card> cards = new ArrayList<>();
        when(mock.getRoundNr())
                .thenReturn(2); // for avoiding the error checking while handing out cards
        player2.game = mock;
        cards.add(green5);
        cards.add(yellow10);
        player2.giveMeCards(cards);
        redTrumpTrick.playCard(
                yellow10, player2); // player two plays yellow card despite having green
        assertEquals(false, player2.iHaveCHeatedFlag);
    }

    @Test
    void playCard() {
        assertEquals(0, redTrumpTrick.getCardsPlayed());

        redTrumpTrick.playCard(jester, player1);

        assertEquals(1, redTrumpTrick.getCardsPlayed());
    }
}
