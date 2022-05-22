package com.github.wizard;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScoreboardActivity extends AppCompatActivity {

    public static String gameId;
    public static String playerId;
    public static String playername;
    public static ArrayList<String> players = new ArrayList<>(); // todo maybe not use String here
    public static com.github.wizard.GamePlayActivity.PlayersRecyclerviewAdapter players_adapter;

    ManagedChannel channel;
    private static final BlockingQueue<GameMove> serverWaitingQueue = new LinkedBlockingQueue<>();
    private static RecyclerView playersRecyclerView;
    private TextView points;
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
                .setGameid(com.github.wizard.GamePlayActivity.gameId)
                .setPlayerid(com.github.wizard.GamePlayActivity.playerId)
                .setData(message)
                .setType(type + "")
                .build();
    }

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
        points = findViewById(R.id.points);
        playersRecyclerView = findViewById(R.id.playerRecyclerView);


        LinearLayoutManager layoutManagerPlayers = new LinearLayoutManager(this);
        playersRecyclerView.setLayoutManager(layoutManagerPlayers);

        players_adapter = new com.github.wizard.GamePlayActivity.PlayersRecyclerviewAdapter(this, players);
        playersRecyclerView.setAdapter(players_adapter);

        showscore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                replacefragment(new ScoreboardFragment());
            }
        });
    }

    private void replacefragment(Fragment fragment) {
        FragmentManager fragmentm = getSupportFragmentManager();
        FragmentTransaction fragmenttrans = fragmentm.beginTransaction();
        fragmenttrans.replace(R.id.framescoreboard, fragment);
        fragmenttrans.commit();
    }

    public void updatePlayersInTable(ArrayList<String> realplayers) {
        Player1Name.setText(realplayers.get(0));
        Player2Name.setText(realplayers.get(1));
        Player3Name.setText(realplayers.get(2));
        Player4Name.setText(realplayers.get(3));
        Player5Name.setText(realplayers.get(4));
        Player6Name.setText(realplayers.get(5));
        com.github.wizard.GamePlayActivity.PlayersRecyclerviewAdapter newadapter = new com.github.wizard.GamePlayActivity.PlayersRecyclerviewAdapter(this, realplayers);
        playersRecyclerView.setAdapter(newadapter);
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
        public void doWhenDone(WeakReference<Activity> activityReference) {
        }

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

                                private void updateRoundNumberAndPoints(
                                        Activity activity, GameStatus gameStatus) {
                                    ((TextView) activity.findViewById(R.id.Player1Points))
                                            .setText(gameStatus.getMyPoints());
                                    ((TextView) activity.findViewById(R.id.Player2Points))
                                            .setText(gameStatus.getMyPoints());
                                    ((TextView) activity.findViewById(R.id.Player3Points))
                                            .setText(gameStatus.getMyPoints());
                                    ((TextView) activity.findViewById(R.id.Player4Points))
                                            .setText(gameStatus.getMyPoints());
                                    ((TextView) activity.findViewById(R.id.Player5Points))
                                            .setText(gameStatus.getMyPoints());
                                    ((TextView) activity.findViewById(R.id.Player6Points))
                                            .setText(gameStatus.getMyPoints());

                                    ((TextView) activity.findViewById(R.id.Roundscounter))
                                            .setText(gameStatus.getRound());

                                }
                            }
        }


        public class PlayersRecyclerviewAdapter
                extends RecyclerView.Adapter<com.github.wizard.GamePlayActivity.PlayersRecyclerviewAdapter.ViewHolder> {

            private ArrayList<String> players;
            private LayoutInflater layoutInflater;
            public String selectedPlayer;

            PlayersRecyclerviewAdapter(Context context, ArrayList<String> players) {
                this.layoutInflater = LayoutInflater.from(context);
                this.players = players;
                selectedPlayer = null;
            }

            @Override
            public com.github.wizard.GamePlayActivity.PlayersRecyclerviewAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
                View view =
                        layoutInflater.inflate(
                                R.layout.players_recyclerview_textfield, viewGroup, false);
                return new com.github.wizard.GamePlayActivity.PlayersRecyclerviewAdapter.ViewHolder(view);
            }

            @Override
            public void onBindViewHolder(com.github.wizard.GamePlayActivity.PlayersRecyclerviewAdapter.ViewHolder viewHolder, int position) {
                String playername = players.get(position);
                viewHolder.playername_holder.setText(playername);
            }
        }
    }
}


