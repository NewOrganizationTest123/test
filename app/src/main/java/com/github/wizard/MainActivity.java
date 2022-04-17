package com.github.wizard;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import com.github.wizard.api.GameStarterGrpc;
import com.github.wizard.api.JoinRequest;
import com.github.wizard.api.StartReply;
import com.github.wizard.api.StartRequest;

public class MainActivity extends AppCompatActivity {
    public static final String GAME_ID_KEY = "com.github.wizard.GAME_ID_KEY";
    public static final String TAG = "wizard";
    public static final String SERVER_ADDRESS = "10.0.2.2";//this is the address for localhost on the host of the emulator. todo use real server address
    public static final String SERVER_PORT = "50051";// todo use real port

    private EditText name;
    private Button startGame;
    private EditText gameId;
    private Button joinGame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        name = findViewById(R.id.editTextTextPersonName);
        gameId = findViewById(R.id.editTextNumber);
        joinGame = findViewById(R.id.button3);
        startGame = findViewById(R.id.button);
        startGame.setOnClickListener(this::startNewGame);
        joinGame.setOnClickListener(this::joinGame);

    }

    public void startNewGame(View view) {
        new GrpcTask(this, true)
                .execute(
                        SERVER_ADDRESS,
                        name.getText().toString(),
                        SERVER_PORT, "-1");
    }

    public void joinGame(View view) {
        new GrpcTask(this, false)
                .execute(
                        SERVER_ADDRESS,
                        name.getText().toString(),
                        SERVER_PORT, gameId.getText().toString()
                );
    }

    private static class GrpcTask extends AsyncTask<String, Void, String> {
        private final WeakReference<Activity> activityReference;
        private ManagedChannel channel;
        private final boolean startNewGame;


        private GrpcTask(Activity activity, boolean startNewGame) {
            this.activityReference = new WeakReference<>(activity);
            this.startNewGame = startNewGame;
        }

        @Override
        protected String doInBackground(String... params) {
            String host = params[0];
            String name = params[1];
            String portStr = params[2];
            String gameid = params[3];

            int port = TextUtils.isEmpty(portStr) ? 0 : Integer.parseInt(portStr);
            try {
                channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
                GameStarterGrpc.GameStarterBlockingStub stub = GameStarterGrpc.newBlockingStub(channel);
                if (startNewGame) {

                    StartRequest request = StartRequest.newBuilder().setName(name).build();
                    StartReply reply = stub.startGame(request);
                    return reply.getGameid();
                } else {
                    JoinRequest request = JoinRequest.newBuilder().setGameid(gameid).setName(name).build();
                    StartReply reply = stub.joinGame(request);
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
            try {
                channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Activity activity = activityReference.get();
            if (activity == null) {
                return;
            }

            Intent intent = new Intent(activity, GamePlayActivity.class);
            intent.putExtra(GAME_ID_KEY, gameId);
            activity.startActivity(intent);
        }
    }
}
