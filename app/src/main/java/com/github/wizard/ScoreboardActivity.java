package com.github.wizard;

import static com.github.wizard.GamePlayActivity.appendLogs;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import io.grpc.stub.StreamObserver;
import com.github.wizard.api.GameActionsGrpc;
import com.github.wizard.api.GameMove;
import com.github.wizard.api.GameStatus;
import com.github.wizard.api.GrpcPlayer;
import com.github.wizard.api.Player;
import com.github.wizard.api.Response;

public class ScoreboardActivity extends AppCompatActivity {

    public static String gameId;
    public static String playerId;
    public static String playername;
    public static ArrayList<GrpcPlayer> players = new ArrayList<>(); // todo maybe not use String here

    private TextView Player1Name;
    private TextView Player2Name;
    private TextView Player3Name;
    private TextView Player4Name;
    private TextView Player5Name;
    private TextView Player6Name;
    private TextView Player1Points;
    private TextView Player2Points;
    private TextView Player3Points;
    private TextView Player4Points;
    private TextView Player5Points;
    private TextView Player6Points;
    private TextView Roundcounter;
    private Button showscore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_scoreboard);
        Intent intent = getIntent();
        Player1Name = findViewById(R.id.Player1Name);
        Player2Name = findViewById(R.id.Player2Name);
        Player3Name = findViewById(R.id.Player3Name);
        Player4Name = findViewById(R.id.Player4Name);
        Player5Name = findViewById(R.id.Player5Name);
        Player6Name = findViewById(R.id.Player6Name);
        Player1Points = findViewById(R.id.Player1Points);
        Player2Points = findViewById(R.id.Player2Points);
        Player3Points = findViewById(R.id.Player3Points);
        Player4Points = findViewById(R.id.Player4Points);
        Player5Points = findViewById(R.id.Player5Points);
        Player6Points = findViewById(R.id.Player6Points);
        Roundcounter = findViewById(R.id.Roundscounter);
        showscore = findViewById(R.id.showscorebutton);

        gameId = intent.getStringExtra(MainActivity.GAME_ID_KEY); // reuse for later requests
        playerId = intent.getStringExtra(MainActivity.PLAYER_ID_KEY);
        playername = intent.getStringExtra(MainActivity.PLAYER_NAME);

        showscore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                replacefragment(new ScoreboardFragment());
            }
        });
    }

    private void replacefragment(ScoreboardFragment fragment) {
        FragmentManager fragmentm = getSupportFragmentManager();
        FragmentTransaction fragmenttrans = fragmentm.beginTransaction();
        fragmenttrans.replace(R.id.framescoreboard, fragment);
        fragmenttrans.commit();
    }

    public void updatePlayersInTable(ArrayList<GrpcPlayer> realplayers) {
        Player1Name.setText(realplayers.get(0).getPlayerName());
        Player2Name.setText(realplayers.get(1).getPlayerName());
        Player3Name.setText(realplayers.get(2).getPlayerName());
        Player4Name.setText(realplayers.get(3).getPlayerName());
        Player5Name.setText(realplayers.get(4).getPlayerName());
        Player6Name.setText(realplayers.get(5).getPlayerName());

        /* only player 4,5,6 because 3-6 players are the rules*/
        if (realplayers.get(3).getPlayerName() == null) {
            Player4Name.setText("");
        }
        if (realplayers.get(4).getPlayerName() == null) {
            Player5Name.setText("");
        }
        if (realplayers.get(5).getPlayerName() == null) {
            Player6Name.setText("");
        }
    }

    public void updatePointsInTable(ArrayList<GrpcPlayer> realplayers) {
        Player1Points.setText(realplayers.get(0).getPoints());
        Player2Points.setText(realplayers.get(1).getPoints());
        Player3Points.setText(realplayers.get(2).getPoints());
        Player4Points.setText(realplayers.get(3).getPoints());
        Player5Points.setText(realplayers.get(4).getPoints());
        Player6Points.setText(realplayers.get(5).getPoints());
    }


    private class GameActionRunnable implements GrpcRunnableNew {
        private Throwable failed;

        @Override
        public String run(
                GameActionsGrpc.GameActionsBlockingStub blockingStub,
                GameActionsGrpc.GameActionsStub asyncStub,
                WeakReference<Activity> activityReference)
                throws Exception {
            return updateScoreboard(asyncStub, activityReference);
        }

        @Override
        public void doWhenDone(WeakReference<Activity> activityReference) {
        }

        /**
         * Bi-directional example, which can only be asynchronous. Send some chat messages, and
         * print any chat messages that are sent from the server.
         */
        private String updateScoreboard(
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

                                @Override
                                public void onNext(Response value) {

                                }

                                @Override
                                public void onError(Throwable t) {

                                }

                                @Override
                                public void onCompleted() {

                                }

                                private void updateRoundAndPoints(
                                        Activity activity, GameStatus gameStatus) {

                                    updatePlayersInTable(Player.getGrpcPlayerList);
                                    updatePointsInTable(Player.getGrpcPlayerList);
                                    Roundcounter.setText(gameStatus.getRound());

                                }
                            });


        }
    }
}


