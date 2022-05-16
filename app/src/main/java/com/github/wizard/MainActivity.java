package com.github.wizard;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.github.wizard.api.GamePlayGrpc;
import com.github.wizard.api.GameStarterGrpc;
import com.github.wizard.api.JoinRequest;
import com.github.wizard.api.Player;
import com.github.wizard.api.StartReply;
import com.github.wizard.api.StartRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity {
    public static final String GAME_ID_KEY = "com.github.wizard.GAME_ID_KEY";
    public static final String PLAYER_ID_KEY = "com.github.wizard.PLAYER_ID_KEY";
    public static final String TAG = "wizard";
    public static final String SERVER_ADDRESS = "10.0.2.2";
    public static final boolean USE_PLAINTEXT = true;
    public static final int SERVER_PORT = 50051;
    public static final int SERVER_TIMEOUT_SECONDS = 10;
    public static String PLAYER_NAME;

    private EditText name;
    private EditText gameId;
    private Button joinGame;
    private static ManagedChannel channel;
    private ExecutorService executorService;
    private static GameStarterGrpc.GameStarterBlockingStub gameStarterBlockingStub;
    private static GamePlayGrpc.GamePlayBlockingStub gamePlayBlockingStub;
    private static int gameIdInt;
    private static int playerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        name = findViewById(R.id.editTextTextPersonName);
        gameId = findViewById(R.id.editTextNumber);
        joinGame = findViewById(R.id.button3);
        Button startGame = findViewById(R.id.button);
        Button next = findViewById(R.id.next);
        startGame.setOnClickListener(this::startNewGame);
        joinGame.setOnClickListener(this::joinGame);
        next.setOnClickListener(
                (View view) ->
                        new GrpcTaskGamePlay(new activateGAme(), new WeakReference<>(this))
                                .execute());
        gameId.addTextChangedListener(
                new TextWatcher() {

                    public void afterTextChanged(Editable s) {}

                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                        Future<Boolean> booleanFuture =
                                executorService.submit(
                                        () -> {
                                            if (gameId.getText() != null
                                                    && gameId.getText().length() != 0) {
                                                ManagedChannelBuilder<?> builder =
                                                        ManagedChannelBuilder.forAddress(
                                                                SERVER_ADDRESS, SERVER_PORT);

                                                if (USE_PLAINTEXT) builder.usePlaintext();

                                                channel = builder.build();

                                                GameStarterGrpc.GameStarterBlockingStub stub =
                                                        GameStarterGrpc.newBlockingStub(channel);
                                                JoinRequest request =
                                                        JoinRequest.newBuilder()
                                                                .setGameid(
                                                                        gameId.getText().toString())
                                                                .setName("")
                                                                .build();
                                                return stub.checkJoinRequest(request).getReady();
                                            } else return false;
                                        });
                        try {
                            joinGame.setEnabled(
                                    booleanFuture.get(
                                            SERVER_TIMEOUT_SECONDS,
                                            TimeUnit.SECONDS)); // using server timeout
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            Thread.currentThread().interrupt();
                        } catch (TimeoutException e) {
                            Toast.makeText(MainActivity.this, "connection lost", Toast.LENGTH_SHORT)
                                    .show();
                            e.printStackTrace();
                        }
                    }
                });
        executorService = Executors.newSingleThreadExecutor();

        Intent intent = getIntent();
        String intent_message = intent.getStringExtra(MainMenuActivity.EXTRA_MESSAGE);

        if (intent_message.equals("new game")) {
            gameId.setVisibility(View.GONE);
            joinGame.setVisibility(View.GONE);
        } else {
            startGame.setVisibility(View.GONE);
        }
    }

    public void startNewGame(View view) {
        PLAYER_NAME = name.getText().toString();
        new GrpcTask(this, true).execute(name.getText().toString(), "-1");
    }

    public void joinGame(View view) {
        PLAYER_NAME = name.getText().toString();
        new GrpcTask(this, false).execute(name.getText().toString(), gameId.getText().toString());
    }

    @Override
    protected void onStop() {
        try {
            channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        super.onStop();
    }

    private static class GrpcTask extends AsyncTask<String, Void, String> {
        private final WeakReference<Activity> activityReference;
        private final boolean startNewGame;

        private GrpcTask(Activity activity, boolean startNewGame) {
            this.activityReference = new WeakReference<>(activity);
            this.startNewGame = startNewGame;
        }

        @Override
        protected String doInBackground(String... params) {
            String name = params[0];
            String gameid = params[1];

            try {
                if (channel == null || channel.isShutdown()) {
                    ManagedChannelBuilder<?> builder =
                            ManagedChannelBuilder.forAddress(SERVER_ADDRESS, SERVER_PORT);

                    if (USE_PLAINTEXT) builder.usePlaintext();

                    channel = builder.build();
                }

                if (gameStarterBlockingStub == null)
                    gameStarterBlockingStub = GameStarterGrpc.newBlockingStub(channel);
                if (startNewGame) {
                    StartRequest request = StartRequest.newBuilder().setName(name).build();
                    StartReply reply = gameStarterBlockingStub.startGame(request);
                    return reply.getGameid();
                } else {
                    JoinRequest request =
                            JoinRequest.newBuilder().setGameid(gameid).setName(name).build();
                    StartReply reply = gameStarterBlockingStub.joinGame(request);
                    playerId = Integer.parseInt(reply.getPlayerid());
                    return reply.getGameid();
                }

            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                Log.e(TAG, Arrays.toString(e.getStackTrace()));
                pw.flush();
                return String.format("Failed... : %n%s", sw);
            }
        }

        @Override
        protected void onPostExecute(String gameId) {
            Activity activity = activityReference.get();
            if (activity == null) {
                return;
            }

            if (!startNewGame) { // directly join game when joining game
                Intent intent = new Intent(activity, GamePlayActivity.class);
                intent.putExtra(GAME_ID_KEY, gameId);
                intent.putExtra(PLAYER_ID_KEY, playerId + "");
                intent.putExtra(PLAYER_NAME, PLAYER_NAME);
                activity.startActivity(intent);
            } else {
                gameIdInt = Integer.parseInt(gameId);

                activity.runOnUiThread(
                        () -> {
                            TextView gameIdTextView = activity.findViewById(R.id.gameid);
                            gameIdTextView.setVisibility(View.VISIBLE);
                            gameIdTextView.setText("Your game Id is: " + gameId);
                            Button startGame = activity.findViewById(R.id.button);
                            startGame.setVisibility(View.GONE);
                            (activity.findViewById(R.id.editTextTextPersonName))
                                    .setVisibility(View.GONE);

                            new GrpcTaskGamePlay(new getPlayers(), activityReference).execute();
                            Button refresh = activity.findViewById(R.id.refresh_playerList);
                            refresh.setVisibility(View.VISIBLE);
                            refresh.setOnClickListener(
                                    (View view) ->
                                            new GrpcTaskGamePlay(
                                                            new getPlayers(), activityReference)
                                                    .execute());
                        });
            }
        }
    }

    private static class getPlayers implements GrpcRunnable {
        @Override
        public String run(WeakReference<Activity> activityReference) {
            StringBuilder logs = new StringBuilder();
            Activity activity = activityReference.get();
            if (activity == null) {
                return "Failure to get activity";
            }
            TextView playersTextView = activity.findViewById(R.id.players);
            activity.runOnUiThread(
                    () -> {
                        activity.findViewById(R.id.refresh_playerList).setEnabled(false);
                        playersTextView.setVisibility(View.VISIBLE);
                        playersTextView.setText("Players: \n");
                    });

            JoinRequest request =
                    JoinRequest.newBuilder().setGameid(gameIdInt + "").setName("").build();
            Iterator<Player> players = gamePlayBlockingStub.getPlayers(request);

            while (players.hasNext()) {
                Player player = players.next();
                activity.runOnUiThread(() -> playersTextView.append(player.getName() + "\n"));
                logs.append(player.getName()).append("\n");
            }

            return logs + "\ngame is ready to launch!";
        }

        @Override
        public void doWhenDone(WeakReference<Activity> activityReference) {
            Activity activity = activityReference.get();
            if (activity == null) {
                return;
            }
            activity.runOnUiThread(
                    () -> {
                        activity.findViewById(R.id.next).setVisibility(View.VISIBLE);
                        activity.findViewById(R.id.refresh_playerList).setEnabled(true);
                    });
        }
    }

    private static class GrpcTaskGamePlay extends AsyncTask<Void, Void, String> {
        private final GrpcRunnable grpcRunnable;

        private final WeakReference<Activity> activityReference;

        GrpcTaskGamePlay(GrpcRunnable grpcRunnable, WeakReference<Activity> activity) {
            this.grpcRunnable = grpcRunnable;
            this.activityReference = activity;
        }

        @Override
        protected String doInBackground(Void... nothing) {
            try {
                if (channel == null || channel.isShutdown()) {
                    ManagedChannelBuilder<?> builder =
                            ManagedChannelBuilder.forAddress(SERVER_ADDRESS, SERVER_PORT);

                    if (USE_PLAINTEXT) builder.usePlaintext();

                    channel = builder.build();
                }
                if (gamePlayBlockingStub == null)
                    gamePlayBlockingStub = GamePlayGrpc.newBlockingStub(channel);
                String logs = grpcRunnable.run(activityReference);

                return "Success!\n" + logs;
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                pw.flush();
                return "Failed... :\n" + sw;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Log.i("GrpcTaskResult", result);
            grpcRunnable.doWhenDone(activityReference);
        }
    }

    private static class activateGAme implements GrpcRunnable {

        /**
         * Perform a grpcRunnable and return all the logs.
         *
         * @param activityReference
         */
        @Override
        public String run(WeakReference<Activity> activityReference) throws Exception {
            JoinRequest request =
                    JoinRequest.newBuilder().setGameid(gameIdInt + "").setName("").build();
            if (gamePlayBlockingStub.setAsReady(request).getReady()) return "Success";
            return "Failure";
        }

        @Override
        public void doWhenDone(WeakReference<Activity> activityReference) {
            Activity activity = activityReference.get();
            if (activity == null) {
                return;
            }

            Intent intent = new Intent(activity, GamePlayActivity.class);
            intent.putExtra(GAME_ID_KEY, gameIdInt + "");
            intent.putExtra(PLAYER_ID_KEY, playerId + "");
            intent.putExtra(PLAYER_NAME, PLAYER_NAME);
            activity.startActivity(intent);
        }
    }
}
