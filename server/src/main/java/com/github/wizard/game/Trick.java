package com.github.wizard.game;

import com.github.wizard.Server;
import java.util.ArrayList;
import java.util.List;

public class Trick {

    private final List<Card> cards = new ArrayList<>(Server.MAX_PLAYERS);
    private final List<Player> players = new ArrayList<>(Server.MAX_PLAYERS);

    Color trump;

    public void reset() {
        getCards().clear();
        players.clear();
    }

    public Trick(Color trump) {
        this.trump = trump;
    }

    public void playCard(Card card, Player player) {
        if (getCards().size() >= Server.MAX_PLAYERS)
            throw new IllegalArgumentException("cannot play more cards than there are players");

        getCards().add(card);
        players.add(player);
    }

    public int getCardsPlayed() {
        return getCards().size();
    }
    /**
     * should return the value for a given Trick
     *
     * @return
     */
    public int getValue() {
        Color firstColor =
                getCards().get(0).color(); // as only cards of similar color and wizards count

        return getCards().stream()
                .map(
                        card -> {
                            if (card.color() != firstColor
                                    || card.value() == Integer.MAX_VALUE
                                    || card.value() == -1) {
                                return 0;
                            }
                            return card.value();
                        })
                .reduce(Integer::sum)
                .orElse(-1);
    }

    public Player getWinningPlayer() { // todo do not ignore trump
        Color firstColor =
                getCards().get(0).color(); // as only cards of similar color and wizards count

        int highestValueIndex = 0;

        for (int i = 0; i < getCards().size(); i++) {
            Card card = getCards().get(i);

            // First wizard wins
            if (card.value() == Integer.MAX_VALUE) return players.get(i);

            // Jester is always ignored
            if (card.value() == -1) continue;

            if (i >= 1) {
                // new color is trump, old was not
                if (card.color() == trump && getCards().get(highestValueIndex).color() != trump)
                    highestValueIndex = i;
                else if (card.color() == firstColor
                        && card.value() > getCards().get(highestValueIndex).value())
                    highestValueIndex = i;
            }
        }

        return players.get(highestValueIndex);
    }

    public List<Card> getCards() {
        return cards;
    }
}
