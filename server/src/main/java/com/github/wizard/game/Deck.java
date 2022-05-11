package com.github.wizard.game;

import com.github.wizard.api.Card;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Deck {

    public static final Card wizard;
    public static final Card jester;

    public static final List<Card.Color> validColors;
    public static final List<Card.Value> validNumbers;

    public static final List<Card> allCards;

    static {
        wizard = Card.newBuilder().setColor(Card.Color.NONE).setValue(Card.Value.WIZARD).build();

        jester = Card.newBuilder().setColor(Card.Color.NONE).setValue(Card.Value.JESTER).build();

        validColors =
                Arrays.stream(Card.Color.values())
                        .filter(
                                color ->
                                        color != Card.Color.UNRECOGNIZED
                                                && color != Card.Color.NONE)
                        .toList();

        validNumbers =
                Arrays.stream(Card.Value.values())
                        .filter(
                                v ->
                                        v != Card.Value.UNRECOGNIZED
                                                && v != Card.Value.WIZARD
                                                && v != Card.Value.JESTER)
                        .toList();

        allCards = Collections.unmodifiableList(generateAllCards());
    }

    private static List<Card> generateNumberedCards() {
        return validColors.stream()
                .flatMap(
                        color ->
                                validNumbers.stream()
                                        .map(
                                                number ->
                                                        Card.newBuilder()
                                                                .setColor(color)
                                                                .setValue(number)
                                                                .build()))
                .toList();
    }

    private static List<Card> generateWizards() {
        return IntStream.range(0, 4).mapToObj(i -> wizard).toList();
    }

    private static List<Card> generateJesters() {
        return IntStream.range(0, 4).mapToObj(i -> jester).toList();
    }

    private static List<Card> generateAllCards() {
        return Stream.of(generateJesters(), generateWizards(), generateNumberedCards())
                .flatMap(List::stream)
                .toList();
    }

    private final Queue<Card> cards;

    public Deck() {
        cards = new LinkedList<>(allCards);
    }

    public void shuffle() {
        cards.clear();
        cards.addAll(allCards);

        Collections.shuffle((List<?>) cards);
    }

    public Card draw() {
        return cards.remove();
    }

    public List<Card> draw(int amount) {
        return IntStream.range(0, amount).mapToObj(i -> draw()).toList();
    }

    public int size() {
        return cards.size();
    }
}
