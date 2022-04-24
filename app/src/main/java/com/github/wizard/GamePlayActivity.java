package com.github.wizard;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.wizard.api.GameActionsGrpc;
import com.github.wizard.api.GameMove;
import com.github.wizard.api.Response;

import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

public class GamePlayActivity extends AppCompatActivity {

    ImageView card_01, card_02, card_03, card_04, card_05, card_06;
    Random r;

    public static String gameId;
    public static String playerId;
    private static BlockingQueue<GameMove> serverWaitingQueue = new LinkedBlockingQueue<>();
    private ManagedChannel channel;

    private static void appendLogs(StringBuffer logs, String msg, Object... params) {
        if (params.length > 0) {
            logs.append(MessageFormat.format(msg, params));
        } else {
            logs.append(msg);
        }
        logs.append("\n");
    }

    private static GameMove newGameMove(int type, String message) {
        return GameMove.newBuilder().setGameid(GamePlayActivity.gameId).setPlayerid(GamePlayActivity.playerId).setData(message).setType(type + "")
                .build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_play);
        Intent intent = getIntent();
        gameId = intent.getStringExtra(MainActivity.GAME_ID_KEY);//reuse for later requests
        playerId = intent.getStringExtra(MainActivity.PLAYER_ID_KEY);
        new GameActionRunner(new GameActionRunnable(), new WeakReference<>(this), channel).execute();//fire up the streaming service
        findViewById(R.id.button_estimate).setOnClickListener(this::submitEstimate);
        findViewById(R.id.button_play_card).setOnClickListener(this::playCard);

        /*card_01 = (ImageView) findViewById(R.id.card_01);
        card_02 = (ImageView) findViewById(R.id.card_02);
        card_03 = (ImageView) findViewById(R.id.card_03);
        card_04 = (ImageView) findViewById(R.id.card_04);
        card_05 = (ImageView) findViewById(R.id.card_05);
        card_06 = (ImageView) findViewById(R.id.card_06);*/

    }

    private void playCard(View view) {
        (findViewById(R.id.editTextN_card)).setVisibility(View.GONE);
        (findViewById(R.id.button_play_card)).setVisibility(View.GONE);

        //submit to server
        EditText card = (EditText) findViewById(R.id.editTextN_card);
        serverWaitingQueue.add(newGameMove(2, card.getText().toString()));
    }

    private void submitEstimate(View view) {
        (findViewById(R.id.editTextNumber_estimate)).setVisibility(View.GONE);
        (findViewById(R.id.button_estimate)).setVisibility(View.GONE);

        //submit to server
        EditText card = (EditText) findViewById(R.id.editTextNumber_estimate);
        serverWaitingQueue.add(newGameMove(1, card.getText().toString()));
    }

    private static class GameActionRunnable implements GrpcRunnableNew {
        private Throwable failed;

        @Override
        public String run(GameActionsGrpc.GameActionsBlockingStub blockingStub, GameActionsGrpc.GameActionsStub asyncStub, WeakReference<Activity> activityReference)
                throws Exception {
            return updateGameBoard(asyncStub, activityReference);
        }

        @Override
        public void doWhenDone(WeakReference<Activity> activityReference) {

        }

        /**
         * Bi-directional example, which can only be asynchronous. Send some chat messages, and print
         * any chat messages that are sent from the server.
         */
        private String updateGameBoard(GameActionsGrpc.GameActionsStub asyncStub, WeakReference<Activity> activityReference)
                throws InterruptedException, RuntimeException {
            final StringBuffer logs = new StringBuffer();
            appendLogs(logs, "*** GamePlay");
            final CountDownLatch finishLatch = new CountDownLatch(1);
            StreamObserver<GameMove> requestObserver =
                    asyncStub.gameStream(
                            /**
                             * Receives a value from the stream.
                             *
                             * <p>Can be called many times but is never called after {@link #onError(Throwable)} or {@link
                             * #onCompleted()} are called.
                             *
                             * <p>Unary calls must invoke onNext at most once.  Clients may invoke onNext at most once for
                             * server streaming calls, but may receive many onNext callbacks.  Servers may invoke onNext at
                             * most once for client streaming calls, but may receive many onNext callbacks.
                             *
                             * <p>If an exception is thrown by an implementation the caller is expected to terminate the
                             * stream by calling {@link #onError(Throwable)} with the caught exception prior to
                             * propagating it.
                             *
                             * @param value the value passed to the stream
                             */

                            new StreamObserver<Response>() {
                                @Override
                                public void onNext(Response value) {
                                    Activity activity = activityReference.get();
                                    if (activity == null) {
                                        return;
                                    }
                                    switch (value.getType()) {
                                        case "0"://general ok
                                            break;
                                        case "1"://show who has made the stich
                                            activity.runOnUiThread(() -> {
                                                Toast.makeText(activity.getApplication().getApplicationContext(), value.getData(), Toast.LENGTH_SHORT).show();
                                                //todo inrease counter if it was me
                                            });
                                            break;
                                        case "2":
                                            activity.runOnUiThread(() -> {
                                                Toast.makeText(activity.getApplication().getApplicationContext(), value.getData(), Toast.LENGTH_SHORT).show();
                                            });
                                            break;
                                        case "3": //display cards
                                            activity.runOnUiThread(() -> {
                                                ((TextView) activity.findViewById(R.id.cards_in_Hand)).setText(value.getData().split("//")[0]);
                                                ((TextView) activity.findViewById(R.id.cards_on_table)).setText(value.getData().split("//").length > 1 ? value.getData().split("//")[1] : "Nothing here yet...");
                                                Toast.makeText(activity.getApplication().getApplicationContext(), "your have received your cards", Toast.LENGTH_SHORT).show();
                                            });
                                            break;
                                        case "4"://show em trump
                                            activity.runOnUiThread(() -> {
                                                ((TextView) activity.findViewById(R.id.trumpf)).setText("Trumpf is " + value.getData());
                                                Toast.makeText(activity.getApplication().getApplicationContext(), "trumpf is: " + value.getData(), Toast.LENGTH_SHORT).show();
                                            });
                                            break;
                                        case "5"://ask user for his estimate
                                            activity.runOnUiThread(() -> {
                                                ((TextView) activity.findViewById(R.id.editTextNumber_estimate)).setVisibility(View.VISIBLE);
                                                ((TextView) activity.findViewById(R.id.button_estimate)).setVisibility(View.VISIBLE);
                                            });
                                            break;
                                        case "6"://display points
                                            activity.runOnUiThread(() -> {
                                                ((TextView) activity.findViewById(R.id.points)).setText("You have " + value.getData().split("/")[0] + " points");
                                                ((TextView) activity.findViewById(R.id.round)).setText("This is round " + value.getData().split("/")[1]);

                                                Toast.makeText(activity.getApplication().getApplicationContext(), "after round " + value.getData().split("/")[1] + " you have " + value.getData().split("/")[0] + " points!", Toast.LENGTH_SHORT).show();

                                            });
                                        default:
                                            throw new IllegalArgumentException("type not implemented");
                                    }
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
                serverWaitingQueue.add(newGameMove(0, "suscribing to updates"));//initial subscription reques

                while (true) {
                    GameMove request = serverWaitingQueue.take();//waits for some new data to become available, blocking
                    if (request.getType().equals("-1"))
                        break;
                    appendLogs(
                            logs,
                            "Sending message \"{0}\"",
                            request.getData());
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
            if (!finishLatch.await(1, TimeUnit.HOURS)) {//max play time
                throw new RuntimeException(
                        "Could not finish rpc within " + MainActivity.SERVER_TIMEOUT_SECONDS + " seconds, the server is likely down");
            }

            if (failed != null) {
                throw new RuntimeException(failed);
            }

            return logs.toString();
        }
    }

}
