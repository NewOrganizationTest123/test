package com.github.wizard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

    @Test
    public void cardPlayRequest() {
        player.responseObserver = responseObserverMock;

        player.CardPlayRequest();

        verify(responseObserverMock)
                .onNext(
                        argThat(
                                response -> {
                                    assertEquals("2", response.getType());
                                    assertEquals("Please play a card", response.getData());

                                    return true;
                                }));
    }

    @Test
    public void onGameBoardUpdate() {
        List<Card> cards =
                List.of(
                        new Card(Color.YELLOW, 10),
                        new Card(null, Integer.MAX_VALUE),
                        new Card(null, -1),
                        new Card(Color.RED, 1),
                        new Card(Color.BLUE, 3),
                        new Card(Color.GREEN, 13));

        player.responseObserver = responseObserverMock;
        Game.Round roundMock = mock(Game.Round.class);
        Stich stichMock = mock(Stich.class);

        when(game.getRoundNr()).thenReturn(6);
        when(game.getCurrentRound()).thenReturn(roundMock);
        when(roundMock.getCardsInTheMiddle()).thenReturn(stichMock);
        when(stichMock.getCards()).thenReturn(cards);

        player.giveMeCards(cards);

        player.OnGameBoardUpdate();

        verify(responseObserverMock)
                .onNext(
                        argThat(
                                response -> {
                                    assertEquals("3", response.getType());
                                    assertEquals(
                                            "/YELLOW(10)/Wizard/Jester/RED(1)/BLUE(3)/GREEN(13)//YELLOW(10)/Wizard/Jester/RED(1)/BLUE(3)/GREEN(13)/",
                                            response.getData());

                                    return true;
                                }));
    }

    @Test
    public void onStichMade() {
        player.responseObserver = responseObserverMock;

        player.OnStichMade(new Player("player"), 13);

        verify(responseObserverMock)
                .onNext(
                        argThat(
                                response -> {
                                    assertEquals("1", response.getType());
                                    assertEquals(
                                            "Player player has made this stich with value 13",
                                            response.getData());

                                    return true;
                                }));
    }

    @Test
    public void onTrumpfSelected() {

        player.responseObserver = responseObserverMock;

        player.OnTrumpfSelected(Color.RED);

        verify(responseObserverMock)
                .onNext(
                        argThat(
                                response -> {
                                    assertEquals("4", response.getType());
                                    assertEquals("RED", response.getData());

                                    return true;
                                }));
    }

    @Test
    public void getEstimate() {
        player.responseObserver = responseObserverMock;

        player.GetEstimate();

        verify(responseObserverMock)
                .onNext(
                        argThat(
                                response -> {
                                    assertEquals("5", response.getType());
                                    return true;
                                }));
    }

    @Test
    public void onRoundFinished() {
        player.responseObserver = responseObserverMock;

        player.OnRoundFinished(3);

        verify(responseObserverMock)
                .onNext(
                        argThat(
                                response -> {
                                    assertEquals("6", response.getType());
                                    assertEquals("0/3", response.getData());

                                    return true;
                                }));
    }
}
