package com.github.wizard.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.wizard.Updater;
import com.github.wizard.api.Card;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PlayerTests {
    Player player;
    Card card1;
    Card card2;
    List<Card> cards = new ArrayList<>();
    Game game;

    // @Mock private StreamObserver<Response> responseObserverMock;

    @BeforeEach
    public void init() {
        player = new Player("player_name");
        card1 = Card.newBuilder().setColor(Card.Color.RED).setValue(Card.Value.SEVEN).build();
        card2 = Card.newBuilder().setColor(Card.Color.RED).setValue(Card.Value.TWO).build();
        cards.add(card1);
        cards.add(card2);
        game = mock(Game.class);
        player.game = game;
    }

    @Test
    public void testAddPoints() {
        assertEquals(0, player.getPoints());
        player.addPoints(30);
        assertEquals(30, player.getPoints());
    }

    @Test
    public void testSubtractPoints() {
        assertEquals(0, player.getPoints());
        player.subtractPoints(10);
        assertEquals(-10, player.getPoints());
    }

    @Test
    public void testGiveMeCards() {
        assertEquals(0, player.getCards().size());
        when(game.getRoundNr()).thenReturn(2);

        player.giveMeCards(cards);

        assertEquals(2, player.getCards().size());
    }

    @Test
    public void testPlayCard() {
        when(game.getRoundNr()).thenReturn(2);
        player.giveMeCards(cards);
        assertTrue(player.getCards().contains(card1));
        int number_of_cards = player.getCards().size();
        player.playCard(0);
        assertEquals(number_of_cards - 1, player.getCards().size());
        assertFalse(player.getCards().contains(card1));
    }

    @Test
    public void updatePointsCorrectPrediction() {
        player.makeEstimate(2);
        player.takeTrick(0);
        player.takeTrick(0);

        player.updatePoints();

        assertEquals(40, player.getPoints());
    }

    @Test
    public void updatePointsWithoutPrediction() {
        player.takeTrick(0);
        player.takeTrick(1);

        player.updatePoints();

        assertEquals(0, player.getPoints());
    }

    @Test
    public void updatePointsIncorrectPrediction() {
        player.makeEstimate(4);
        player.takeTrick(0);
        player.takeTrick(0);

        player.updatePoints();

        assertEquals(-20, player.getPoints());
    }

    @Test
    public void isNotSubscribed() {
        player.setUpdater(null);

        assertFalse(player.isSubscribed());
    }

    @Test
    public void isSubscribed() {
        player.setUpdater(mock(Updater.class));

        assertTrue(player.isSubscribed());
    }

    @Test
    public void giveInvalidCardAmount() {
        List<Card> cardsMock = mock(ArrayList.class);
        when(game.getRoundNr()).thenReturn(4);
        when(cardsMock.size()).thenReturn(3);

        assertThrows(
                IndexOutOfBoundsException.class,
                () -> player.giveMeCards(cardsMock),
                "You gave me too many or to few cards. Current round is 4 and you gave me 3 cards");
    }

    @Test
    public void playInvalidCard() {
        when(game.getRoundNr()).thenReturn(1);

        Card yellow1 =
                Card.newBuilder().setColor(Card.Color.YELLOW).setValue(Card.Value.ONE).build();

        player.giveMeCards(List.of(yellow1));

        assertThrows(
                IllegalArgumentException.class,
                () -> player.playCard(1),
                "I wanted to play a card I did not have");
    }
}
