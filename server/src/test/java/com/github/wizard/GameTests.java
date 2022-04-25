package com.github.wizard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
        assertEquals(player3, game.getPlayerArrayList().get(2));
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
    public void testStartNewRound() {
        game.initializeCardStack();
        int number_of_cards_on_stack = game.getCardsStack().size();
        game_withMockedPlayers.startNewRound();

        assertNotNull(game_withMockedPlayers.getCurrentRound().trumpf);
        assertEquals(
                number_of_cards_on_stack
                        - game_withMockedPlayers.getRoundNr() * game.getNrPlayers(),
                game_withMockedPlayers.getCardsStack().size());
    }

    @Test
    public void testAllPlayersSubscribed_True() {
        when(mocked_player1.isSubscribed()).thenReturn(true);
        when(mocked_player2.isSubscribed()).thenReturn(true);
        assertTrue(game_withMockedPlayers.allPlayersSubscribed());
    }

    @Test
    public void testAllPlayersSubscribed_False() {
        when(mocked_player1.isSubscribed()).thenReturn(true);
        when(mocked_player2.isSubscribed()).thenReturn(false);
        assertFalse(game_withMockedPlayers.allPlayersSubscribed());
    }

    @Test
    public void playCardTest_lastCard() {
        Game.Round roundMock = mock(Game.Round.class);
        Stich stichMocked = mock(Stich.class);
        roundMock.cardsInTheMiddle = stichMocked;
        roundMock.stiche = new int[Server.MAX_PLAYERS];
        roundMock.stiche[0] = 0;
        roundMock.stiche[1] = 0;
        roundMock.valuesOfStiche = new int[Server.MAX_PLAYERS];
        roundMock.valuesOfStiche[0] = 0;
        roundMock.valuesOfStiche[1] = 0;
        roundMock.estimates = new int[Server.MAX_PLAYERS];
        roundMock.estimates[0] = 0;
        roundMock.estimates[1] = 1;

        game_withMockedPlayers.startNewRound();
        ArrayList<Game.Round> round_list = new ArrayList<>();
        round_list.add(roundMock);
        game_withMockedPlayers.setRounds(round_list);

        when(stichMocked.getWinningPlayer()).thenReturn(mocked_player1);
        when(stichMocked.getValue()).thenReturn(12);
        when(mocked_player1.carsLeft()).thenReturn(0);
        when(roundMock.PlayCard(any(Card.class), any(Byte.class), any(Player.class)))
                .thenReturn(true);

        game_withMockedPlayers.playCard(mock(Card.class), mocked_player1);

        verify(stichMocked).getWinningPlayer();
        verify(stichMocked).getValue();
        verify(mocked_player1).carsLeft();
    }

    @Test
    public void playCardTest_NotlastCard() {
        Game.Round roundMock = mock(Game.Round.class);
        game_withMockedPlayers.startNewRound();
        ArrayList<Game.Round> round_list = new ArrayList<>();
        round_list.add(roundMock);
        game_withMockedPlayers.setRounds(round_list);

        when(roundMock.PlayCard(any(Card.class), any(Byte.class), any(Player.class)))
                .thenReturn(false);
        game_withMockedPlayers.playCard(mock(Card.class), mocked_player1);

        verify(mocked_player2).CardPlayRequest();
    }
}
