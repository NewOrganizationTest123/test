package com.github.wizard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import android.hardware.SensorEventListener;


public class GamePlayActivity extends AppCompatActivity {


    public static String gameId;
    public static String playerId;
    public static ArrayList<String> players = new ArrayList<>();//todo maybe not use String here
    public static PlayersRecyclerviewAdapter adapter;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_play);
        Intent intent = getIntent();
        gameId = intent.getStringExtra(MainActivity.GAME_ID_KEY); // reuse for later requests
        playerId = intent.getStringExtra(MainActivity.PLAYER_ID_KEY);
        new GameActionRunner(new GameActionRunnable(), new WeakReference<>(this), channel)
                .execute(); // fire up the streaming service

        findViewById(R.id.button_estimate).setOnClickListener(this::submitEstimate);
        findViewById(R.id.button_play_card).setOnClickListener(this::playCard);
        cheatsView = findViewById(R.id.ExposeCheatsView);
        playersRecyclerView = findViewById(R.id.playerRecyclerView);
        closeCheatsViewButton = findViewById(R.id.closeCheatsViewButton);
        cheatsViewTitle = findViewById(R.id.cheatingViewTitle);
        hideCheatingExposingView(); //by default, the cheating-exposing view is not visible; only shows up after shaking device

        closeCheatsViewButton.setOnClickListener(e->{
            hideCheatingExposingView();
        });

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Objects.requireNonNull(sensorManager).registerListener(sensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        deviceAcceleration=10;
        deviceAcceleration_before=SensorManager.GRAVITY_EARTH;
        deviceAcceleration_now=SensorManager.GRAVITY_EARTH;

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        playersRecyclerView.setLayoutManager(layoutManager);

        //TODO: get real players instead of example ArrayList

        adapter = new PlayersRecyclerviewAdapter(this, players);
        playersRecyclerView.setAdapter(adapter);

        findViewById(R.id.cheatin).setOnClickListener((view)->showCheatingExposingView());

    }

    public void showCheatingExposingView(){
        cheatsView.setVisibility(View.VISIBLE);
        playersRecyclerView.setVisibility(View.VISIBLE);
        closeCheatsViewButton.setVisibility(View.VISIBLE);
        cheatsViewTitle.setVisibility(View.VISIBLE);
    }

    public void hideCheatingExposingView(){
        cheatsView.setVisibility(View.GONE);
        playersRecyclerView.setVisibility(View.GONE);
        closeCheatsViewButton.setVisibility(View.GONE);
        cheatsViewTitle.setVisibility(View.GONE);
    }

    public void exposeCheating(String playername){
       /* //TODO: get boolean variable of player with name "playername" from server, which is true if the player has cheated

        Boolean hasCheated = true; //provisional variable until real value is retrieved from server

        if(hasCheated){
            //TODO: update points on server (this player +20 and player with name "playername" -2 and show Toast with information to all players
        }

        else{
            //TODO: update points on server (this player -10) and show Toast with information to all players
        }*/
        serverWaitingQueue.add(newGameMove(3,playername));



    }

    public void updatePlayersInRecyclerView(ArrayList<String> realplayers){
        PlayersRecyclerviewAdapter newdapater = new PlayersRecyclerviewAdapter(this, realplayers);
        playersRecyclerView.setAdapter(newdapater);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(sensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    }
    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorListener);
    }


    private final SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            double x_axis = sensorEvent.values[0];
            double y_axis = sensorEvent.values[1];
            double z_axis = sensorEvent.values[2];
            deviceAcceleration_before = deviceAcceleration_now;
            deviceAcceleration_now = Math.sqrt(x_axis * x_axis + y_axis * y_axis + z_axis * z_axis);
            double delta = deviceAcceleration_now - deviceAcceleration_before;
            deviceAcceleration = deviceAcceleration * 0.9 + delta;

            if (deviceAcceleration > 10) {
                showCheatingExposingView();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    private void playCard(View view) {
        (findViewById(R.id.editTextN_card)).setVisibility(View.GONE);
        (findViewById(R.id.button_play_card)).setVisibility(View.GONE);

        // submit to server
        EditText card = findViewById(R.id.editTextN_card);
        serverWaitingQueue.add(newGameMove(2, card.getText().toString()));
    }

    private void submitEstimate(View view) {
        (findViewById(R.id.editTextNumber_estimate)).setVisibility(View.GONE);
        (findViewById(R.id.button_estimate)).setVisibility(View.GONE);

        // submit to server
        EditText card = findViewById(R.id.editTextNumber_estimate);
        serverWaitingQueue.add(newGameMove(1, card.getText().toString()));
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
                                    if(stichmade.getPlayerid().equals(playerId)){//I made the stich
                                        Toast.makeText(
                                                activity.getApplication()
                                                        .getApplicationContext(),
                                                "You have made this stich!",
                                                Toast.LENGTH_SHORT)
                                                .show();
                                        ((TextView) activity.findViewById(R.id.stiche_made)).setText(stichmade.getTotalstichebyplayer());

                                    }
                                    else//someone else made the stich
                                        Toast.makeText(
                                                activity.getApplication()
                                                        .getApplicationContext(),
                                                "player "+ stichmade.getPlayerName()+" has made this stich. They have made a total of "+stichmade.getTotalstichebyplayer()+" stich",
                                                Toast.LENGTH_SHORT)
                                                .show();
                                }

                                private void makeCardPlayRequest(
                                        Activity activity, Response response) {
                                    Toast.makeText(
                                                    activity.getApplication()
                                                            .getApplicationContext(),
                                                    response.getData(),
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                }


                                private void updateGameField(Activity activity, CardList cardList) {
                                    //update cards in hand
                                    StringBuilder builderHand=new StringBuilder();
                                    for (Card c:cardList.getHandList()) {
                                        builderHand.append(c.getColor()+c.getValue().toString()+"/");
                                    }
                                    ((TextView) activity.findViewById(R.id.cards_in_Hand))
                                            .setText(builderHand.toString());

                                    //update cards on table
                                    StringBuilder builderTable=new StringBuilder();
                                    for (Card c:cardList.getTableList()) {
                                        builderTable.append(c.getColor()+c.getValue().toString()+"/");
                                    }
                                    ((TextView) activity.findViewById(R.id.cards_on_table))
                                            .setText(builderTable);
                                    Toast.makeText(
                                            activity.getApplication()
                                                    .getApplicationContext(),
                                            "your have received your" + " cards",
                                            Toast.LENGTH_SHORT)
                                            .show();
                                }

                                private void showTrump(Activity activity, Response response) {
                                    ((TextView) activity.findViewById(R.id.trumpf))
                                            .setText("Trumpf is " + response.getData());
                                    Toast.makeText(
                                                    activity.getApplication()
                                                            .getApplicationContext(),
                                                    "trumpf is: " + response.getData(),
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                }

                                private void makeEstimate(Activity activity, Response response) {
                                    (activity.findViewById(R.id.editTextNumber_estimate))
                                            .setVisibility(View.VISIBLE);
                                    (activity.findViewById(R.id.button_estimate))
                                            .setVisibility(View.VISIBLE);
                                }

                                private void updateRoundNumberAndPoints(Activity activity, GameStatus gameStatus) {
                                    ((TextView) activity.findViewById(R.id.points))
                                            .setText(
                                                    "You have "
                                                            + gameStatus.getMyPoints()
                                                            + " points");
                                    ((TextView) activity.findViewById(R.id.round))
                                            .setText(
                                                    "This is round "
                                                            +gameStatus.getRound());

                                    Toast.makeText(
                                            activity.getApplication()
                                                    .getApplicationContext(),
                                            "after round "
                                                    + gameStatus.getRound()
                                                    + " you have "
                                                    + gameStatus.getMyPoints()
                                                    + " points!",
                                            Toast.LENGTH_SHORT)
                                            .show();
                                }
                                private void youCheated(Activity activity,CheatingSubmittedResult cheatingSubmittedResult){
                                    ((TextView) activity.findViewById(R.id.points))
                                            .setText(
                                                    "You have "
                                                            + cheatingSubmittedResult.getNewPoints()
                                                            + " points");
                                    Toast.makeText(
                                            activity.getApplication()
                                                    .getApplicationContext(),
                                          "Your cheating was discovered!!!! You now have "+ cheatingSubmittedResult.getNewPoints()+" Points",
                                            Toast.LENGTH_SHORT)
                                            .show();
                                }
                                private void someOneCheated(Activity activity,CheatingSubmittedResult cheatingSubmittedResult){//we will not get our id because submitting cheaters is anonymous
                                    ((TextView) activity.findViewById(R.id.points))
                                            .setText(
                                                    "You have "
                                                            + cheatingSubmittedResult.getNewPoints()
                                                            + " points");
                                    Toast.makeText(
                                            activity.getApplication()
                                                    .getApplicationContext(),
                                            "Someone cheated! You now have "+ cheatingSubmittedResult.getNewPoints()+" Points",
                                            Toast.LENGTH_SHORT)
                                            .show();
                                }

                                private void handleResponse(Activity activity, Response response) {



                                    if(response.getActionCase()==Response.ActionCase.CARDLIST){
                                        //todo new display cards
                                        activity.runOnUiThread(
                                                () -> updateGameField(activity, response.getCardList()));

                                        return;
                                    }else if(response.getActionCase()==Response.ActionCase.STICHMADE){
                                        activity.runOnUiThread(
                                                () -> showStich(activity, response.getStichMade()));
                                        return;
                                    }else if(response.getActionCase()==Response.ActionCase.GAMESTATUS){
                                        activity.runOnUiThread( () ->
                                                updateRoundNumberAndPoints(
                                                        activity, response.getGameStatus()));
                                    }else if(response.getActionCase()==Response.ActionCase.CHEATING){
                                        if(response.getCheating().getCheaterId().equals(playerId)){//I have cheated
                                            activity.runOnUiThread(()->youCheated(activity,response.getCheating()));
                                            return;
                                        }
                                        else{//someone else has cheated
                                            activity.runOnUiThread(()->someOneCheated(activity,response.getCheating()));
                                            return;
                                        }
                                    }else if(response.getActionCase()==Response.ActionCase.PLAYERLIST){

                                        ArrayList realplayers=new ArrayList<>();
                                        for (int i = 0; i < response.getPlayerList().getPlayerCount(); i++) {
                                            realplayers.add(response.getPlayerList().getPlayer(i).getPlayerName());
                                        }



                                        activity.runOnUiThread(()-> updatePlayersInRecyclerView(realplayers));
                                        return;
                                    }


                                    switch (response.getType()) {//legacy switch case
                                        case "0":
                                            break;
                                        case "1":
                                            /*activity.runOnUiThread(
                                                    () -> showStich(activity, response));*///todo remove
                                            break;
                                        case "2":
                                            activity.runOnUiThread(
                                                    () -> makeCardPlayRequest(activity, response));
                                            break;
                                        case "3"://todo remove
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

    public class PlayersRecyclerviewAdapter extends RecyclerView.Adapter<PlayersRecyclerviewAdapter.ViewHolder> {

        private ArrayList<String> players;
        private LayoutInflater layoutInflater;
        public String selectedPlayer;

        PlayersRecyclerviewAdapter(Context context, ArrayList<String> players) {
            this.layoutInflater = LayoutInflater.from(context);
            this.players = players;
            selectedPlayer=null;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View view = layoutInflater.inflate(R.layout.players_recyclerview_textfield, viewGroup, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int position) {
            String playername = players.get(position);
            viewHolder.playername_holder.setText(playername);
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

                playername_holder.setOnClickListener(e->{
                    hideCheatingExposingView();
                    exposeCheating(playername_holder.getText().toString());
                });
            }
        }
    }

}
