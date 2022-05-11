package com.github.wizard.game;

import static org.junit.jupiter.api.Assertions.*;

import com.github.wizard.api.Card;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeckTest {

    private Deck deck;

    @BeforeEach
    void setUp() {
        deck = new Deck();
    }

    @Test
    void shuffle() {
        deck.draw(60);

        assertEquals(0, deck.size());

        deck.shuffle();

        assertEquals(60, deck.size());
    }

    @Test
    void drawNotShuffled() {
        List<Card> allCards = deck.draw(60);

        assertEquals(Deck.allCards, allCards);
    }

    @Test
    void drawShuffled() {
        List<Card> cards = deck.draw(60);

        List<Card> redCards = getCardsFromColor(cards, Card.Color.RED);
        List<Card> yellowCards = getCardsFromColor(cards, Card.Color.YELLOW);
        List<Card> greenCards = getCardsFromColor(cards, Card.Color.GREEN);
        List<Card> blueCards = getCardsFromColor(cards, Card.Color.BLUE);
        List<Card> jesters = cards.stream().filter(card -> card == Deck.jester).toList();
        List<Card> wizards = cards.stream().filter(card -> card == Deck.wizard).toList();

        assertColoredCards(redCards);
        assertColoredCards(yellowCards);
        assertColoredCards(greenCards);
        assertColoredCards(blueCards);

        assertEquals(4, jesters.size());
        assertEquals(4, wizards.size());
    }

    @Test
    void drawTooMany() {
        assertThrows(NoSuchElementException.class, () -> deck.draw(61));
    }

    @Test
    void size() {
        IntStream.range(0, 60)
                .forEach(
                        i -> {
                            assertEquals(60 - i, deck.size());
                            deck.draw();
                        });
    }

    private static List<Card> getCardsFromColor(List<Card> cards, Card.Color color) {
        return cards.stream().filter(card -> card.getColor() == color).toList();
    }

    private static void assertColoredCards(List<Card> cards) {
        List<Card> distinct = cards.stream().distinct().toList();
        List<Card> valid =
                distinct.stream()
                        .filter(card -> Deck.validColors.contains(card.getColor()))
                        .filter(card -> Deck.validNumbers.contains(card.getValue()))
                        .toList();

        assertEquals(13, valid.size());
    }
}
