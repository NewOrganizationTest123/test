package com.example.myapplication;

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
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import server.GameStarterGrpc;
import server.StartReply;
import server.StartRequest;

public class MainActivity extends AppCompatActivity {
    public static final String GAME_ID_KEY = "com.example.myapplication.GAME_ID_KEY";
    public static final String TAG = "wizzard";

    private EditText name;
    private Button startGame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        name = findViewById(R.id.editTextTextPersonName);
        startGame = findViewById(R.id.button);
        startGame.setOnClickListener(view -> startNewGame(view));
    }

    public void startNewGame(View view) {
        new GrpcTask(this)
                .execute(
                        "10.0.2.2",//todo use real host name and port
                        name.getText().toString(),
                        "50051");
    }

    private static class GrpcTask extends AsyncTask<String, Void, String> {
        private final WeakReference<Activity> activityReference;
        private ManagedChannel channel;

        private GrpcTask(Activity activity) {
            this.activityReference = new WeakReference<Activity>(activity);
        }


        @Override
        protected String doInBackground(String... params) {
            String host = params[0];
            String message = params[1];
            String portStr = params[2];
            int port = TextUtils.isEmpty(portStr) ? 0 : Integer.valueOf(portStr);
            try {
                channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
                GameStarterGrpc.GameStarterBlockingStub stub = GameStarterGrpc.newBlockingStub(channel);
                StartRequest request = StartRequest.newBuilder().setName(message).build();
                StartReply reply = stub.startGame(request);
                return reply.getGameid();
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                Log.e(TAG, String.valueOf(e.getStackTrace()));
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