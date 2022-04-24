package com.github.wizard;

import android.app.Activity;
import com.github.wizard.api.GameActionsGrpc;
import java.lang.ref.WeakReference;

public interface GrpcRunnableNew {
    /** Perform a grpcRunnable and return all the logs. */
    String run(
            GameActionsGrpc.GameActionsBlockingStub blockingStub,
            GameActionsGrpc.GameActionsStub asyncStub,
            WeakReference<Activity> activityReference)
            throws Exception;

    void doWhenDone(WeakReference<Activity> activityReference);
}
