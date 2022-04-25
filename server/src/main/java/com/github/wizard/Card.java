package com.github.wizard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

enum Color {
    RED,
    GREEN,
    BLUE,
    YELLOW,
}

public final class Card {

    public static final List<Card> deck = Collections.unmodifiableList(generateDeck());

    public final Color color;
    // +inf:=Zauberer
    public final int value;

    public Card(Color color, int value) {
        this.color = color;
        this.value = value;
    } // -1:=Narr

    @Override
    public String toString() {
        if (value == Integer.MAX_VALUE) {
            return "Wizard";
        } else if (value == -1) {
            return "Jester";
        }

        return color.name() + "(" + value + ")";
    }

    public static Queue<Card> getShuffledDeck() {
        List<Card> deckCopy = new ArrayList<>(deck);
        Collections.shuffle(deckCopy);

        return new LinkedList<>(deckCopy);
    }

    private static List<Card> generateNumberedCards() {
        return IntStream.rangeClosed(1, 13)
                .mapToObj(number -> Stream.of(Color.values()).map(color -> new Card(color, number)))
                .flatMap(Function.identity())
                .collect(Collectors.toList());
    }

    private static List<Card> generateJesters() {
        return IntStream.range(0, 4)
                .mapToObj(ignored -> new Card(null, -1))
                .collect(Collectors.toList());
    }

    private static List<Card> generateWizards() {
        return IntStream.range(0, 4)
                .mapToObj(ignored -> new Card(null, Integer.MAX_VALUE))
                .collect(Collectors.toList());
    }

    private static List<Card> generateDeck() {
        List<Card> list = new ArrayList<>(60);

        list.addAll(generateWizards());
        list.addAll(generateJesters());
        list.addAll(generateNumberedCards());

        return list;
    }
}
