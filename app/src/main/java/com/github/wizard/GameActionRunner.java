package com.github.wizard;

import android.app.Activity;
import android.os.AsyncTask;

import com.github.wizard.api.GameActionsGrpc;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class GameActionRunner extends AsyncTask<Void, Void, String> {
    private final GrpcRunnableNew grpcRunnable;
    private final WeakReference<Activity> activityReference;
    private ManagedChannel channel;
    private GameActionsGrpc.GameActionsBlockingStub gamePlayBlockingStub;
    private GameActionsGrpc.GameActionsStub gamePlayStub;

    GameActionRunner(GrpcRunnableNew grpcRunnable, WeakReference<Activity> activity, ManagedChannel channel) {
        this.grpcRunnable = grpcRunnable;
        this.activityReference = activity;
        this.channel = channel;
    }

    /**
     * @param voids
     * @deprecated
     */
    @Override
    protected String doInBackground(Void... voids) {
        boolean reconnect = false;
        try {
            if (channel == null || channel.isShutdown()) {
                channel = ManagedChannelBuilder.forAddress(MainActivity.SERVER_ADDRESS, Integer.parseInt(MainActivity.SERVER_PORT)).usePlaintext().build();
                reconnect = true;
            }
            if (gamePlayBlockingStub == null || reconnect)
                gamePlayBlockingStub = GameActionsGrpc.newBlockingStub(channel);
            if (gamePlayStub == null || reconnect)
                gamePlayStub = GameActionsGrpc.newStub(channel);
            String logs = grpcRunnable.run(gamePlayBlockingStub, gamePlayStub, activityReference);//ron whatever we had to run
            return "Success!\n" + logs;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.flush();
            return "Failed... :\n" + sw;
        }
    }
}
