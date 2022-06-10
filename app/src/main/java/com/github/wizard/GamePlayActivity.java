package com.github.wizard;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.github.wizard.api.Card;
import com.github.wizard.api.CardList;
import com.github.wizard.api.CheatingSubmittedResult;
import com.github.wizard.api.GameActionsGrpc;
import com.github.wizard.api.GameMove;
import com.github.wizard.api.GameStatus;
import com.github.wizard.api.GrpcPlayer;
import com.github.wizard.api.Response;
import com.github.wizard.api.StichMade;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GamePlayActivity extends AppCompatActivity {

    public static String gameId;
    public static String playerId;
    public static String playername;
    private static int myPoints = 0;
    private static List<ClientPlayer> players = new ArrayList<>();
    public static PlayersRecyclerviewAdapter players_adapter;
    public static CardsInHandRecyclerViewAdapter cards_adapter;
    public static CardsInTheMiddleRecyclerViewAdapter cards_middle_adapter;

    ManagedChannel channel;
    private static final BlockingQueue<GameMove> serverWaitingQueue = new LinkedBlockingQueue<>();
    private SensorManager sensorManager;
    private double deviceAcceleration;
    private double deviceAcceleration_before;
    private double deviceAcceleration_now;
    private View cheatsView;
    private RecyclerView playersRecyclerView;
    private Button closeCheatsViewButton;
    private TextView cheatsViewTitle;
    private TextView points;
    private RecyclerView cardsInHandRecyclerView;
    private RecyclerView cardsInTheMiddleRecyclerView;
    private TextView whosTurnIsItText;
    private int numberOfStitchesMade = 0;

    private int cardPlayTimerProgress;
    private CardsInHandRecyclerViewAdapter playcardadapter;
    private ArrayList<String> cards;

    private static void appendLogs(StringBuffer logs, String msg, Object... params) {
        if (params.length > 0) {
            logs.append(MessageFormat.format(msg, params));
        } else {
            logs.append(msg);
        }
        logs.append("\n");
    }

    private static GameMove newGameMove(int type, String message) {
        return GameMove.newBuilder()
                .setGameid(GamePlayActivity.gameId)
                .setPlayerid(GamePlayActivity.playerId)
                .setData(message)
                .setType(type + "")
                .build();
    }

    public static List<ClientPlayer> getPlayers() {
        return players;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_play);
        Intent intent = getIntent();
        gameId = intent.getStringExtra(MainActivity.GAME_ID_KEY); // reuse for later requests
        playerId = intent.getStringExtra(MainActivity.PLAYER_ID_KEY);
        playername = intent.getStringExtra(MainActivity.PLAYER_NAME);
        new GameActionRunner(new GameActionRunnable(), new WeakReference<>(this), channel)
                .execute(); // fire up the streaming service

        points = findViewById(R.id.points);
        whosTurnIsItText = findViewById(R.id.whosTurnIsItTextview);
        cheatsView = findViewById(R.id.ExposeCheatsView);
        cheatsView.bringToFront();
        playersRecyclerView = findViewById(R.id.playerRecyclerView);
        playersRecyclerView.bringToFront();
        closeCheatsViewButton = findViewById(R.id.closeCheatsViewButton);
        closeCheatsViewButton.bringToFront();
        cheatsViewTitle = findViewById(R.id.cheatingViewTitle);
        cheatsViewTitle.bringToFront();
        hideCheatingExposingView(); // by default, the cheating-exposing view is not visible; only
        // shows up after shaking device

        closeCheatsViewButton.setOnClickListener(
                e -> {
                    hideCheatingExposingView();
                });

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Objects.requireNonNull(sensorManager)
                .registerListener(
                        sensorListener,
                        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                        SensorManager.SENSOR_DELAY_NORMAL);
        deviceAcceleration = 10;
        deviceAcceleration_before = SensorManager.GRAVITY_EARTH;
        deviceAcceleration_now = SensorManager.GRAVITY_EARTH;

        LinearLayoutManager layoutManagerPlayers = new LinearLayoutManager(this);
        playersRecyclerView.setLayoutManager(layoutManagerPlayers);

        players_adapter = new PlayersRecyclerviewAdapter(this, players);
        playersRecyclerView.setAdapter(players_adapter);

        cardsInHandRecyclerView = findViewById(R.id.cardsInHandRecyclerview);
        LinearLayoutManager layoutManagerCards =
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        cardsInHandRecyclerView.setLayoutManager(layoutManagerCards);

        ArrayList<String> cardsList = new ArrayList<>();
        cards_adapter = new CardsInHandRecyclerViewAdapter(this, cardsList);
        cardsInHandRecyclerView.setAdapter(players_adapter);

        cardsInTheMiddleRecyclerView = findViewById(R.id.cardsInTheMiddleRecyclerView);
        LinearLayoutManager layoutManagerCardsMiddle =
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        cardsInTheMiddleRecyclerView.setLayoutManager(layoutManagerCardsMiddle);

        ArrayList<String> cardsMiddleList = new ArrayList<>();
        cards_middle_adapter = new CardsInTheMiddleRecyclerViewAdapter(this, cardsMiddleList);
        cardsInTheMiddleRecyclerView.setAdapter(cards_middle_adapter);
    }

    public void showCheatingExposingView() {
        cheatsView.setVisibility(View.VISIBLE);
        playersRecyclerView.setVisibility(View.VISIBLE);
        closeCheatsViewButton.setVisibility(View.VISIBLE);
        cheatsViewTitle.setVisibility(View.VISIBLE);
    }

    public void hideCheatingExposingView() {
        cheatsView.setVisibility(View.GONE);
        playersRecyclerView.setVisibility(View.GONE);
        closeCheatsViewButton.setVisibility(View.GONE);
        cheatsViewTitle.setVisibility(View.GONE);
    }

    public void exposeCheating(String playername) {
        serverWaitingQueue.add(newGameMove(3, playername));
    }

    public void updatePlayersInRecyclerView(List<ClientPlayer> realplayers) {
        players = realplayers; // include myself for scoreboard
        // remove myself
        for (ClientPlayer cPlayer : realplayers) {
            if (cPlayer.getId().equals(playerId)) {
                realplayers.remove(cPlayer);
                break;
            }
        }
        PlayersRecyclerviewAdapter newdapater = new PlayersRecyclerviewAdapter(this, realplayers);
        playersRecyclerView.setAdapter(newdapater);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(
                sensorListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorListener);
    }

    private final SensorEventListener sensorListener =
            new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent sensorEvent) {
                    double x_axis = sensorEvent.values[0];
                    double y_axis = sensorEvent.values[1];
                    double z_axis = sensorEvent.values[2];
                    deviceAcceleration_before = deviceAcceleration_now;
                    deviceAcceleration_now =
                            Math.sqrt(x_axis * x_axis + y_axis * y_axis + z_axis * z_axis);
                    double delta = deviceAcceleration_now - deviceAcceleration_before;
                    deviceAcceleration = deviceAcceleration * 0.9 + delta;

                    if (deviceAcceleration > 10) {
                        showCheatingExposingView();
                    }
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int i) {}
            };

    private void playCard(String cardnum) {
        ProgressBar cardPlayTimeout = findViewById(R.id.cardPlayTimeout);
        cardPlayTimeout.setVisibility(View.INVISIBLE);
        whosTurnIsItText.setText("Please wait for your turn!");
        serverWaitingQueue.add(newGameMove(2, cardnum));
    }

    private void updateEstimateTextview(String estimate) {
        TextView estimateTextView = findViewById(R.id.stiche_estimated);
        estimateTextView.setText("You wanted to make " + estimate + " Stiche");
    }

    private void submitEstimate(String estimate) {
        serverWaitingQueue.add(newGameMove(1, estimate));
    }

    private void openEstimateDialog() {
        EstimateDialog estimateDialog = new EstimateDialog(this);
        estimateDialog.setContentView(R.layout.estimate_dialog);
        estimateDialog.setCancelable(false);
        estimateDialog.show();
        numberOfStitchesMade = 0;
    }

    /**
     * this should show the final score board and end the game session. This is the last call of the
     * game
     */
    private void showGameResults(Activity activity) {
        // TODO: 26.05.2022 Silvio, show your scoreboard whith winning player highlighted when this
        // is called, forward to home screen when scoreboard is closed
        Toast.makeText(
                        activity.getApplication().getApplicationContext(),
                        "The game has ended! Good bye",
                        Toast.LENGTH_SHORT)
                .show();
    }

    private void updateCardsInHandRecyclerView(ArrayList<String> cards_in_hand) {
        CardsInHandRecyclerViewAdapter newcards_adapter =
                new CardsInHandRecyclerViewAdapter(this, cards_in_hand);
        cardsInHandRecyclerView.setAdapter(newcards_adapter);
    }

    private void updateCardsInMiddleRecyclerView(ArrayList<String> cards_in_middle) {
        CardsInTheMiddleRecyclerViewAdapter newcards_adapter =
                new CardsInTheMiddleRecyclerViewAdapter(this, cards_in_middle);
        cardsInTheMiddleRecyclerView.setAdapter(newcards_adapter);
    }

    private void allowPlayingCard() {
        // TODO: only allow playing Card when CardPlayRequest
        CountDownTimer cardPlayTimer;
        playcardadapter = (CardsInHandRecyclerViewAdapter) cardsInHandRecyclerView.getAdapter();
        playcardadapter.activatePlayingCard();
        whosTurnIsItText.setText("Its your turn!");
        ProgressBar cardPlayTimeout = findViewById(R.id.cardPlayTimeout);
        cardPlayTimeout.setVisibility(View.VISIBLE);
        cardPlayTimerProgress = 0;
        cardPlayTimeout.setProgress(cardPlayTimerProgress);

        cardPlayTimer =
                new CountDownTimer(60000, 1000) {
                    @Override
                    public void onTick(long l) {
                        cardPlayTimeout.setProgress(cardPlayTimerProgress * 100 / (60000 / 1000));
                        cardPlayTimerProgress++;
                    }

                    @Override
                    public void onFinish() {
                        cardPlayTimeout.setProgress(100);
                        Log.i("Wizzard", "Client timer play card timed out");
                        cardPlayTimeout.setVisibility(View.INVISIBLE);
                    }
                };
        cardPlayTimer.start();
    }

    public void updateNumberOfStichesTextview() {
        TextView stiche = findViewById(R.id.stiche_made);
        stiche.setText("You habe already made " + numberOfStitchesMade + " Stiche");
    }

    private class GameActionRunnable implements GrpcRunnableNew {
        private Throwable failed;

        @Override
        public String run(
                GameActionsGrpc.GameActionsBlockingStub blockingStub,
                GameActionsGrpc.GameActionsStub asyncStub,
                WeakReference<Activity> activityReference)
                throws Exception {
            return updateGameBoard(asyncStub, activityReference);
        }

        @Override
        public void doWhenDone(WeakReference<Activity> activityReference) {}

        /**
         * Bi-directional example, which can only be asynchronous. Send some chat messages, and
         * print any chat messages that are sent from the server.
         */
        private String updateGameBoard(
                GameActionsGrpc.GameActionsStub asyncStub,
                WeakReference<Activity> activityReference)
                throws InterruptedException, RuntimeException {
            final StringBuffer logs = new StringBuffer();
            appendLogs(logs, "*** GamePlay");
            final CountDownLatch finishLatch = new CountDownLatch(1);
            StreamObserver<GameMove> requestObserver =
                    asyncStub.gameStream(
                            /**
                             * Receives a value from the stream.
                             *
                             * <p>Can be called many times but is never called after {@link
                             * #onError(Throwable)} or {@link #onCompleted()} are called.
                             *
                             * <p>Unary calls must invoke onNext at most once. Clients may invoke
                             * onNext at most once for server streaming calls, but may receive many
                             * onNext callbacks. Servers may invoke onNext at most once for client
                             * streaming calls, but may receive many onNext callbacks.
                             *
                             * <p>If an exception is thrown by an implementation the caller is
                             * expected to terminate the stream by calling {@link
                             * #onError(Throwable)} with the caught exception prior to propagating
                             * it.
                             *
                             * @param value the value passed to the stream
                             */
                            new StreamObserver<Response>() {

                                private void showStich(Activity activity, StichMade stichmade) {
                                    if (stichmade
                                            .getPlayerid()
                                            .equals(playerId)) { // I made the stich

                                        numberOfStitchesMade++;
                                        updateNumberOfStichesTextview();

                                        Toast.makeText(
                                                        activity.getApplication()
                                                                .getApplicationContext(),
                                                        "You have made this stich!",
                                                        Toast.LENGTH_SHORT)
                                                .show();
                                        ((TextView) activity.findViewById(R.id.stiche_made))
                                                .setText(
                                                        "You have already made "
                                                                + stichmade.getTotalstichebyplayer()
                                                                + " Stiche");

                                    } else // someone else made the stich
                                    Toast.makeText(
                                                        activity.getApplication()
                                                                .getApplicationContext(),
                                                        "player "
                                                                + stichmade.getPlayerName()
                                                                + " has made this stich. They have"
                                                                + " made a total of "
                                                                + stichmade.getTotalstichebyplayer()
                                                                + " stich",
                                                        Toast.LENGTH_SHORT)
                                                .show();
                                }

                                private void makeCardPlayRequest(
                                        Activity activity, Response response) {
                                    allowPlayingCard();
                                }

                                private void updateGameField(Activity activity, CardList cardList) {
                                    // update cards in hand
                                    StringBuilder builderHand = new StringBuilder();

                                    ArrayList<String> cards_in_hand = new ArrayList<>();
                                    for (Card c : cardList.getHandList()) {
                                        String cardname = c.getColor() + c.getValue().toString();
                                        cards_in_hand.add(cardname);
                                    }

                                    updateCardsInHandRecyclerView(cards_in_hand);

                                    ((TextView) activity.findViewById(R.id.cards_on_table))
                                            .setText("");

                                    ArrayList<String> cards_in_middle = new ArrayList<>();
                                    for (Card c : cardList.getTableList()) {
                                        String cardname = c.getColor() + c.getValue().toString();
                                        cards_in_middle.add(cardname);
                                    }
                                    updateCardsInMiddleRecyclerView(cards_in_middle);

                                    cards = cards_in_hand;
                                }

                                private void showTrump(Activity activity, Response response) {
                                    ((TextView) activity.findViewById(R.id.trumpf))
                                            .setText("Trumpf is " + response.getData());

                                    switch (response.getData()) {
                                        case "GREEN":
                                            ((TextView) activity.findViewById(R.id.trumpf))
                                                    .setTextColor(
                                                            getResources().getColor(R.color.green));
                                            break;
                                        case "BLUE":
                                            ((TextView) activity.findViewById(R.id.trumpf))
                                                    .setTextColor(
                                                            getResources().getColor(R.color.blue));
                                            break;
                                        case "RED":
                                            ((TextView) activity.findViewById(R.id.trumpf))
                                                    .setTextColor(
                                                            getResources().getColor(R.color.red));
                                            break;
                                        case "YELLOW":
                                            ((TextView) activity.findViewById(R.id.trumpf))
                                                    .setTextColor(
                                                            getResources()
                                                                    .getColor(R.color.yellow));
                                            break;
                                        default:
                                            ((TextView) activity.findViewById(R.id.trumpf))
                                                    .setTextColor(
                                                            getResources().getColor(R.color.white));
                                            break;
                                    }

                                    Toast.makeText(
                                                    activity.getApplication()
                                                            .getApplicationContext(),
                                                    "trumpf is: " + response.getData(),
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                }

                                private void makeEstimate(Activity activity, Response response) {
                                    openEstimateDialog();
                                }

                                private void updateRoundNumberAndPoints(
                                        Activity activity, GameStatus gameStatus) {
                                    // reset stich counter to 0 when new round starts
                                    ((TextView) activity.findViewById(R.id.stiche_made))
                                            .setText("You have already made 0 Stiche");

                                    for (GrpcPlayer grpcPlayer : gameStatus.getPlayersList()) {
                                        // update my points
                                        if (playerId.equals(grpcPlayer.getPlayerId()))
                                            myPoints = Integer.parseInt(grpcPlayer.getPoints());
                                        // update points for other people
                                        for (ClientPlayer cPlayer : players) {
                                            if (cPlayer.getId().equals(grpcPlayer.getPlayerId())) {
                                                cPlayer.setPoints(grpcPlayer.getPoints());
                                            }
                                        }
                                    }
                                    ((TextView) activity.findViewById(R.id.points))
                                            .setText("You have " + myPoints + " points");

                                    int roundNr = Integer.parseInt(gameStatus.getRound()) + 1;
                                    ((TextView) activity.findViewById(R.id.round))
                                            .setText("This is round " + roundNr);

                                    Toast.makeText(
                                                    activity.getApplication()
                                                            .getApplicationContext(),
                                                    "after round "
                                                            + gameStatus.getRound()
                                                            + " you have "
                                                            + myPoints
                                                            + " points!",
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                }

                                private void youCheated(
                                        Activity activity,
                                        CheatingSubmittedResult cheatingSubmittedResult) {

                                    for (GrpcPlayer grpcPlayer :
                                            cheatingSubmittedResult.getPlayersList()) {
                                        // update my points
                                        if (playerId.equals(grpcPlayer.getPlayerId()))
                                            myPoints = Integer.parseInt(grpcPlayer.getPoints());
                                        // update points for other people
                                        for (ClientPlayer cPlayer : players) {
                                            if (cPlayer.getId().equals(grpcPlayer.getPlayerId())) {
                                                cPlayer.setPoints(grpcPlayer.getPoints());
                                            }
                                        }
                                    }

                                    ((TextView) activity.findViewById(R.id.points))
                                            .setText("You have " + myPoints + " points");
                                    Toast.makeText(
                                                    activity.getApplication()
                                                            .getApplicationContext(),
                                                    "Your cheating was discovered!!!! You now have "
                                                            + myPoints
                                                            + " Points",
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                }

                                private void someOneCheated(
                                        Activity activity,
                                        CheatingSubmittedResult
                                                cheatingSubmittedResult) { // we will not get our id
                                    // because submitting
                                    // cheaters is anonymous
                                    String currentPointsString = points.getText().toString();
                                    Pattern pattern = Pattern.compile("\\d+");
                                    Matcher matcher = pattern.matcher(currentPointsString);

                                    int currentPoints = 0;

                                    while (matcher.find()) {
                                        currentPoints = Integer.parseInt(matcher.group());
                                    }
                                    for (GrpcPlayer grpcPlayer :
                                            cheatingSubmittedResult.getPlayersList()) {
                                        // update my points
                                        if (playerId.equals(grpcPlayer.getPlayerId()))
                                            myPoints = Integer.parseInt(grpcPlayer.getPoints());
                                        // update points for other people
                                        for (ClientPlayer cPlayer : players) {
                                            if (cPlayer.getId().equals(grpcPlayer.getPlayerId())) {
                                                cPlayer.setPoints(grpcPlayer.getPoints());
                                            }
                                        }
                                    }

                                    if (currentPoints > myPoints) {
                                        Toast.makeText(
                                                        activity.getApplication()
                                                                .getApplicationContext(),
                                                        "Wrong assumption! You now have "
                                                                + myPoints
                                                                + " Points",
                                                        Toast.LENGTH_SHORT)
                                                .show();
                                    } else {
                                        Toast.makeText(
                                                        activity.getApplication()
                                                                .getApplicationContext(),
                                                        "Someone cheated! You now have "
                                                                + myPoints
                                                                + " Points",
                                                        Toast.LENGTH_SHORT)
                                                .show();
                                    }

                                    ((TextView) activity.findViewById(R.id.points))
                                            .setText("You have " + myPoints + " points");
                                }

                                private void handleResponse(Activity activity, Response response) {

                                    if (response.getActionCase() == Response.ActionCase.CARDLIST) {

                                        activity.runOnUiThread(
                                                () ->
                                                        updateGameField(
                                                                activity, response.getCardList()));

                                        return;
                                    } else if (response.getActionCase()
                                            == Response.ActionCase.STICHMADE) {
                                        activity.runOnUiThread(
                                                () -> showStich(activity, response.getStichMade()));
                                        return;
                                    } else if (response.getActionCase()
                                            == Response.ActionCase.GAMESTATUS) {
                                        activity.runOnUiThread(
                                                () ->
                                                        updateRoundNumberAndPoints(
                                                                activity,
                                                                response.getGameStatus()));
                                    } else if (response.getActionCase()
                                            == Response.ActionCase.CHEATING) {
                                        if (response.getCheating()
                                                .getCheaterId()
                                                .equals(playerId)) { // I have cheated
                                            activity.runOnUiThread(
                                                    () ->
                                                            youCheated(
                                                                    activity,
                                                                    response.getCheating()));
                                            return;
                                        } else { // someone else has cheated
                                            activity.runOnUiThread(
                                                    () ->
                                                            someOneCheated(
                                                                    activity,
                                                                    response.getCheating()));
                                            return;
                                        }
                                    } else if (response.getActionCase()
                                            == Response.ActionCase.PLAYERLIST) {

                                        ArrayList realplayers = new ArrayList<>();
                                        for (int i = 0;
                                                i < response.getPlayerList().getPlayerCount();
                                                i++) {
                                            realplayers.add(
                                                    new ClientPlayer(
                                                            response.getPlayerList()
                                                                    .getPlayer(i)
                                                                    .getPlayerId(),
                                                            response.getPlayerList()
                                                                    .getPlayer(i)
                                                                    .getPlayerName(),
                                                            response.getPlayerList()
                                                                    .getPlayer(i)
                                                                    .getPoints()));
                                        }

                                        activity.runOnUiThread(
                                                () -> updatePlayersInRecyclerView(realplayers));
                                        return;
                                    }

                                    switch (response.getType()) { // legacy switch case
                                        case "0":
                                            break;
                                        case "1":
                                            /*activity.runOnUiThread(
                                            () -> showStich(activity, response));*/
                                            // todo remove
                                            break;
                                        case "2":
                                            activity.runOnUiThread(
                                                    () -> makeCardPlayRequest(activity, response));
                                            break;
                                        case "3": // todo remove
                                            /* activity.runOnUiThread(
                                            () -> updateGameField(activity, response));*/
                                            break;
                                        case "4":
                                            activity.runOnUiThread(
                                                    () -> showTrump(activity, response));
                                            break;
                                        case "5":
                                            activity.runOnUiThread(
                                                    () -> makeEstimate(activity, response));
                                            break;
                                        case "6":
                                            /* activity.runOnUiThread(
                                            () ->
                                                    updateRoundNumberAndPoints(
                                                            activity, response));*/
                                            break;
                                        case "7":
                                            activity.runOnUiThread(() -> showGameResults(activity));
                                            break;
                                        case "8":
                                            activity.runOnUiThread(
                                                    () ->
                                                            randomEstimateReceived(
                                                                    activity, response));
                                            break;
                                        case "9":
                                            activity.runOnUiThread(
                                                    GamePlayActivity.this::randomCardPlayed);
                                            break;

                                        default:
                                            throw new IllegalArgumentException(
                                                    "type not implemented");
                                    }
                                }

                                @Override
                                public void onNext(Response response) {
                                    Activity activity = activityReference.get();
                                    if (activity == null) {
                                        return;
                                    }

                                    handleResponse(activity, response);
                                }

                                @Override
                                public void onError(Throwable t) {
                                    failed = t;
                                    finishLatch.countDown();
                                }

                                @Override
                                public void onCompleted() {
                                    appendLogs(logs, "Finished RouteChat");
                                    finishLatch.countDown();
                                }
                            });
            try {
                serverWaitingQueue.add(
                        newGameMove(0, "suscribing to updates")); // initial subscription reques

                while (true) {
                    GameMove request =
                            serverWaitingQueue
                                    .take(); // waits for some new data to become available,
                    // blocking
                    if (request.getType().equals("-1")) break;
                    appendLogs(logs, "Sending message \"{0}\"", request.getData());
                    requestObserver.onNext(request);
                }

            } catch (RuntimeException e) {
                // Cancel RPC
                requestObserver.onError(e);
                throw e;
            }
            // Mark the end of requests
            requestObserver.onCompleted();

            // Receiving happens asynchronously
            if (!finishLatch.await(1, TimeUnit.HOURS)) { // max play time
                throw new RuntimeException(
                        "Could not finish rpc within "
                                + MainActivity.SERVER_TIMEOUT_SECONDS
                                + " seconds, the server is likely down");
            }

            if (failed != null) {
                throw new RuntimeException(failed);
            }

            return logs.toString();
        }
    }

    private void randomCardPlayed() {
        playcardadapter.endPlayingCard();
        ProgressBar cardPlayTimeout = findViewById(R.id.cardPlayTimeout);
        cardPlayTimeout.setVisibility(View.INVISIBLE);
        whosTurnIsItText.setText("Please wait for your turn!");
    }

    private void randomEstimateReceived(Activity activity, Response response) {

        Toast.makeText(
                        activity.getApplication().getApplicationContext(),
                        "Your estimate" + response.getData() + " was choosen randomly. Good luck!",
                        Toast.LENGTH_SHORT)
                .show();
        updateEstimateTextview(response.getData());
    }

    public class PlayersRecyclerviewAdapter
            extends RecyclerView.Adapter<PlayersRecyclerviewAdapter.ViewHolder> {

        private List<ClientPlayer> players;
        private LayoutInflater layoutInflater;
        public String selectedPlayer;

        PlayersRecyclerviewAdapter(Context context, List<ClientPlayer> players) {
            this.layoutInflater = LayoutInflater.from(context);
            this.players = players;
            selectedPlayer = null;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View view =
                    layoutInflater.inflate(
                            R.layout.players_recyclerview_textfield, viewGroup, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int position) {
            viewHolder.playername_holder.setText(players.get(position).getName());
        }

        @Override
        public int getItemCount() {
            return players.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            Button playername_holder;

            ViewHolder(View view) {
                super(view);
                playername_holder = view.findViewById(R.id.cheating_player_button);

                playername_holder.setOnClickListener(
                        e -> {
                            hideCheatingExposingView();
                            exposeCheating(playername_holder.getText().toString());
                        });
            }
        }
    }

    public class CardsInHandRecyclerViewAdapter
            extends RecyclerView.Adapter<CardsInHandRecyclerViewAdapter.ViewHolder> {
        private ArrayList<String> cards;
        private LayoutInflater layoutInflater;
        public String selectedCard;
        public int counter = 0;
        private boolean allowPlayingCard = false;
        // public boolean isActivated = false;

        CardsInHandRecyclerViewAdapter(Context context, ArrayList<String> cards) {
            this.layoutInflater = LayoutInflater.from(context);
            this.cards = cards;
            selectedCard = null;
        }

        public void activatePlayingCard() {
            allowPlayingCard = true;
        }

        public void endPlayingCard() {
            allowPlayingCard = false;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View view = layoutInflater.inflate(R.layout.cards_recyclerview_image, viewGroup, false);
            return new ViewHolder(view);
        }

        @SuppressLint("UseCompatLoadingForDrawables")
        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int position) {
            String cardname = cards.get(position);

            viewHolder.card_holder.setTag(counter);
            counter++;

            switch (cardname) {
                case ("BLUEONE"):
                    viewHolder.card_holder.setImageResource(R.drawable.bl_01);
                    break;
                case ("BLUETWO"):
                    viewHolder.card_holder.setImageResource(R.drawable.bl_02);
                    break;
                case ("BLUETHREE"):
                    viewHolder.card_holder.setImageResource(R.drawable.bl_03);
                    break;
                case ("BLUEFOUR"):
                    viewHolder.card_holder.setImageResource(R.drawable.bl_04);
                    break;
                case ("BLUEFIVE"):
                    viewHolder.card_holder.setImageResource(R.drawable.bl_05);
                    break;
                case ("BLUESIX"):
                    viewHolder.card_holder.setImageResource(R.drawable.bl_06);
                    break;
                case ("BLUESEVEN"):
                    viewHolder.card_holder.setImageResource(R.drawable.bl_07);
                    break;
                case ("BLUEEIGHT"):
                    viewHolder.card_holder.setImageResource(R.drawable.bl_08);
                    break;
                case ("BLUENINE"):
                    viewHolder.card_holder.setImageResource(R.drawable.bl_09);
                    break;
                case ("BLUETEN"):
                    viewHolder.card_holder.setImageResource(R.drawable.bl_10);
                    break;
                case ("BLUEELEVEN"):
                    viewHolder.card_holder.setImageResource(R.drawable.bl_11);
                    break;
                case ("BLUETWELVE"):
                    viewHolder.card_holder.setImageResource(R.drawable.bl_12);
                    break;

                case ("REDONE"):
                    viewHolder.card_holder.setImageResource(R.drawable.ro_01);
                    break;
                case ("REDTWO"):
                    viewHolder.card_holder.setImageResource(R.drawable.ro_02);
                    break;
                case ("REDTHREE"):
                    viewHolder.card_holder.setImageResource(R.drawable.ro_03);
                    break;
                case ("REDFOUR"):
                    viewHolder.card_holder.setImageResource(R.drawable.ro_04);
                    break;
                case ("REDFIVE"):
                    viewHolder.card_holder.setImageResource(R.drawable.ro_05);
                    break;
                case ("REDSIX"):
                    viewHolder.card_holder.setImageResource(R.drawable.ro_06);
                    break;
                case ("REDSEVEN"):
                    viewHolder.card_holder.setImageResource(R.drawable.ro_07);
                    break;
                case ("REDEIGHT"):
                    viewHolder.card_holder.setImageResource(R.drawable.ro_08);
                    break;
                case ("REDNINE"):
                    viewHolder.card_holder.setImageResource(R.drawable.ro_09);
                    break;
                case ("REDTEN"):
                    viewHolder.card_holder.setImageResource(R.drawable.ro_10);
                    break;
                case ("REDELEVEN"):
                    viewHolder.card_holder.setImageResource(R.drawable.ro_11);
                    break;
                case ("REDTWELVE"):
                    viewHolder.card_holder.setImageResource(R.drawable.ro_12);
                    break;

                case ("YELLOWONE"):
                    viewHolder.card_holder.setImageResource(R.drawable.ge_01);
                    break;
                case ("YELLOWTWO"):
                    viewHolder.card_holder.setImageResource(R.drawable.ge_02);
                    break;
                case ("YELLOWTHREE"):
                    viewHolder.card_holder.setImageResource(R.drawable.ge_03);
                    break;
                case ("YELLOWFOUR"):
                    viewHolder.card_holder.setImageResource(R.drawable.ge_04);
                    break;
                case ("YELLOWFIVE"):
                    viewHolder.card_holder.setImageResource(R.drawable.ge_05);
                    break;
                case ("YELLOWSIX"):
                    viewHolder.card_holder.setImageResource(R.drawable.ge_06);
                    break;
                case ("YELLOWSEVEN"):
                    viewHolder.card_holder.setImageResource(R.drawable.ge_07);
                    break;
                case ("YELLOWEIGHT"):
                    viewHolder.card_holder.setImageResource(R.drawable.ge_08);
                    break;
                case ("YELLOWNINE"):
                    viewHolder.card_holder.setImageResource(R.drawable.ge_09);
                    break;
                case ("YELLOWTEN"):
                    viewHolder.card_holder.setImageResource(R.drawable.ge_10);
                    break;
                case ("YELLOWELEVEN"):
                    viewHolder.card_holder.setImageResource(R.drawable.ge_11);
                    break;
                case ("YELLOWTWELVE"):
                    viewHolder.card_holder.setImageResource(R.drawable.ge_12);
                    break;

                case ("GREENONE"):
                    viewHolder.card_holder.setImageResource(R.drawable.gr_01);
                    break;
                case ("GREENTWO"):
                    viewHolder.card_holder.setImageResource(R.drawable.gr_02);
                    break;
                case ("GREENTHREE"):
                    viewHolder.card_holder.setImageResource(R.drawable.gr_03);
                    break;
                case ("GREENFOUR"):
                    viewHolder.card_holder.setImageResource(R.drawable.gr_04);
                    break;
                case ("GREENFIVE"):
                    viewHolder.card_holder.setImageResource(R.drawable.gr_05);
                    break;
                case ("GREENSIX"):
                    viewHolder.card_holder.setImageResource(R.drawable.gr_06);
                    break;
                case ("GREENSEVEN"):
                    viewHolder.card_holder.setImageResource(R.drawable.gr_07);
                    break;
                case ("GREENEIGHT"):
                    viewHolder.card_holder.setImageResource(R.drawable.gr_08);
                    break;
                case ("GREENNINE"):
                    viewHolder.card_holder.setImageResource(R.drawable.gr_09);
                    break;
                case ("GREENTEN"):
                    viewHolder.card_holder.setImageResource(R.drawable.gr_10);
                    break;
                case ("GREENLEVEN"):
                    viewHolder.card_holder.setImageResource(R.drawable.gr_11);
                    break;
                case ("GREENTWELVE"):
                    viewHolder.card_holder.setImageResource(R.drawable.gr_12);
                    break;
                case ("NONEWIZARD"):
                    viewHolder.card_holder.setImageResource(R.drawable.z_01);
                    break;
                case ("NONEJESTER"):
                    viewHolder.card_holder.setImageResource(R.drawable.n_01);
                    break;
                    // TODO: add the other Wizards&Jesters after the Naming Bug is Fixed
                default:
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return cards.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ImageView card_holder;
            String cardname;

            ViewHolder(View view) {
                super(view);
                card_holder = view.findViewById(R.id.cardImageButton);

                card_holder.setOnClickListener(
                        e -> {
                            if (allowPlayingCard) {
                                String cardname = card_holder.getTag().toString();
                                playCard(cardname);
                                allowPlayingCard = false;
                            }
                        });
            }
        }
    }

    public class CardsInTheMiddleRecyclerViewAdapter
            extends RecyclerView.Adapter<CardsInTheMiddleRecyclerViewAdapter.ViewHolder> {
        private ArrayList<String> cards;
        private LayoutInflater layoutInflater;

        CardsInTheMiddleRecyclerViewAdapter(Context context, ArrayList<String> cards) {
            this.layoutInflater = LayoutInflater.from(context);
            this.cards = cards;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View view = layoutInflater.inflate(R.layout.cards_recyclerview_image, viewGroup, false);
            return new ViewHolder(view);
        }

        @SuppressLint("UseCompatLoadingForDrawables")
        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int position) {
            String cardname = cards.get(position);

            switch (cardname) {
                case ("BLUEONE"):
                    viewHolder.card_holder.setImageResource(R.drawable.bl_01);
                    break;
                case ("BLUETWO"):
                    viewHolder.card_holder.setImageResource(R.drawable.bl_02);
                    break;
                case ("BLUETHREE"):
                    viewHolder.card_holder.setImageResource(R.drawable.bl_03);
                    break;
                case ("BLUEFOUR"):
                    viewHolder.card_holder.setImageResource(R.drawable.bl_04);
                    break;
                case ("BLUEFIVE"):
                    viewHolder.card_holder.setImageResource(R.drawable.bl_05);
                    break;
                case ("BLUESIX"):
                    viewHolder.card_holder.setImageResource(R.drawable.bl_06);
                    break;
                case ("BLUESEVEN"):
                    viewHolder.card_holder.setImageResource(R.drawable.bl_07);
                    break;
                case ("BLUEEIGHT"):
                    viewHolder.card_holder.setImageResource(R.drawable.bl_08);
                    break;
                case ("BLUENINE"):
                    viewHolder.card_holder.setImageResource(R.drawable.bl_09);
                    break;
                case ("BLUETEN"):
                    viewHolder.card_holder.setImageResource(R.drawable.bl_10);
                    break;
                case ("BLUEELEVEN"):
                    viewHolder.card_holder.setImageResource(R.drawable.bl_11);
                    break;
                case ("BLUETWELVE"):
                    viewHolder.card_holder.setImageResource(R.drawable.bl_12);
                    break;

                case ("REDONE"):
                    viewHolder.card_holder.setImageResource(R.drawable.ro_01);
                    break;
                case ("REDTWO"):
                    viewHolder.card_holder.setImageResource(R.drawable.ro_02);
                    break;
                case ("REDTHREE"):
                    viewHolder.card_holder.setImageResource(R.drawable.ro_03);
                    break;
                case ("REDFOUR"):
                    viewHolder.card_holder.setImageResource(R.drawable.ro_04);
                    break;
                case ("REDFIVE"):
                    viewHolder.card_holder.setImageResource(R.drawable.ro_05);
                    break;
                case ("REDSIX"):
                    viewHolder.card_holder.setImageResource(R.drawable.ro_06);
                    break;
                case ("REDSEVEN"):
                    viewHolder.card_holder.setImageResource(R.drawable.ro_07);
                    break;
                case ("REDEIGHT"):
                    viewHolder.card_holder.setImageResource(R.drawable.ro_08);
                    break;
                case ("REDNINE"):
                    viewHolder.card_holder.setImageResource(R.drawable.ro_09);
                    break;
                case ("REDTEN"):
                    viewHolder.card_holder.setImageResource(R.drawable.ro_10);
                    break;
                case ("REDELEVEN"):
                    viewHolder.card_holder.setImageResource(R.drawable.ro_11);
                    break;
                case ("REDTWELVE"):
                    viewHolder.card_holder.setImageResource(R.drawable.ro_12);
                    break;

                case ("YELLOWONE"):
                    viewHolder.card_holder.setImageResource(R.drawable.ge_01);
                    break;
                case ("YELLOWTWO"):
                    viewHolder.card_holder.setImageResource(R.drawable.ge_02);
                    break;
                case ("YELLOWTHREE"):
                    viewHolder.card_holder.setImageResource(R.drawable.ge_03);
                    break;
                case ("YELLOWFOUR"):
                    viewHolder.card_holder.setImageResource(R.drawable.ge_04);
                    break;
                case ("YELLOWFIVE"):
                    viewHolder.card_holder.setImageResource(R.drawable.ge_05);
                    break;
                case ("YELLOWSIX"):
                    viewHolder.card_holder.setImageResource(R.drawable.ge_06);
                    break;
                case ("YELLOWSEVEN"):
                    viewHolder.card_holder.setImageResource(R.drawable.ge_07);
                    break;
                case ("YELLOWEIGHT"):
                    viewHolder.card_holder.setImageResource(R.drawable.ge_08);
                    break;
                case ("YELLOWNINE"):
                    viewHolder.card_holder.setImageResource(R.drawable.ge_09);
                    break;
                case ("YELLOWTEN"):
                    viewHolder.card_holder.setImageResource(R.drawable.ge_10);
                    break;
                case ("YELLOWELEVEN"):
                    viewHolder.card_holder.setImageResource(R.drawable.ge_11);
                    break;
                case ("YELLOWTWELVE"):
                    viewHolder.card_holder.setImageResource(R.drawable.ge_12);
                    break;

                case ("GREENONE"):
                    viewHolder.card_holder.setImageResource(R.drawable.gr_01);
                    break;
                case ("GREENTWO"):
                    viewHolder.card_holder.setImageResource(R.drawable.gr_02);
                    break;
                case ("GREENTHREE"):
                    viewHolder.card_holder.setImageResource(R.drawable.gr_03);
                    break;
                case ("GREENFOUR"):
                    viewHolder.card_holder.setImageResource(R.drawable.gr_04);
                    break;
                case ("GREENFIVE"):
                    viewHolder.card_holder.setImageResource(R.drawable.gr_05);
                    break;
                case ("GREENSIX"):
                    viewHolder.card_holder.setImageResource(R.drawable.gr_06);
                    break;
                case ("GREENSEVEN"):
                    viewHolder.card_holder.setImageResource(R.drawable.gr_07);
                    break;
                case ("GREENEIGHT"):
                    viewHolder.card_holder.setImageResource(R.drawable.gr_08);
                    break;
                case ("GREENNINE"):
                    viewHolder.card_holder.setImageResource(R.drawable.gr_09);
                    break;
                case ("GREENTEN"):
                    viewHolder.card_holder.setImageResource(R.drawable.gr_10);
                    break;
                case ("GREENLEVEN"):
                    viewHolder.card_holder.setImageResource(R.drawable.gr_11);
                    break;
                case ("GREENTWELVE"):
                    viewHolder.card_holder.setImageResource(R.drawable.gr_12);
                    break;
                case ("NONEWIZARD"):
                    viewHolder.card_holder.setImageResource(R.drawable.z_01);
                    break;
                case ("NONEJESTER"):
                    viewHolder.card_holder.setImageResource(R.drawable.n_01);
                    break;
                    // TODO: add the other Wizards&Jesters after the Naming Bug is Fixed
                default:
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return cards.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ImageView card_holder;

            ViewHolder(View view) {
                super(view);
                card_holder = view.findViewById(R.id.cardImageButton);
            }
        }
    }

    public class EstimateDialog extends Dialog {
        public Activity activity;
        EditText estimateInputField;
        Button estimateSend;
        ProgressBar submitEstimateTimeoutProgressBar;
        CountDownTimer countDownTimer;
        int progress = 0;
        // ArrayList<String> cards;

        public EstimateDialog(Activity activity) {
            super(activity);
            this.activity = activity;
            // this.cards = cards;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            RecyclerView cardsRecyclerView = findViewById(R.id.cardsInEstimateDialog);
            ArrayList<String> cardsList = new ArrayList<>();
            LinearLayoutManager layoutManagerCardsEstimate =
                    new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
            cardsRecyclerView.setLayoutManager(layoutManagerCardsEstimate);

            CardsInTheMiddleRecyclerViewAdapter estimateCardsAdapter =
                    new CardsInTheMiddleRecyclerViewAdapter(getContext(), cards);
            cardsRecyclerView.setAdapter(estimateCardsAdapter);

            estimateSend = findViewById(R.id.dialogEnterButton);
            submitEstimateTimeoutProgressBar = findViewById(R.id.submitEstimateTimeoutProgressBar);

            estimateSend.setOnClickListener(
                    e -> {
                        estimateInputField = findViewById(R.id.dialogEstimateInput);
                        String estimate = estimateInputField.getText().toString();

                        if (!estimate.equals("")) {
                            submitEstimate(estimate);
                            updateEstimateTextview(estimate);
                            dismiss();
                        }
                    });
            submitEstimateTimeoutProgressBar.setProgress(progress); // initial progress
            countDownTimer =
                    new CountDownTimer(60000, 1000) {
                        @Override
                        public void onTick(long l) {
                            submitEstimateTimeoutProgressBar.setProgress(
                                    progress * 100 / (60000 / 1000));
                            progress++;
                        }

                        @Override
                        public void onFinish() {
                            submitEstimateTimeoutProgressBar.setProgress(100);
                            Log.i(
                                    "Wizzard",
                                    "Client timer submit estimate timed out, waiting for server to"
                                            + " calculate new estimate...");
                            dismiss();
                        }
                    };
            countDownTimer.start();
        }
    }
}
