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
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlayerTests {
    Player player;
    Card card1;
    Card card2;
    List<Card> cards = new ArrayList<>();
    Game game;

    // @Mock private StreamObserver<Response> responseObserverMock;

    @BeforeEach
    void init() {
        player = new Player("player_name");
        card1 = Card.newBuilder().setColor(Card.Color.RED).setValue(Card.Value.SEVEN).build();
        card2 = Card.newBuilder().setColor(Card.Color.RED).setValue(Card.Value.TWO).build();
        cards.add(card1);
        cards.add(card2);
        game = mock(Game.class);
        player.game = game;
    }

    @Test
    void testAddPoints() {
        assertEquals(0, player.getPoints());
        player.addPoints(30);
        assertEquals(30, player.getPoints());
    }

    @Test
    void testSubtractPoints() {
        assertEquals(0, player.getPoints());
        player.subtractPoints(10);
        assertEquals(-10, player.getPoints());
    }

    @Test
    void testGiveMeCards() {
        assertEquals(0, player.getCards().size());
        when(game.getRoundNr()).thenReturn(2);

        player.giveMeCards(cards);

        assertEquals(2, player.getCards().size());
    }

    @Test
    void testPlayCard() {
        when(game.getRoundNr()).thenReturn(2);
        player.giveMeCards(cards);
        assertTrue(player.getCards().contains(card1));
        int number_of_cards = player.getCards().size();
        player.playCard(0);
        assertEquals(number_of_cards - 1, player.getCards().size());
        assertFalse(player.getCards().contains(card1));
    }

    @Test
    void updatePointsCorrectPrediction() {
        player.makeEstimate(2);
        player.takeTrick();
        player.takeTrick();

        player.updatePoints();

        assertEquals(40, player.getPoints());
    }

    @Test
    void updatePointsWithoutPrediction() {
        player.takeTrick();
        player.takeTrick();

        player.updatePoints();

        assertEquals(0, player.getPoints());
    }

    @Test
    void updatePointsIncorrectPrediction() {
        player.makeEstimate(4);
        player.takeTrick();
        player.takeTrick();

        player.updatePoints();

        assertEquals(-20, player.getPoints());
    }

    @Test
    void isNotSubscribed() {
        player.setUpdater(null);

        assertFalse(player.isSubscribed());
    }

    @Test
    void isSubscribed() {
        player.setUpdater(mock(Updater.class));

        assertTrue(player.isSubscribed());
    }

    @Test
    void giveInvalidCardAmount() {
        List<Card> cardsMock = mock(ArrayList.class);
        when(game.getRoundNr()).thenReturn(4);
        when(cardsMock.size()).thenReturn(3);

        assertThrows(
                IndexOutOfBoundsException.class,
                () -> player.giveMeCards(cardsMock),
                "You gave me too many or to few cards. Current round is 4 and you gave me 3 cards");
    }

    @Test
    void playInvalidCard() {
        when(game.getRoundNr()).thenReturn(1);

        Card yellow1 =
                Card.newBuilder().setColor(Card.Color.YELLOW).setValue(Card.Value.ONE).build();

        player.giveMeCards(List.of(yellow1));

        assertThrows(
                IllegalArgumentException.class,
                () -> player.playCard(1),
                "I wanted to play a card I did not have");
    }

    @RepeatedTest(50) // run this multiple times to cover all random scenarios
    void testMakeRandomEstimate() {
        when(game.getRoundNr()).thenReturn(1);
        Updater mockUpdater = mock(Updater.class);
        player.setUpdater(mockUpdater);

        assertFalse(player.estimateSubmitted());
        player.makeRandomEstimate();
        assertTrue(player.estimateSubmitted());
    }

    @RepeatedTest(10) // run this multiple times to cover all random scenarios
    void testMakeRandomEstimateWizzards() {
        when(game.getRoundNr()).thenReturn(2);
        ArrayList<Card> wizzards = new ArrayList<>();
        wizzards.add(
                Card.newBuilder().setColor(Card.Color.RED).setValue(Card.Value.WIZARD).build());
        wizzards.add(
                Card.newBuilder().setColor(Card.Color.RED).setValue(Card.Value.WIZARD).build());

        player.giveMeCards(wizzards);

        Updater mockUpdater = mock(Updater.class);
        player.setUpdater(mockUpdater);

        assertFalse(player.estimateSubmitted());
        player.makeRandomEstimate();
        assertTrue(player.estimateSubmitted());
        assertTrue(player.getEstimate() >= 2); // every wizzard counts at least one
    }

    @RepeatedTest(10) // run this multiple times to cover all random scenarios
    void testMakeRandomEstimateTrumpf() {
        when(game.getRoundNr()).thenReturn(2);
        ArrayList<Card> cardArrayList = new ArrayList<>();
        cardArrayList.add(
                Card.newBuilder().setColor(Card.Color.RED).setValue(Card.Value.NINE).build());
        cardArrayList.add(
                Card.newBuilder().setColor(Card.Color.RED).setValue(Card.Value.TEN).build());
        Round mockedRound = mock(Round.class);
        when(game.getCurrentRound()).thenReturn(mockedRound);
        when(mockedRound.getTrump())
                .thenReturn(
                        Card.newBuilder()
                                .setValue(Card.Value.EIGHT)
                                .setColor(Card.Color.RED)
                                .build()); // red is trump
        player.giveMeCards(cardArrayList);

        Updater mockUpdater = mock(Updater.class);
        player.setUpdater(mockUpdater);

        assertFalse(player.estimateSubmitted());
        player.makeRandomEstimate();
        assertTrue(player.estimateSubmitted());
        assertTrue(player.getEstimate() >= 2); // every wizzard counts at least one
    }

    @RepeatedTest(10) // run this multiple times to cover all random scenarios
    void testGetPossibleCardsAndSortNoCheating() {
        when(game.getRoundNr()).thenReturn(3);
        ArrayList<Card> cardArrayList = new ArrayList<>();
        cardArrayList.add(
                Card.newBuilder().setColor(Card.Color.RED).setValue(Card.Value.NINE).build());
        cardArrayList.add(
                Card.newBuilder().setColor(Card.Color.RED).setValue(Card.Value.TEN).build());
        cardArrayList.add(
                Card.newBuilder().setColor(Card.Color.GREEN).setValue(Card.Value.TEN).build());
        Round mockedRound = mock(Round.class);
        when(game.getCurrentRound()).thenReturn(mockedRound);
        Trick mockedTrick = mock(Trick.class);
        when(mockedRound.getCardsInTheMiddle()).thenReturn(mockedTrick);
        ArrayList<Card> playedCards = new ArrayList<>();
        playedCards.add(
                Card.newBuilder()
                        .setColor(Card.Color.RED)
                        .setValue(Card.Value.THIRTEEN)
                        .build()); // as red is on the table we need to play red
        when(mockedRound.getTrump())
                .thenReturn(
                        Card.newBuilder()
                                .setValue(Card.Value.EIGHT)
                                .setColor(Card.Color.RED)
                                .build()); // red is trump
        when(mockedTrick.getCards()).thenReturn(playedCards);
        player.giveMeCards(cardArrayList);

        List<Card> possibleCards = player.getPossibleCards(0);
        // red cards are allowed but not green ones
        assertTrue(possibleCards.contains(cardArrayList.get(0)));
        assertTrue(possibleCards.contains(cardArrayList.get(1)));
        assertFalse(possibleCards.contains(cardArrayList.get(2)));

        player.sortPossibleCards(possibleCards);
        assertEquals(possibleCards.get(0), cardArrayList.get(0));
        assertEquals(possibleCards.get(possibleCards.size() - 1), cardArrayList.get(1));
        player.makeEstimate(10); // this will result in us always playing the higher card
        assertEquals(possibleCards.size() - 1, player.selectCardToPlay(possibleCards));
    }

    @RepeatedTest(50) // run this multiple times to cover all random scenarios
    void testGetPossibleCardsCheating() {
        when(game.getRoundNr()).thenReturn(3);
        ArrayList<Card> cardArrayList = new ArrayList<>();
        cardArrayList.add(
                Card.newBuilder().setColor(Card.Color.RED).setValue(Card.Value.NINE).build());
        cardArrayList.add(
                Card.newBuilder().setColor(Card.Color.RED).setValue(Card.Value.TEN).build());
        cardArrayList.add(
                Card.newBuilder().setColor(Card.Color.GREEN).setValue(Card.Value.TEN).build());
        Round mockedRound = mock(Round.class);
        when(game.getCurrentRound()).thenReturn(mockedRound);
        Trick mockedTrick = mock(Trick.class);
        when(mockedRound.getCardsInTheMiddle()).thenReturn(mockedTrick);
        ArrayList<Card> playedCards = new ArrayList<>();
        playedCards.add(
                Card.newBuilder()
                        .setColor(Card.Color.RED)
                        .setValue(Card.Value.THIRTEEN)
                        .build()); // as red is on the table we need to play red
        when(mockedTrick.getCards()).thenReturn(playedCards);
        player.giveMeCards(cardArrayList);

        List<Card> possibleCards = player.getPossibleCards(101);
        // red cards are allowed but not green ones, we wanted to cheat therefor we will get only
        // the green one.
        assertFalse(possibleCards.contains(cardArrayList.get(0)));
        assertFalse(possibleCards.contains(cardArrayList.get(1)));
        assertTrue(possibleCards.contains(cardArrayList.get(2)));
    }
}
