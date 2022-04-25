package com.github.wizard;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.github.wizard.api.GameActionsGrpc;
import com.github.wizard.api.GameMove;
import com.github.wizard.api.Response;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class GamePlayActivity extends AppCompatActivity {

    // ArrayList<ImageView> cards = new ArrayList<>(6);
    // Random r;

    public static String gameId;
    public static String playerId;
    private static final BlockingQueue<GameMove> serverWaitingQueue = new LinkedBlockingQueue<>();
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

        /*
        cards.add(findViewById(R.id.card1));
        cards.add(findViewById(R.id.card2));
        cards.add(findViewById(R.id.card3));
        cards.add(findViewById(R.id.card4));
        cards.add(findViewById(R.id.card5));
        cards.add(findViewById(R.id.card6));
        */

    }

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

    private static class GameActionRunnable implements GrpcRunnableNew {
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

                                private void showStich(Activity activity, Response response) {
                                    Toast.makeText(
                                                    activity.getApplication()
                                                            .getApplicationContext(),
                                                    response.getData(),
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                    // todo inrease counter if it was me
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

                                private void updateGameField(Activity activity, Response response) {
                                    ((TextView) activity.findViewById(R.id.cards_in_Hand))
                                            .setText(response.getData().split("//")[0]);
                                    ((TextView) activity.findViewById(R.id.cards_on_table))
                                            .setText(
                                                    response.getData().split("//").length > 1
                                                            ? response.getData().split("//")[1]
                                                            : "Nothing here" + " yet...");
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

                                private void updateRoundNumberAndPoints(
                                        Activity activity, Response response) {
                                    ((TextView) activity.findViewById(R.id.points))
                                            .setText(
                                                    "You have "
                                                            + response.getData().split("/")[0]
                                                            + " points");
                                    ((TextView) activity.findViewById(R.id.round))
                                            .setText(
                                                    "This is round "
                                                            + response.getData().split("/")[1]);

                                    Toast.makeText(
                                                    activity.getApplication()
                                                            .getApplicationContext(),
                                                    "after round "
                                                            + response.getData().split("/")[1]
                                                            + " you have "
                                                            + response.getData().split("/")[0]
                                                            + " points!",
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                }

                                private void handleResponse(Activity activity, Response response) {
                                    switch (response.getType()) {
                                        case "0":
                                            break;
                                        case "1":
                                            activity.runOnUiThread(
                                                    () -> showStich(activity, response));
                                            break;
                                        case "2":
                                            activity.runOnUiThread(
                                                    () -> makeCardPlayRequest(activity, response));
                                            break;
                                        case "3":
                                            activity.runOnUiThread(
                                                    () -> updateGameField(activity, response));
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
                                            activity.runOnUiThread(
                                                    () ->
                                                            updateRoundNumberAndPoints(
                                                                    activity, response));
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
}
