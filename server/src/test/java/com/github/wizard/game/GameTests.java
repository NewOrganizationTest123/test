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
import java.util.Timer;
import java.util.TimerTask;
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
        when(trickMocked.getCardsPlayed()).thenReturn(2);
        round.playCard(mock(Card.class), mocked_player1);

        new Timer()
                .schedule(
                        new TimerTask() {
                            @Override
                            public void run() {
                                verify(trickMocked).getWinningPlayer();
                                verify(trickMocked).getValue();
                                verify(mocked_player1).cardsLeft();

                                verify(mocked_player2)
                                        .update(Updater.newOnTrickTakenResponse(new Player(null)));
                                verify(mocked_player2)
                                        .update(Updater.newOnGameBoardUpdate(null, null));
                            }
                        },
                        3000);
    }

    @Test
    public void playCardTest_NotLastCard() {
        Round round = new Round(game_withMockedPlayers, trickMocked, 1);
        when(trickMocked.getCardsPlayed()).thenReturn(1);

        round.playCard(mock(Card.class), mocked_player1);

        //        verify(mocked_player2).update(Updater.newCardPlayRequestResponse());not applicable
        // any more
        verify(mocked_player2).update(Updater.newOnGameBoardUpdate(null, null));
    }

    @Test
    public void allEstimatesSubmittedTest() {
        player1.makeEstimate(1);
        player2.makeEstimate(0);
        assertTrue(game.allEstimatesSubmitted());
    }

    @Test
    public void notAllEstimatesSubmittedTest() {
        player1.makeEstimate(1);
        assertFalse(game.allEstimatesSubmitted());
    }

    @Test
    public void CorrectCheatDiscoverySubmittedTest() {
        Updater updater = mock(Updater.class);
        player1.setUpdater(updater);
        player2.setUpdater(updater);

        int player2_points = player2.getPoints();
        int player1_points = player1.getPoints();

        player1.iHaveCHeatedFlag = true;
        game.cheatDiscoverySubmitted(player1, player2);

        assertEquals(player1.getPoints(), player1_points - 10);
        assertEquals(player2.getPoints(), player2_points + 30);
        assertFalse(player1.iHaveCHeatedFlag);
    }

    @Test
    public void FalseCheatDiscoverySubmittedTest() {
        Updater updater = mock(Updater.class);
        player1.setUpdater(updater);
        player2.setUpdater(updater);

        int player2_points = player2.getPoints();

        player1.iHaveCHeatedFlag = false;
        game.cheatDiscoverySubmitted(player1, player2);

        assertEquals(player2.getPoints(), player2_points - 10);
        assertTrue(player2.iHaveCHeatedFlag);
    }
}
