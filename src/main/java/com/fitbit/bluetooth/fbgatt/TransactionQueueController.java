/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.VisibleForTesting;
import timber.log.Timber;

class TransactionQueueController {

    private String threadName;
    private AtomicBoolean stopped = new AtomicBoolean(true);
    private final LinkedBlockingQueue<Runnable> transactionQueue = new LinkedBlockingQueue<>();
    private ClientThread transactionThread;

    TransactionQueueController(String name) {
        this.threadName = name;
    }

    TransactionQueueController(GattConnection connection) {
        this(String.format(Locale.ENGLISH, "%s GATT Transaction Thread", connection.getDevice().getName()));
    }

    TransactionQueueController() {
        this("GATT Server Transaction Thread");
    }

    void queueTransaction(Runnable tx) {
        if (stopped.get()) {
            Timber.i("Implicitly restarting queue");
            start();
        }
        transactionQueue.add(tx);
    }

    void clearQueue() {
        transactionQueue.clear();
    }

    @VisibleForTesting
    synchronized void start() {
        if (stopped.compareAndSet(true, false)) {
            Timber.v("Starting execution thread");
            if (transactionThread != null) {
                transactionThread.interrupt();
                transactionThread = null;
            }
            transactionThread = new ClientThread(threadName);
            transactionThread.setPriority(Thread.MAX_PRIORITY);
            transactionThread.start();
        }
    }

    synchronized void stop() {
        if (!isQueueThreadStopped()) {
            Timber.v("Stopping execution thread");
            stopped.compareAndSet(false, true);
            clearQueue();
            transactionThread.interrupt();
            transactionThread = null;
        }
    }

    @VisibleForTesting
    boolean isQueueThreadStopped() {
        return transactionThread == null || transactionThread.isInterrupted();
    }

    private class ClientThread extends Thread {

        ClientThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            Runnable tx;
            while (!stopped.get()) {
                try {
                    tx = transactionQueue.take();
                    tx.run();
                } catch (InterruptedException e) {
                    Timber.i("Thread was interrupted, it's OK, we will retake");
                }
            }
            Timber.i("Thread was stopped");
        }
    }

}
