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
import com.github.wizard.game.Game;
import io.grpc.Grpc;
import io.grpc.ServerBuilder;
import io.grpc.ServerCredentials;
import io.grpc.TlsServerCredentials;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.stub.StreamObserver;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.tinylog.Level;
import org.tinylog.Logger;
import org.tinylog.configuration.Configuration;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

public class Server implements Callable<Integer> {
    private static final Map<Integer, Game> games = new HashMap<>();
    private int gameCounter = 0;
    private io.grpc.Server grpcServer;
    public static final int MAX_PLAYERS = 6;
    public static final int GAME_MOVE_TIMEOUT = 60000;

    @Option(
            names = {"-p", "--port"},
            defaultValue = "50051",
            description = "The port for the server to listen on (default = ${DEFAULT-VALUE})")
    private int port;

    @Option(
            names = {"-l", "--log-level"},
            defaultValue = "TRACE",
            description = "The logging level (default = ${DEFAULT-VALUE})")
    private Level level;

    static class TlsArgs {
        @Option(
                names = {"-c", "--cert"},
                required = true,
                description = "The certificate chain for the server.")
        private File certChain;

        @Option(
                names = {"-k", "--key"},
                required = true,
                description = "The key for the certificate chain.")
        private File privateKey;
    }

    @ArgGroup(exclusive = false)
    private TlsArgs tlsArgs;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Server()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        Configuration.set("level", level.toString());

        start();
        blockUntilShutdown();
        return 0;
    }

    private void start() throws IOException {
        ServerBuilder<?> serverBuilder;

        if (tlsArgs != null) {
            ServerCredentials serverCredentials =
                    TlsServerCredentials.newBuilder()
                            .keyManager(tlsArgs.certChain, tlsArgs.privateKey)
                            .build();
            serverBuilder = Grpc.newServerBuilderForPort(port, serverCredentials);
        } else {
            serverBuilder = ServerBuilder.forPort(port);
        }

        grpcServer =
                serverBuilder
                        .addService(new GameStarterImpl())
                        .addService(new GamePlayImpl())
                        .addService(new GameActionsImpl())
                        .addService(ProtoReflectionService.newInstance())
                        .build()
                        .start();

        Logger.info("Server ready. Port: {}", port);

        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(
                                () -> {
                                    Logger.error(
                                            "*** shutting down gRPC server since JVM is shutting"
                                                    + " down");
                                    try {
                                        Server.this.stop();
                                    } catch (InterruptedException e) {
                                        Logger.trace(e);
                                        Thread.currentThread().interrupt();
                                    }
                                    Logger.error("*** server shut down");
                                }));
    }

    private void stop() throws InterruptedException {
        if (grpcServer != null) {
            grpcServer.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (grpcServer != null) {
            grpcServer.awaitTermination();
        }
    }

    public static void removeGame(Game game) {
        games.remove(game.gameId, game);
    }

    class GameStarterImpl extends GameStarterGrpc.GameStarterImplBase {

        @Override
        public void startGame(StartRequest request, StreamObserver<StartReply> responseObserver) {
            Logger.info("start request received by {}", request.getName());
            Game newGame = new Game(++gameCounter);
            games.put(newGame.gameId, newGame);
            StartReply reply =
                    StartReply.newBuilder()
                            .setGameid(newGame.gameId + "")
                            .setPlayerid(
                                    newGame.addPlayer(
                                                    new com.github.wizard.game.Player(
                                                            request.getName()))
                                            + "")
                            .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        @Override
        public void joinGame(JoinRequest request, StreamObserver<StartReply> responseObserver) {
            Logger.info("join request received for game id {}", request.getGameid());
            Game newGame = games.get(Integer.valueOf(request.getGameid()));
            if (newGame == null || newGame.getNrPlayers() > MAX_PLAYERS || newGame.ready) {
                Logger.info(
                        "error for game with id {}: this game does not exist", request.getGameid());
                responseObserver.onNext(null);
                responseObserver.onCompleted();
                return;
            }
            StartReply reply =
                    StartReply.newBuilder()
                            .setGameid(newGame.gameId + "")
                            .setPlayerid(
                                    newGame.addPlayer(
                                                    new com.github.wizard.game.Player(
                                                            request.getName()))
                                            + "")
                            .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        /**
         * @param request a valid game request
         * @param responseObserver ReadyToJoin ready() when joining is possible
         */
        @Override
        public void checkJoinRequest(
                JoinRequest request, StreamObserver<ReadyToJoin> responseObserver) {
            Logger.debug("checkJoinRequest called");
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

        @Override
        public void getPlayers(JoinRequest request, StreamObserver<Player> responseObserver) {
            Logger.debug("getPlayers called");
            Game newGame = games.get(Integer.valueOf(request.getGameid()));
            if (newGame != null && newGame.getNrPlayers() < MAX_PLAYERS) {
                for (com.github.wizard.game.Player player : newGame.getPlayers()) {
                    if (player == null) break;
                    Logger.info("player: {}", player.getName());
                    Player grpcPlayer = Player.newBuilder().setName(player.getName()).build();
                    responseObserver.onNext(grpcPlayer);
                }

            } else {
                Logger.error("players requested for invalid game id: {}", request.getGameid());
                Player grpcPlayer = Player.newBuilder().setName("").build();
                responseObserver.onNext(grpcPlayer);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void setAsReady(JoinRequest request, StreamObserver<ReadyToJoin> responseObserver) {
            Logger.debug("setAsReady called");
            Game newGame = games.get(Integer.valueOf(request.getGameid()));
            if (newGame != null && newGame.getNrPlayers() < MAX_PLAYERS) {
                newGame.ready = true;
                ReadyToJoin reply = ReadyToJoin.newBuilder().setReady(true).build();
                responseObserver.onNext(reply);

            } else {
                Logger.error("invalid game");
                ReadyToJoin reply = ReadyToJoin.newBuilder().setReady(false).build();
                responseObserver.onNext(reply);
            }
            responseObserver.onCompleted();
        }
    }

    static class GameActionsImpl extends GameActionsGrpc.GameActionsImplBase {

        @Override
        public StreamObserver<GameMove> gameStream(StreamObserver<Response> responseObserver) {
            Logger.debug("gameStream called");
            return new StreamObserver<>() {
                Game newGame;
                com.github.wizard.game.Player player;

                @Override
                public void onNext(GameMove gameMove) {
                    if (newGame == null) {
                        Logger.info("player subscribed");
                        newGame = games.get(Integer.valueOf(gameMove.getGameid()));
                        player = newGame.getPlayers().get(Integer.parseInt(gameMove.getPlayerid()));
                        player.setUpdater(
                                new Updater(
                                        responseObserver)); // subscribe me for updates if I am new
                        // or
                        // connection was lost
                    }

                    // do whatever the gameAction was
                    Logger.info("gameMove: {}", gameMove.getType());
                    switch (gameMove.getType()) {
                        case "0" -> { // player subscribed
                            Logger.info("request to subscribe new player");
                            if (newGame
                                    .allPlayersSubscribed()) { // see if we are the last, then start
                                // handing out cards
                                Logger.info("game starting");
                                newGame.start();
                            }
                        }
                        case "1" -> { // submit estimates
                            int estimate = Integer.parseInt(gameMove.getData());
                            Logger.debug(
                                    "submitting estimate {} for player {}",
                                    estimate,
                                    player.getPlayerId());
                            player.makeEstimate(Integer.parseInt(gameMove.getData()));
                            if (newGame.allEstimatesSubmitted()) newGame.playFirstCard();
                        }
                        case "2" -> { // 2 is play card
                            int cardIndex = Integer.parseInt(gameMove.getData());
                            Logger.debug(
                                    "playing card {} for player {}",
                                    cardIndex,
                                    player.getPlayerId());
                            player.playCard(Integer.parseInt(gameMove.getData()));
                        }
                        case "3" -> { // player who might have cheated
                            Game game = newGame;
                            for (com.github.wizard.game.Player cheater : newGame.players) {
                                if (cheater.getName().equals(gameMove.getData())) {
                                    game.cheatDiscoverySubmitted(cheater, player);
                                }
                            }
                        }
                        default -> throw new IllegalArgumentException(
                                "This game Action is not yet implemented");
                    }
                }

                @Override
                public void onError(Throwable t) {
                    Logger.error("gamePlay cancelled");
                    Logger.trace(t);
                    // todo try to recover or kill game session if not possible
                }

                @Override
                public void onCompleted() {
                    // client will terminate subscription
                    responseObserver.onCompleted();
                    Logger.info("gameplay completed");
                }
            };
        }
    }
}
