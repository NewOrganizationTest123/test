package com.github.wizard;

import com.github.wizard.api.GameActionsGrpc;
import com.github.wizard.api.GameMove;
import com.github.wizard.api.GamePlayGrpc;
import com.github.wizard.api.GameStarterGrpc;
import com.github.wizard.api.JoinRequest;
import com.github.wizard.api.Player;
import com.github.wizard.api.ReadyToJoin;
import com.github.wizard.api.Response;
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
    public final static int MAX_PLAYERS = 6;
    public static final Card[] cards = {new Card(Color.RED, -1), new Card(Color.RED, Integer.MAX_VALUE), new Card(Color.RED, 5), new Card(Color.RED, 10), new Card(Color.RED, 12)};

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
                .addService(new GameActiontyIml())
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
            StartReply reply = StartReply.newBuilder().setGameid(newGame.gameId + "").setPlayerid(newGame.addPlayer(new com.github.wizard.Player(request.getName())) + "").build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        @Override
        public void joinGame(JoinRequest request, StreamObserver<StartReply> responseObserver) {
            System.out.println("join request received for gameid " + request.getGameid());
            Game newGame = games.get(Integer.valueOf(request.getGameid()));
            if (newGame == null || newGame.getNrPlayers() > MAX_PLAYERS || newGame.ready) {
                System.out.println("error for game with id " + request.getGameid() + ": this game does not exist");
                responseObserver.onNext(null);
                responseObserver.onCompleted();
                return;
            }
            StartReply reply = StartReply.newBuilder().setGameid(newGame.gameId + "").setPlayerid(newGame.addPlayer(new com.github.wizard.Player(request.getName())) + "").build();
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
            if (newGame == null || newGame.getNrPlayers() > MAX_PLAYERS || newGame.ready) {
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
            if (newGame != null && newGame.getNrPlayers() < MAX_PLAYERS) {
                for (com.github.wizard.Player player : newGame.getPlayerArrayList()) {
                    if (player == null)
                        break;
                    System.out.println("player: " + player.name);
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
            if (newGame != null && newGame.getNrPlayers() < MAX_PLAYERS) {
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

    static class GameActiontyIml extends GameActionsGrpc.GameActionsImplBase {

        /**
         * @param responseObserver
         */
        @Override
        public StreamObserver<GameMove> gameStream(StreamObserver<Response> responseObserver) {
            System.out.println("gameStream");
            return new StreamObserver<GameMove>() {
                Game newGame;
                com.github.wizard.Player player;

                @Override
                public void onNext(GameMove gameMove) {
                    if (newGame == null) {
                        System.out.println("player subscribed");
                        newGame = games.get(Integer.valueOf(gameMove.getGameid()));
                        player = newGame.getPlayerArrayList()[Integer.parseInt(gameMove.getPlayerid())];
                        player.responseObserver = responseObserver;//subscribe me for updates if I am new or connection was lost
                    }
                    //do whatever the gameAction was
                    System.out.println("gameMove: " + gameMove.getType());
                    switch (gameMove.getType()) {
                        case "0"://player subscribed
                            System.out.println("request to subscribe new player");
                            // TODO: 22.04.2022 check
                            if (newGame.allPlayersSubscribed()) {//see if we are the last, then start handing out cards
                                System.out.println("we are starting the game");
                                newGame.startNewRound();
                            }
                            break;
                        case "1": // submit estimates
                            newGame.getCurrentRound().estimates[player.playerId] = Integer.parseInt(gameMove.getData());
                            break;
                        case "2"://2 is play card
                            newGame.playCard(player.getCard(Integer.parseInt(gameMove.getData())), player);//retrieve card he/she wanted to play and play it
                            break;
                        default:
                            throw new IllegalArgumentException("This game Action is not yet implemented");
                    }

                }

                @Override
                public void onError(Throwable t) {
                    System.out.println("gamePlay cancelled");
                    t.printStackTrace();
                    //todo try to recover or kill game session if not possible
                }

                @Override
                public void onCompleted() {
                    //client will terminate subscription
                    responseObserver.onCompleted();
                }
            };
        }
    }
}
