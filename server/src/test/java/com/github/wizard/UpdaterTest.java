package com.github.wizard;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

import com.github.wizard.api.Response;
import com.github.wizard.game.Card;
import com.github.wizard.game.Color;
import com.github.wizard.game.Player;
import io.grpc.stub.StreamObserver;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdaterTest {

    @Mock private StreamObserver<Response> streamObserver;

    @InjectMocks private Updater mockedUpdater;

    @Test
    void update() {
        Response response = Updater.newOnRoundFinishedResponse(0, 1);
        mockedUpdater.update(response);

        verify(streamObserver).onNext(response);
    }

    @Test
    void newOnStichMadeResponse() {
        Response response = Updater.newOnStichMadeResponse(new Player("player"), 13);

        assertEquals("1", response.getType());
        assertEquals("Player player has made this stich with value 13", response.getData());
    }

    @Test
    void newCardPlayRequestResponse() {
        Response response = Updater.newCardPlayRequestResponse();

        assertEquals("2", response.getType());
        assertEquals("Please play a card", response.getData());
    }

    @Test
    void newOnGameBoardUpdate() {
        List<Card> cards =
                List.of(
                        new Card(Color.YELLOW, 10),
                        new Card(null, Integer.MAX_VALUE),
                        new Card(null, -1),
                        new Card(Color.RED, 1),
                        new Card(Color.BLUE, 3),
                        new Card(Color.GREEN, 13));

        Response response = Updater.newOnGameBoardUpdate(cards, cards);

        assertEquals("3", response.getType());
        assertEquals(
                "/YELLOW(10)/Wizard/Jester/RED(1)/BLUE(3)/GREEN(13)//YELLOW(10)/Wizard/Jester/RED(1)/BLUE(3)/GREEN(13)/",
                response.getData());
    }

    @Test
    void newOnTrumpfSelectedResponse() {
        Response response = Updater.newOnTrumpfSelectedResponse(Color.RED);

        assertEquals("4", response.getType());
        assertEquals("RED", response.getData());
    }

    @Test
    void newGetEstimateResponse() {

        Response response = Updater.newGetEstimateResponse();
        assertEquals("5", response.getType());
    }

    @Test
    void newOnRoundFinishedResponse() {
        Response response = Updater.newOnRoundFinishedResponse(10, 4);
        assertEquals("6", response.getType());
        assertEquals("10/4", response.getData());
    }
}
