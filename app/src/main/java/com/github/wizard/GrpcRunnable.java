package com.github.wizard;

import android.app.Activity;

import java.lang.ref.WeakReference;

public interface GrpcRunnable {
    /**
     * Perform a grpcRunnable and return all the logs.
     */
    String run(WeakReference<Activity> activityReference) throws Exception;

    void doWhenDone(WeakReference<Activity> activityReference);
}
