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
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
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

    private static String gameId;
    private static String playerId;
    private static int myPoints = 0;
    private static List<ClientPlayer> players = new ArrayList<>();

    ManagedChannel channel;
    private static final BlockingQueue<GameMove> serverWaitingQueue = new LinkedBlockingQueue<>();
    private SensorManager sensorManager;
    private double deviceAcceleration;
    private double deviceAccelerationBefore;
    private double deviceAccelerationNow;
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
    private ImageButton showscore;
    private FrameLayout scoreboardframe;
    private Button homebutton;
    private Button endgame; // ONLY for testcases
    private Button closeScoreboard;

    public static String getGameId() {
        return gameId;
    }

    public static String getPlayerId() {
        return playerId;
    }

    private static void appendLogs(StringBuilder logs, String msg, Object... params) {
        if (params.length > 0) {
            logs.append(MessageFormat.format(msg, params));
        } else {
            logs.append(msg);
        }
        logs.append("\n");
    }

    private static GameMove newGameMove(int type, String message) {
        return GameMove.newBuilder()
                .setGameid(GamePlayActivity.getGameId())
                .setPlayerid(GamePlayActivity.getPlayerId())
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
        new GameActionRunner(new GameActionRunnable(), new WeakReference<>(this), channel)
                .execute(); // fire up the streaming service

        points = findViewById(R.id.points);
        showscore = findViewById(R.id.btnscoreboard);
        scoreboardframe = findViewById(R.id.framescoreboard);
        endgame = findViewById(R.id.btnendgame);
        homebutton = findViewById(R.id.btnhomescreen);
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

        closeCheatsViewButton.setOnClickListener(e -> hideCheatingExposingView());

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Objects.requireNonNull(sensorManager)
                .registerListener(
                        sensorListener,
                        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                        SensorManager.SENSOR_DELAY_NORMAL);
        deviceAcceleration = 10;
        deviceAccelerationBefore = SensorManager.GRAVITY_EARTH;
        deviceAccelerationNow = SensorManager.GRAVITY_EARTH;

        LinearLayoutManager layoutManagerPlayers = new LinearLayoutManager(this);
        playersRecyclerView.setLayoutManager(layoutManagerPlayers);

        PlayersRecyclerviewAdapter players_adapter = new PlayersRecyclerviewAdapter(this, players);
        playersRecyclerView.setAdapter(players_adapter);

        cardsInHandRecyclerView = findViewById(R.id.cardsInHandRecyclerview);
        LinearLayoutManager layoutManagerCards =
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        cardsInHandRecyclerView.setLayoutManager(layoutManagerCards);

        cardsInHandRecyclerView.setAdapter(players_adapter);

        cardsInTheMiddleRecyclerView = findViewById(R.id.cardsInTheMiddleRecyclerView);
        LinearLayoutManager layoutManagerCardsMiddle =
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        cardsInTheMiddleRecyclerView.setLayoutManager(layoutManagerCardsMiddle);

        ArrayList<String> cardsMiddleList = new ArrayList<>();
        CardsInTheMiddleRecyclerViewAdapter cards_middle_adapter =
                new CardsInTheMiddleRecyclerViewAdapter(this, cardsMiddleList);
        cardsInTheMiddleRecyclerView.setAdapter(cards_middle_adapter);

        showscore.setOnClickListener(view -> showScoreBoard(new ScoreboardFragment()));
        endgame.setOnClickListener(view -> EndofGame(new ScoreboardFragment()));
        homebutton.setOnClickListener(view -> backtohome());

        closeScoreboard = findViewById(R.id.closeScoreboardButton);
        closeScoreboard.setVisibility(View.GONE);
        closeScoreboard.setOnClickListener(
                e -> {
                    scoreboardframe.setVisibility(View.GONE);
                    closeScoreboard.setVisibility(View.GONE);
                });
    }

    public static String getPlayerpoints(int i) {
        return players.get(i).getPoints().toString();
    }

    /* just for testcase*/
    public void EndofGame(ScoreboardFragment scoreboardfragment) {
        scoreboardframe.setVisibility(View.VISIBLE);
        FragmentManager fragmentm = getSupportFragmentManager();
        FragmentTransaction fragmenttrans = fragmentm.beginTransaction();
        fragmenttrans.replace(R.id.framescoreboard, scoreboardfragment);
        fragmenttrans.commit();
        showscore.setVisibility(View.GONE);
        endgame.setVisibility(View.GONE);
        playersRecyclerView.setVisibility(View.GONE);
        cardsInHandRecyclerView.setVisibility(View.GONE);
        cardsInTheMiddleRecyclerView.setVisibility(View.GONE);
        hideCheatingExposingView();
        homebutton.setVisibility(View.VISIBLE);
        whosTurnIsItText.setVisibility(View.GONE);
        cheatsViewTitle.setVisibility(View.GONE);
        points.setVisibility(View.GONE);
    }

    /*just for test cases*/
    public void backtohome() {
        Intent intent = new Intent(this, MainMenuActivity.class);
        startActivity(intent);
    }

    public void showScoreBoard(ScoreboardFragment fragment) {
        if (scoreboardframe.getVisibility() == View.VISIBLE) {
            scoreboardframe.setVisibility(View.GONE);
            FragmentManager fragmentm = getSupportFragmentManager();
            FragmentTransaction fragmenttrans = fragmentm.beginTransaction();
            fragmenttrans.replace(R.id.framescoreboard, fragment);
            fragmenttrans.commit();
        } else {
            scoreboardframe.setVisibility(View.VISIBLE);
            scoreboardframe.bringToFront();
            closeScoreboard.setVisibility(View.VISIBLE);
            FragmentManager fragmentm = getSupportFragmentManager();
            FragmentTransaction fragmenttrans = fragmentm.beginTransaction();
            fragmenttrans.replace(R.id.framescoreboard, fragment);
            fragmenttrans.commit();
        }
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
        players = new ArrayList<>(realplayers); // include myself for scoreboard
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
                    double xAxis = sensorEvent.values[0];
                    double yAxis = sensorEvent.values[1];
                    double zAxis = sensorEvent.values[2];
                    deviceAccelerationBefore = deviceAccelerationNow;
                    deviceAccelerationNow =
                            Math.sqrt(xAxis * xAxis + yAxis * yAxis + zAxis * zAxis);
                    double delta = deviceAccelerationNow - deviceAccelerationBefore;
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
    private void showGameResults() {
        ScoreboardFragment score = new ScoreboardFragment();
        score.winningplayerhighlighted();
    }

    private void updateCardsInHandRecyclerView(ArrayList<String> cardsInHand) {
        CardsInHandRecyclerViewAdapter newcards_adapter =
                new CardsInHandRecyclerViewAdapter(this, cardsInHand);
        cardsInHandRecyclerView.setAdapter(newcards_adapter);
    }

    private void updateCardsInMiddleRecyclerView(ArrayList<String> cardsInMiddle) {
        CardsInTheMiddleRecyclerViewAdapter newcardsAdapter =
                new CardsInTheMiddleRecyclerViewAdapter(this, cardsInMiddle);
        cardsInTheMiddleRecyclerView.setAdapter(newcardsAdapter);
    }

    private void allowPlayingCard() {
        // TODO: only allow playing Card when CardPlayRequest
        CountDownTimer cardPlayTimer;
        playcardadapter = (CardsInHandRecyclerViewAdapter) cardsInHandRecyclerView.getAdapter();
        assert playcardadapter != null;
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

        /**
         * Bi-directional example, which can only be asynchronous. Send some chat messages, and
         * print any chat messages that are sent from the server.
         */
        private String updateGameBoard(
                GameActionsGrpc.GameActionsStub asyncStub,
                WeakReference<Activity> activityReference)
                throws InterruptedException, RuntimeException {
            final StringBuilder logs = new StringBuilder();
            appendLogs(logs, "*** GamePlay");
            final CountDownLatch finishLatch = new CountDownLatch(1);
            StreamObserver<GameMove> requestObserver =
                    asyncStub.gameStream(
                            new StreamObserver<Response>() {

                                private void showStich(Activity activity, StichMade stichmade) {
                                    if (stichmade
                                            .getPlayerid()
                                            .equals(getPlayerId())) { // I made the stich

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

                                    } else { // someone else made the stich
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
                                }

                                private void makeCardPlayRequest() {
                                    allowPlayingCard();
                                }

                                private void updateGameField(Activity activity, CardList cardList) {

                                    ArrayList<String> cardsInHand = new ArrayList<>();
                                    for (Card c : cardList.getHandList()) {
                                        String cardname = c.getColor() + c.getValue().toString();
                                        cardsInHand.add(cardname);
                                    }

                                    updateCardsInHandRecyclerView(cardsInHand);

                                    ((TextView) activity.findViewById(R.id.cards_on_table))
                                            .setText("");

                                    ArrayList<String> cardsInMiddle = new ArrayList<>();
                                    for (Card c : cardList.getTableList()) {
                                        String cardname = c.getColor() + c.getValue().toString();
                                        cardsInMiddle.add(cardname);
                                    }
                                    updateCardsInMiddleRecyclerView(cardsInMiddle);

                                    cards = cardsInHand;
                                }

                                private void showTrump(Activity activity, Response response) {
                                    ((TextView) activity.findViewById(R.id.trumpf))
                                            .setText("Trump is " + response.getData());

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

                                private void makeEstimate() {
                                    openEstimateDialog();
                                }

                                private void updateRoundNumberAndPoints(
                                        Activity activity, GameStatus gameStatus) {
                                    // reset stich counter to 0 when new round starts
                                    ((TextView) activity.findViewById(R.id.stiche_made))
                                            .setText("You have already made 0 Stiche");

                                    for (GrpcPlayer grpcPlayer : gameStatus.getPlayersList()) {
                                        // update my points
                                        if (getPlayerId().equals(grpcPlayer.getPlayerId()))
                                            myPoints = Integer.parseInt(grpcPlayer.getPoints());
                                        // update points for other people
                                        for (ClientPlayer cPlayer : players) {
                                            if (cPlayer.getId().equals(grpcPlayer.getPlayerId())) {
                                                cPlayer.setPoints(grpcPlayer.getPoints());
                                            }
                                        }
                                    }
                                    ((TextView) activity.findViewById(R.id.points))
                                            .setText(myPoints + " Points");

                                    int roundNr = Integer.parseInt(gameStatus.getRound()) + 1;
                                    ((TextView) activity.findViewById(R.id.round))
                                            .setText("Round " + roundNr);

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
                                        if (getPlayerId().equals(grpcPlayer.getPlayerId()))
                                            myPoints = Integer.parseInt(grpcPlayer.getPoints());
                                        // update points for other people
                                        for (ClientPlayer cPlayer : players) {
                                            if (cPlayer.getId().equals(grpcPlayer.getPlayerId())) {
                                                cPlayer.setPoints(grpcPlayer.getPoints());
                                            }
                                        }
                                    }

                                    ((TextView) activity.findViewById(R.id.points))
                                            .setText(myPoints + " Points");
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
                                        if (getPlayerId().equals(grpcPlayer.getPlayerId()))
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
                                                .equals(getPlayerId())) { // I have cheated
                                            activity.runOnUiThread(
                                                    () ->
                                                            youCheated(
                                                                    activity,
                                                                    response.getCheating()));
                                        } else { // someone else has cheated
                                            activity.runOnUiThread(
                                                    () ->
                                                            someOneCheated(
                                                                    activity,
                                                                    response.getCheating()));
                                        }
                                        return;
                                    } else if (response.getActionCase()
                                            == Response.ActionCase.PLAYERLIST) {

                                        ArrayList<ClientPlayer> realplayers = new ArrayList<>();
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
                                        case "6":
                                        case "3":
                                            break;
                                        case "2":
                                            activity.runOnUiThread(this::makeCardPlayRequest);
                                            break;
                                        case "4":
                                            activity.runOnUiThread(
                                                    () -> showTrump(activity, response));
                                            break;
                                        case "5":
                                            activity.runOnUiThread(this::makeEstimate);
                                            break;
                                        case "7":
                                            activity.runOnUiThread(
                                                    GamePlayActivity.this::showGameResults);
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

        private final List<ClientPlayer> players;
        private final LayoutInflater layoutInflater;

        PlayersRecyclerviewAdapter(Context context, List<ClientPlayer> players) {
            this.layoutInflater = LayoutInflater.from(context);
            this.players = players;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
            View view =
                    layoutInflater.inflate(
                            R.layout.players_recyclerview_textfield, viewGroup, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int position) {
            viewHolder.playernameHolder.setText(players.get(position).getName());
        }

        @Override
        public int getItemCount() {
            return players.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            Button playernameHolder;

            ViewHolder(View view) {
                super(view);
                playernameHolder = view.findViewById(R.id.cheating_player_button);

                playernameHolder.setOnClickListener(
                        e -> {
                            hideCheatingExposingView();
                            exposeCheating(playernameHolder.getText().toString());
                        });
            }
        }
    }

    private enum CardNames {
        BLUEONE(R.drawable.bl_01),
        BLUETWO(R.drawable.bl_02),
        BLUETHREE(R.drawable.bl_03),
        BLUEFOUR(R.drawable.bl_04),
        BLUEFIVE(R.drawable.bl_05),
        BLUESIX(R.drawable.bl_06),
        BLUESEVEN(R.drawable.bl_07),
        BLUEEIGHT(R.drawable.bl_08),
        BLUENINE(R.drawable.bl_09),
        BLUETEN(R.drawable.bl_10),
        BLUEELEVEN(R.drawable.bl_11),
        BLUETWELVE(R.drawable.bl_12),
        BLUETHIRTEEN(R.drawable.bl_13),
        REDONE(R.drawable.ro_01),
        REDTWO(R.drawable.ro_02),
        REDTHREE(R.drawable.ro_03),
        REDFOUR(R.drawable.ro_04),
        REDFIVE(R.drawable.ro_05),
        REDSIX(R.drawable.ro_06),
        REDSEVEN(R.drawable.ro_07),
        REDEIGHT(R.drawable.ro_08),
        REDNINE(R.drawable.ro_09),
        REDTEN(R.drawable.ro_10),
        REDELEVEN(R.drawable.ro_11),
        REDTWELVE(R.drawable.ro_12),
        REDTHIRTEEN(R.drawable.ro_13),
        YELLOWONE(R.drawable.ge_01),
        YELLOWTWO(R.drawable.ge_02),
        YELLOWTHREE(R.drawable.ge_03),
        YELLOWFOUR(R.drawable.ge_04),
        YELLOWFIVE(R.drawable.ge_05),
        YELLOWSIX(R.drawable.ge_06),
        YELLOWSEVEN(R.drawable.ge_07),
        YELLOWEIGHT(R.drawable.ge_08),
        YELLOWNINE(R.drawable.ge_09),
        YELLOWTEN(R.drawable.ge_10),
        YELLOWELEVEN(R.drawable.ge_11),
        YELLOWTWELVE(R.drawable.ge_12),
        YELLOWTHIRTEEN(R.drawable.ge_13),
        GREENONE(R.drawable.gr_01),
        GREENTWO(R.drawable.gr_02),
        GREENTHREE(R.drawable.gr_03),
        GREENFOUR(R.drawable.gr_04),
        GREENFIVE(R.drawable.gr_05),
        GREENSIX(R.drawable.gr_06),
        GREENSEVEN(R.drawable.gr_07),
        GREENEIGHT(R.drawable.gr_08),
        GREENNINE(R.drawable.gr_09),
        GREENTEN(R.drawable.gr_10),
        GREENLEVEN(R.drawable.gr_11),
        GREENTWELVE(R.drawable.gr_12),
        GREENTHIRTEEN(R.drawable.gr_13),
        REDWIZARD(R.drawable.z_04),
        YELLOWWIZARD(R.drawable.z_03),
        GREENWIZARD(R.drawable.z_02),
        BLUEWIZARD(R.drawable.z_01),
        REDJESTER(R.drawable.n_02),
        YELLOWJESTER(R.drawable.n_03),
        BLUEJESTER(R.drawable.n_01),
        GREENJESTER(R.drawable.n_04);

        private int id;

        CardNames(int cardId) {
            this.id = cardId;
        }

        static CardNames fromName(String name) {
            return valueOf(name);
        }

        static int nameToId(String name) {
            return fromName(name).getId();
        }

        int getId() {
            return id;
        }
    }

    public class CardsInHandRecyclerViewAdapter
            extends RecyclerView.Adapter<CardsInHandRecyclerViewAdapter.ViewHolder> {
        private final ArrayList<String> cards;
        private final LayoutInflater layoutInflater;
        private int counter = 0;
        private boolean allowPlayingCard = false;

        CardsInHandRecyclerViewAdapter(Context context, ArrayList<String> cards) {
            this.layoutInflater = LayoutInflater.from(context);
            this.cards = cards;
        }

        public void activatePlayingCard() {
            allowPlayingCard = true;
        }

        public void endPlayingCard() {
            allowPlayingCard = false;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
            View view = layoutInflater.inflate(R.layout.cards_recyclerview_image, viewGroup, false);
            return new ViewHolder(view);
        }

        @SuppressLint("UseCompatLoadingForDrawables")
        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int position) {
            String cardname = cards.get(position);

            viewHolder.cardHolder.setTag(counter);
            counter++;

            try {
                int imageResource = GamePlayActivity.CardNames.nameToId(cardname);
                viewHolder.cardHolder.setImageResource(imageResource);
            } catch (IllegalArgumentException e) {
                // ignore if no resource was found for the card name
            }
        }

        @Override
        public int getItemCount() {
            return cards.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ImageView cardHolder;

            ViewHolder(View view) {
                super(view);
                cardHolder = view.findViewById(R.id.cardImageButton);

                cardHolder.setOnClickListener(
                        e -> {
                            if (allowPlayingCard) {
                                String cardname = cardHolder.getTag().toString();
                                playCard(cardname);
                                allowPlayingCard = false;
                            }
                        });
            }
        }
    }

    public static class CardsInTheMiddleRecyclerViewAdapter
            extends RecyclerView.Adapter<CardsInTheMiddleRecyclerViewAdapter.ViewHolder> {
        private final ArrayList<String> cards;
        private final LayoutInflater layoutInflater;

        CardsInTheMiddleRecyclerViewAdapter(Context context, ArrayList<String> cards) {
            this.layoutInflater = LayoutInflater.from(context);
            this.cards = cards;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
            View view = layoutInflater.inflate(R.layout.cards_recyclerview_image, viewGroup, false);
            return new ViewHolder(view);
        }

        @SuppressLint("UseCompatLoadingForDrawables")
        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
            String cardname = cards.get(position);

            try {
                int imageResource = GamePlayActivity.CardNames.nameToId(cardname);
                viewHolder.cardsImageView.setImageResource(imageResource);
            } catch (IllegalArgumentException e) {
                // ignore if no resource was found for the card name
            }
        }

        @Override
        public int getItemCount() {
            return cards.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView cardsImageView;

            ViewHolder(View view) {
                super(view);
                cardsImageView = view.findViewById(R.id.cardImageButton);
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

        public EstimateDialog(Activity activity) {
            super(activity);
            this.activity = activity;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            RecyclerView cardsRecyclerView = findViewById(R.id.cardsInEstimateDialog);
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
