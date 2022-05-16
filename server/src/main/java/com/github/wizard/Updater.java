package com.github.wizard;

import com.github.wizard.api.Card;
import com.github.wizard.api.CardList;
import com.github.wizard.api.CheatingSubmittedResult;
import com.github.wizard.api.GameStatus;
import com.github.wizard.api.GrpcPlayer;
import com.github.wizard.api.PlayersList;
import com.github.wizard.api.Response;
import com.github.wizard.game.Player;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import org.tinylog.Logger;

public record Updater(StreamObserver<Response> responseStreamObserver) {

    public void update(Response response) {
        responseStreamObserver.onNext(response);
    }

    public static Response newOnTrickTakenResponse(Player player, int value) {
        return Response.newBuilder()
                .setType("1")
                .setData(
                        String.format(
                                "Player %s has made this trick with value %s",
                                player.getName(), value))
                .build();
    }

    public static Response newCardPlayRequestResponse() {
        return Response.newBuilder().setType("2").setData("Please play a card").build();
    }

    public static Response newOnGameBoardUpdate(List<Card> hand, List<Card> table) {
        if (hand == null) hand = new ArrayList<>();
        if (table == null) table = new ArrayList<>();

        Logger.info(
                "sending out cards in hand: {} and on table: {}",
                hand.stream().toString(),
                table.stream().toString());
        return Response.newBuilder()
                .setType("3")
                .setCardList(CardList.newBuilder().addAllHand(hand).addAllTable(table).build())
                .build();
    }

    public static Response newOnTrumpSelectedResponse(Card c) {
        return Response.newBuilder().setType("4").setData(c.getColor().name()).build();
    }

    public static Response newGetEstimateResponse() {
        return Response.newBuilder().setType("5").build();
    }

    public static Response newOnRoundFinishedResponse(int points, int round) {
        return Response.newBuilder()
                .setType("6")
                .setData(points + "/" + round)
                .setGameStatus(
                        GameStatus.newBuilder()
                                .setRound(round + "")
                                .setMyPoints(points + "")
                                .build())
                .build();
    }

    public static Response newOnCheatingSubmittedResponse(
            Player cheater, boolean succesfulOrNot, int points) {
        return Response.newBuilder()
                .setCheating(
                        CheatingSubmittedResult.newBuilder()
                                .setCheaterId(cheater.getPlayerId() + "")
                                .setNewPoints(points + "")
                                .setSuccesfulOrNot(succesfulOrNot + "")
                                .build())
                .build();
    }

    public static Response newGetPlayersResponse(Player.Players players) {
        ArrayList<GrpcPlayer> temp = new ArrayList();
        for (Player p : players)
            temp.add(
                    GrpcPlayer.newBuilder()
                            .setPlayerName(p.getName())
                            .setPlayerId(p.getPlayerId() + "")
                            .build());
        return Response.newBuilder()
                .setPlayerList(PlayersList.newBuilder().addAllPlayer(temp).build())
                .build();
    }
}
