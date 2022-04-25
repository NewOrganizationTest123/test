package com.github.wizard.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.wizard.api.Response;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PlayerTests {
    Player player;
    Card card1;
    Card card2;
    List<Card> cards = new ArrayList<>();
    Game game;

    @Mock private StreamObserver<Response> responseObserverMock;

    @BeforeEach
    public void init() {
        player = new Player("player_name");
        card1 = new Card(Color.RED, 7);
        card2 = new Card(Color.RED, 2);
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
    public void testSubstractPoints() {
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
        player.winStich(0);
        player.winStich(0);

        player.updatePoints();

        assertEquals(40, player.getPoints());
    }

    @Test
    public void updatePointsIncorrectPrediction() {
        player.makeEstimate(4);
        player.winStich(0);
        player.winStich(0);

        player.updatePoints();

        assertEquals(-20, player.getPoints());
    }
}
