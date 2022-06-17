package com.github.wizard.game;

import com.github.wizard.Server;
import com.github.wizard.api.Card;
import java.util.ArrayList;
import java.util.List;

public class Trick {

    private final List<Card> cards = new ArrayList<>(Server.MAX_PLAYERS);
    private final List<Player> players = new ArrayList<>(Server.MAX_PLAYERS);

    Card trump;

    public void reset() {
        cards.clear();
        players.clear();
    }

    public Trick(Card trump) {
        this.trump = trump;
    }

    public void playCard(Card card, Player player) {
        if (getCards().size() >= Server.MAX_PLAYERS)
            throw new IllegalArgumentException("cannot play more cards than there are players");

        getCards().add(card);
        players.add(player);
        getWinningPlayer(); // this will set the cheated flag
    }

    public int getCardsPlayed() {
        return getCards().size();
    }

    public Player getWinningPlayer() {
        Card.Color firstColor =
                getCards().get(0).getColor(); // as only cards of similar color and wizards count

        int highestValueIndex = 0;

        for (int i = 0; i < getCards().size(); i++) {
            Card card = getCards().get(i);

            // First wizard wins
            if (card.getValue() == Card.Value.WIZARD) return players.get(i);

            // Jester is always ignored
            if (card.getValue() == Card.Value.JESTER) continue;

            if (i >= 1) {
                // new color is trump, old was not
                if (card.getColor() == trump.getColor()
                        && getCards().get(highestValueIndex).getColor() != trump.getColor())
                    highestValueIndex = i;
                else if (card.getColor() == firstColor
                        && card.getValue().getNumber()
                                > getCards().get(highestValueIndex).getValue().getNumber())
                    highestValueIndex = i;
                else // not trumpf and some other color
                if (players.get(i).getCards().stream()
                                .filter(
                                        card1 ->
                                                card1.getColor() == firstColor
                                                        || card1.getColor() == trump.getColor())
                                .count()
                        > 0)
                    players.get(i).iHaveCHeatedFlag =
                            true; // if this player could have made this stich with some other card
                // or the trump he has cheated
            }
        }

        return players.get(highestValueIndex);
    }

    public List<Card> getCards() {
        return cards;
    }
}
