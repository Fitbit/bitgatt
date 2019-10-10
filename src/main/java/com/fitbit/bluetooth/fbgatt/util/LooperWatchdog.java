/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.util;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import timber.log.Timber;

/**
 * A class for determining if a provided looper is still responding, will log an error
 * if it doesn't respond before the typical ANR timeout of 5s.  This class is not suitable
 * for watching the main thread, though if your main thread isn't responding for longer than
 * the ANR timeout you should be seeing ANRs rendering any service this class could provide
 * moot.
 */

public class LooperWatchdog implements Handler.Callback {
    /*
     * The message ID
     */
    private static final int MESSAGE_QUEUE_STILL_ALIVE = 25341;
    /*
     * The time to wait before logging the stalled state
     */
    private static final long TIME_TO_WAIT = TimeUnit.SECONDS.toMillis(4);
    /*
     * Time to wait between checks
     */
    private static final long TIME_TO_WAIT_BETWEEN_CHECKS = TimeUnit.SECONDS.toMillis(60);
    /*
     * The looper that we would like to watch for stalls
     */
    private final Handler watchedLooper;
    // the main looper for scheduling probes
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    /*
     * The alert runnable for telling us that we are stalled and which HandlerThread it was
     */
    private final Runnable alertRunnable = new Runnable() {
        @Override
        public void run() {
            Timber.e(new LooperDeadException(watchedLooper.getLooper()),
                "Watched Looper, %s not responding quickly, performance impacted.",
                watchedLooper.getLooper().getThread().getName());
        }
    };

    /**
     * The initializer for the looper watchdog, will take a looper that we would like to watch
     * @param targetLooper The target looper
     */
    public LooperWatchdog(Looper targetLooper) {
        this.watchedLooper = new Handler(targetLooper, this);
    }

    /**
     * Starts the probing of the looper every @{LooperWatchdog#TIME_TO_WAIT_BETWEEN_CHECKS} seconds
     */

    public void startProbing() {
        Message message = Message.obtain(watchedLooper);
        message.what = MESSAGE_QUEUE_STILL_ALIVE;
        watchedLooper.sendMessage(message);
        mainHandler.postDelayed(alertRunnable, TIME_TO_WAIT);
    }

    /**
     * Will stop probing the looper
     */

    public void stopProbing(){
        watchedLooper.removeCallbacksAndMessages(null);
        mainHandler.removeCallbacksAndMessages(null);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    void continueProbing(){
        Message message = Message.obtain(watchedLooper);
        // if we are testing message will be null
        if(message == null) {
            message = new Message();
            message.setTarget(watchedLooper);
        }
        message.what = MESSAGE_QUEUE_STILL_ALIVE;
        watchedLooper.sendMessageDelayed(message, TIME_TO_WAIT_BETWEEN_CHECKS);
        mainHandler.postDelayed(alertRunnable, TIME_TO_WAIT_BETWEEN_CHECKS + TIME_TO_WAIT);
    }

    /**
     * Handles the callback from the target looper that should disarm the alert
     * @param msg The MSG, probably @{MESSAGE_QUEUE_STILL_ALIVE}
     * @return True if the message was handled by this subclass, false otherwise
     */

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        switch(msg.what) {
            case MESSAGE_QUEUE_STILL_ALIVE:
                // disarm
                mainHandler.removeCallbacks(alertRunnable);
                // schedule next
                continueProbing();
                break;
            default:
                // nothing
                break;
        }
        return true;
    }

    static class LooperDeadException extends Exception {
        LooperDeadException(Looper watchedLooper){
            super("Looper must be dead, no response");
            this.setStackTrace(watchedLooper.getThread().getStackTrace());
        }
    }
}
