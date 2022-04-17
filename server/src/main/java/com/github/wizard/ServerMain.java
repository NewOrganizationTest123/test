package com.github.wizard;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import com.github.wizard.api.GameStarterGrpc;
import com.github.wizard.api.JoinRequest;
import com.github.wizard.api.StartReply;
import com.github.wizard.api.StartRequest;


public class ServerMain {
    public static HashMap<Integer, Game> games = new HashMap<>();
    public static int gameCounter = 0;
    private Server server;

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

            StartReply reply = StartReply.newBuilder().setGameid(newGame.gameId + "").build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        @Override
        public void joinGame(JoinRequest request, StreamObserver<StartReply> responseObserver) {
            System.out.println("join request received for gameid " + request.getGameid());
            Game newGame = games.get(Integer.valueOf(request.getGameid()));
            if (newGame == null) {
                System.out.println("error for game with id " + request.getGameid() + ": this game does not exist");
                responseObserver.onNext(null);
                responseObserver.onCompleted();
            }
            StartReply reply = StartReply.newBuilder().setGameid(newGame.gameId + "").build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }

}
