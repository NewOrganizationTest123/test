package com.github.wizard;

import com.github.wizard.api.Card;
import com.github.wizard.api.CardList;
import com.github.wizard.api.CheatingSubmittedResult;
import com.github.wizard.api.GameStatus;
import com.github.wizard.api.GrpcPlayer;
import com.github.wizard.api.PlayersList;
import com.github.wizard.api.Response;
import com.github.wizard.api.StichMade;
import com.github.wizard.game.Player;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import org.tinylog.Logger;

public record Updater(StreamObserver<Response> responseStreamObserver) {

    public void update(Response response) {
        responseStreamObserver.onNext(response);
    }

    public static Response newOnTrickTakenResponse(Player player) {
        return Response.newBuilder()
                .setType("1")
                .setData(String.format("Player %s has made this trick", player.getName()))
                .setStichMade(
                        StichMade.newBuilder()
                                .setPlayerid(player.getPlayerId() + "")
                                .setPlayerName(player.getName())
                                .setTotalstichebyplayer(player.getTakeTrick() + "")
                                .build())
                .build();
    }

    public static Response newCardPlayRequestResponse() {
        return Response.newBuilder().setType("2").setData("Please play a card").build();
    }

    public static Response newOnGameBoardUpdate(
            List<Card> hand, List<Card> table, String nextPlayer) {
        if (hand == null) hand = new ArrayList<>();
        if (table == null) table = new ArrayList<>();

        Logger.info(
                "sending out cards in hand: {} and on table: {}",
                hand.stream().toString(),
                table.stream().toString());
        return Response.newBuilder()
                .setType("3")
                .setCardList(
                        CardList.newBuilder()
                                .addAllHand(hand)
                                .addAllTable(table)
                                .setTurn(nextPlayer)
                                .build())
                .build();
    }

    public static Response newOnTrumpSelectedResponse(Card c) {
        return Response.newBuilder().setType("4").setData(c.getColor().name()).build();
    }

    public static Response newGetEstimateResponse() {
        return Response.newBuilder().setType("5").build();
    }

    public static Response newOnRoundFinishedResponse(List<GrpcPlayer> playerList, int round) {
        return Response.newBuilder()
                .setType("6")
                .setData("/" + round)
                .setGameStatus(
                        GameStatus.newBuilder()
                                .setRound(round + "")
                                .addAllPlayers(playerList)
                                .build())
                .build();
    }

    public static Response newOnCheatingSubmittedResponse(
            Player cheater, boolean succesfulOrNot, List<GrpcPlayer> playerList) {
        return Response.newBuilder()
                .setCheating(
                        CheatingSubmittedResult.newBuilder()
                                .setCheaterId(cheater.getPlayerId() + "")
                                .addAllPlayers(playerList)
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
                            .setPoints(p.getPoints() + "")
                            .build());
        return Response.newBuilder()
                .setPlayerList(PlayersList.newBuilder().addAllPlayer(temp).build())
                .build();
    }

    public static Response newEndGameResponse() {
        return Response.newBuilder().setType("7").build();
    }

    public static Response newRandomEstimateCalcuatedResponse(String randomEstimate) {
        return Response.newBuilder().setType("8").setData(randomEstimate).build();
    }

    public static Response newRandomCardPlayedResponse() {
        return Response.newBuilder().setType("9").build();
    }
}
