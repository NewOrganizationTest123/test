package com.github.wizard;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

import com.github.wizard.api.Card;
import com.github.wizard.api.Response;
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
    void newOnTrickTakenResponse() {
        Response response = Updater.newOnTrickTakenResponse(new Player("player"), 13);

        assertEquals("1", response.getType());
        assertEquals("Player player has made this trick with value 13", response.getData());
    }

    @Test
    void newCardPlayRequestResponse() {
        Response response = Updater.newCardPlayRequestResponse();

        assertEquals("2", response.getType());
        assertEquals("Please play a card", response.getData());
    }

    Card wizard = Card.newBuilder().setColor(Card.Color.NONE).setValue(Card.Value.WIZARD).build();

    Card red1 = Card.newBuilder().setColor(Card.Color.RED).setValue(Card.Value.ONE).build();

    Card red3 = Card.newBuilder().setColor(Card.Color.RED).setValue(Card.Value.THREE).build();

    Card red13 = Card.newBuilder().setColor(Card.Color.RED).setValue(Card.Value.THIRTEEN).build();

    Card yellow10 = Card.newBuilder().setColor(Card.Color.YELLOW).setValue(Card.Value.TEN).build();

    Card green2 = Card.newBuilder().setColor(Card.Color.GREEN).setValue(Card.Value.TWO).build();

    Card green5 = Card.newBuilder().setColor(Card.Color.GREEN).setValue(Card.Value.FIVE).build();

    Card jester = Card.newBuilder().setColor(Card.Color.NONE).setValue(Card.Value.JESTER).build();

    @Test
    void newOnGameBoardUpdate() {
        List<Card> cards = List.of(wizard, red1, red3, red13, yellow10, green2, green5, jester);

        Response response = Updater.newOnGameBoardUpdate(cards, cards);

        assertEquals("3", response.getType());
        assertEquals(
                "/WIZARD/RED(1)/RED(3)/RED(13)/YELLOW(10)/GREEN(2)/GREEN(5)/JESTER//WIZARD/RED(1)/RED(3)/RED(13)/YELLOW(10)/GREEN(2)/GREEN(5)/JESTER/",
                response.getData());
    }

    @Test
    void newOnTrumpSelectedResponse() {
        Response response = Updater.newOnTrumpSelectedResponse(red1);

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
