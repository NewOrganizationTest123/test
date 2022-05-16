package com.github.wizard.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.wizard.Updater;
import com.github.wizard.api.Card;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GameTests {
    Game game;
    Player player1;
    Player player2;

    Game game_withMockedPlayers;
    Player mocked_player1;
    Player mocked_player2;

    @BeforeEach
    public void init() {
        game = new Game(1);
        player1 = new Player("player_1_name");
        player2 = new Player("player_2_name");
        game.addPlayer(player1);
        game.addPlayer(player2);

        game_withMockedPlayers = new Game(2);
        mocked_player1 = mock(Player.class);

        mocked_player2 = mock(Player.class);

        game_withMockedPlayers.addPlayer(mocked_player1);
        game_withMockedPlayers.addPlayer(mocked_player2);

        // when(mocked_player1.)
    }

    @Test
    public void addPlayerTest() {
        assertEquals(2, game.getNrPlayers());
        Player player3 = new Player("player_3_name");
        game.addPlayer(player3);
        assertEquals(3, game.getNrPlayers());
        assertEquals(player3, game.getPlayers().get(2));
    }

    @Test
    public void addToManyPlayerTest() {
        Player player3 = new Player("player_3_name");
        game.addPlayer(player3);
        assertEquals(3, game.getNrPlayers());

        Player player4 = new Player("player_4_name");
        game.addPlayer(player4);
        assertEquals(4, game.getNrPlayers());

        Player player5 = new Player("player_5_name");
        game.addPlayer(player5);
        assertEquals(5, game.getNrPlayers());

        Player player6 = new Player("player_6_name");
        game.addPlayer(player6);
        assertEquals(6, game.getNrPlayers());

        Player player7 = new Player("player_7_name");
        game.addPlayer(player7);
        assertNotEquals(7, game.getNrPlayers());
    }

    @Test
    public void testStartGame() {
        when(mocked_player2.getName()).thenReturn("mocked_player2");
        when(mocked_player1.getName()).thenReturn("mocked_player1");
        game_withMockedPlayers.start();
        Round firstRound = game_withMockedPlayers.getCurrentRound();

        assertNotNull(firstRound.getTrump());
        assertEquals(firstRound.getNumber(), 1);

        verify(mocked_player1).giveMeCards(argThat(cards -> cards.size() == 1));
        verify(mocked_player2).giveMeCards(argThat(cards -> cards.size() == 1));
    }

    @Test
    public void testAllPlayersSubscribed_True() {
        when(mocked_player1.isSubscribed()).thenReturn(true);
        when(mocked_player2.isSubscribed()).thenReturn(true);
        assertTrue(game_withMockedPlayers.getPlayers().areSubscribed());
    }

    @Test
    public void testAllPlayersSubscribed_False() {
        when(mocked_player1.isSubscribed()).thenReturn(true);
        when(mocked_player2.isSubscribed()).thenReturn(false);
        assertFalse(game_withMockedPlayers.getPlayers().areSubscribed());
    }

    @Mock Trick trickMocked;

    @Test
    public void playCardTest_lastCard() {

        player1.makeEstimate(0);
        player2.makeEstimate(1);

        player1.takeTrick(0);
        player2.takeTrick(0);

        Round round = new Round(game_withMockedPlayers, trickMocked, 1);

        when(trickMocked.getWinningPlayer()).thenReturn(mocked_player1);
        when(trickMocked.getCardsPlayed()).thenReturn(2);
        when(trickMocked.getValue()).thenReturn(12);
        when(mocked_player1.cardsLeft()).thenReturn(0);

        round.playCard(mock(Card.class), mocked_player1);

        verify(trickMocked).getWinningPlayer();
        verify(trickMocked).getValue();
        verify(mocked_player1).cardsLeft();

        verify(mocked_player2).update(Updater.newOnTrickTakenResponse(new Player(null), 12));
        verify(mocked_player2).update(Updater.newOnGameBoardUpdate(null, null));
    }

    @Test
    public void playCardTest_NotLastCard() {
        Round round = new Round(game_withMockedPlayers, trickMocked, 1);
        when(trickMocked.getCardsPlayed()).thenReturn(1);

        round.playCard(mock(Card.class), mocked_player1);

        verify(mocked_player2).update(Updater.newCardPlayRequestResponse());
        verify(mocked_player2).update(Updater.newOnGameBoardUpdate(null, null));
    }
}
