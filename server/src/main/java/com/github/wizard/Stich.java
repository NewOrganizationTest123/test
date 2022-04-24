package com.github.wizard;

public class Stich {
    Card[] cards = new Card[ServerMain.MAX_PLAYERS];
    Player[] players = new Player[ServerMain.MAX_PLAYERS];
    int cardsCounter = 0;
    Color trumpf;

    public Stich(Color trumpf) {
        this.trumpf = trumpf;
    }

    public void playCArd(Card card, Player player) {
        cards[cardsCounter] = card;
        players[cardsCounter++] = player;
    }

    /**
     * should return the value for a given Stich
     *
     * @return
     */
    public int getValue() {
        Color firstColor = cards[0].color; // as only cards of similar color and wizzards count
        int sum = cards[0].value;
        for (int i = 1; i < cardsCounter; i++) {
            if (cards[i].color != firstColor
                    || cards[i].value == Integer.MAX_VALUE
                    || cards[i].value
                            == -1) // cards of different color, wizzards and narren do not count
            continue;
            sum += cards[i].value;
        }
        return sum;
    }

    public Player getWinningPlayer() { // todo do not ignore trumpf
        Color firstColor = cards[0].color; // as only cards of similar color and wizzards count
        int highestValueIndex = 0;
        for (int i = 1; i < cardsCounter; i++) {
            if (cards[i].color != firstColor
                    && cards[i].value
                            != Integer.MAX_VALUE) // cards of different color that are not a wizzard
                // wil loose automatically
                continue;
            // if it is the same color and no wizzard it can only be higher or lower
            if (cards[i].value > cards[highestValueIndex].value) // it is bigger
            highestValueIndex = i;
            // if we already have the highest card we have to do nothing
        }
        return players[highestValueIndex];
    }
}
