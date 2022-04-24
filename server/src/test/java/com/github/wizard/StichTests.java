package com.github.wizard;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StichTests {
    Stich stich;
    Card card1;
    Card card2;
    Player player1;
    Player player2;

    @BeforeEach
    public void init() {
        stich = new Stich(Color.RED);
        player1 = new Player("player1");
        player2 = new Player("player2");
        card1 = new Card(Color.RED, 7);
        card2 = new Card(Color.RED, 3);
    }

    @Test
    public void testPlayCard() {
        assertEquals(null, stich.cards[0]);
        assertEquals(null, stich.players[0]);

        stich.playCArd(card1, player1);

        assertEquals(card1, stich.cards[0]);
        assertEquals(player1, stich.players[0]);
    }

    @Test
    public void testGetWinningPlayer() {
        stich.playCArd(card1, player1);
        stich.playCArd(card2, player2);
        stich.getWinningPlayer();
        assertEquals(player1, stich.getWinningPlayer());
    }
}
