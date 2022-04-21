package com.github.wizard;

import com.github.wizard.api.GamePlayGrpc;
import com.github.wizard.api.GameStarterGrpc;
import com.github.wizard.api.JoinRequest;
import com.github.wizard.api.Player;
import com.github.wizard.api.ReadyToJoin;
import com.github.wizard.api.StartReply;
import com.github.wizard.api.StartRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;


public class ServerMain {
    public static HashMap<Integer, Game> games = new HashMap<>();
    public static int gameCounter = 0;
    private Server server;
    public final static int MAX_PLAYERS=6;

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Welcome user!");
        final ServerMain server = new ServerMain();
        server.start();
        server.blockUntilShutdown();
    }

    private void start() throws IOException {
        int port = 50051;
        server = ServerBuilder.forPort(port)
                .addService(new GameStarterImpl())
                .addService(new GamePlayImpl())
                .build()
                .start();

        System.out.println("Server ready. Port:  " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            try {
                ServerMain.this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
            System.err.println("*** server shut down");
        }));
    }

    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    static class GameStarterImpl extends GameStarterGrpc.GameStarterImplBase {

        @Override
        public void startGame(StartRequest request, StreamObserver<StartReply> responseObserver) {
            System.out.println("start request received by " + request.getName());
            Game newGame = new Game(++gameCounter);
            games.put(newGame.gameId, newGame);
            newGame.addPlayer(new com.github.wizard.Player(request.getName()));

            StartReply reply = StartReply.newBuilder().setGameid(newGame.gameId + "").build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        @Override
        public void joinGame(JoinRequest request, StreamObserver<StartReply> responseObserver) {
            System.out.println("join request received for gameid " + request.getGameid());
            Game newGame = games.get(Integer.valueOf(request.getGameid()));
            if (newGame == null || newGame.getPlayerArrayList().size() > MAX_PLAYERS || newGame.ready) {
                System.out.println("error for game with id " + request.getGameid() + ": this game does not exist");
                responseObserver.onNext(null);
                responseObserver.onCompleted();
                return;
            }
            newGame.addPlayer(new com.github.wizard.Player(request.getName()));
            StartReply reply = StartReply.newBuilder().setGameid(newGame.gameId + "").build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }


        /**
         * @param request          a valid game request
         * @param responseObserver ReadyToJoin ready() when joining is possible
         */
        @Override
        public void checkJoinRequest(JoinRequest request, StreamObserver<ReadyToJoin> responseObserver) {
            System.out.println("checkGame");
            Game newGame = games.get(Integer.valueOf(request.getGameid()));
            if (newGame == null || newGame.getPlayerArrayList().size() > MAX_PLAYERS || newGame.ready) {
                ReadyToJoin reply = ReadyToJoin.newBuilder().setReady(false).build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            } else {
                ReadyToJoin reply = ReadyToJoin.newBuilder().setReady(true).build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            }
        }

    }

    static class GamePlayImpl extends GamePlayGrpc.GamePlayImplBase {

        /**
         * @param request
         * @param responseObserver
         */
        @Override
        public void getPlayers(JoinRequest request, StreamObserver<Player> responseObserver) {
            System.out.println("getPlayers");
            Game newGame = games.get(Integer.valueOf(request.getGameid()));
            if (newGame != null && newGame.getPlayerArrayList().size() < MAX_PLAYERS) {
                for (com.github.wizard.Player player : newGame.getPlayerArrayList()) {
                    Player grpcPlayer = Player.newBuilder().setName(player.name).build();
                    responseObserver.onNext(grpcPlayer);
                }

            } else {
                System.out.println("players requested for invalid game: gameid=" + request.getGameid());
                Player grpcPlayer = Player.newBuilder().setName("").build();
                responseObserver.onNext(grpcPlayer);
            }
            responseObserver.onCompleted();

        }

        /**
         * @param request
         * @param responseObserver
         */
        @Override
        public void setAsReady(JoinRequest request, StreamObserver<ReadyToJoin> responseObserver) {
            System.out.println("setAsReady");
            Game newGame = games.get(Integer.valueOf(request.getGameid()));
            if (newGame != null && newGame.getPlayerArrayList().size() < MAX_PLAYERS) {
                newGame.ready = true;
                ReadyToJoin reply = ReadyToJoin.newBuilder().setReady(true).build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();

            } else {
                System.out.println("invalid game");
                ReadyToJoin reply = ReadyToJoin.newBuilder().setReady(false).build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            }
        }
    }
}
