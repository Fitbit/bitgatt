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

import timber.log.Timber;

class TransactionQueueController {

    private String threadName;
    private AtomicBoolean stop = new AtomicBoolean(false);
    private final LinkedBlockingQueue<Runnable> transactionQueue = new LinkedBlockingQueue<>();
    private ClientThread transactionThread;

    TransactionQueueController(GattConnection connection){
        this.threadName = String.format(Locale.ENGLISH, "%s GATT Transaction Thread", connection.getDevice().getName());
    }

    TransactionQueueController(){
        this.threadName = "GATT Server Transaction Thread";
    }

    TransactionQueueController(String name) {
        this.threadName = name;
    }


    void queueTransaction(Runnable tx) {
        if(stop.get()) {
            Timber.i("Implicitly restarting queue");
            start();
        }
        transactionQueue.add(tx);
    }

    /**
     * Will return the client thread being used to run transactions on this connection
     * @return The client transaction thread
     */

    ClientThread getTransactionThread(){
        return transactionThread;
    }

    void clearQueue(){
        transactionQueue.clear();
    }

    synchronized void start(){
        Timber.v("Starting execution thread");
        stop.compareAndSet(true, false);
        if(transactionThread != null) {
            transactionThread.interrupt();
            transactionThread = null;
        }
        transactionThread = new ClientThread(threadName);
        transactionThread.setPriority(Thread.MAX_PRIORITY);
        transactionThread.start();
    }

    synchronized void stop(){
        Timber.v("Stopping execution thread");
        stop.compareAndSet(false, true);
        clearQueue();
        transactionThread.interrupt();
        transactionThread = null;
    }

    boolean isQueueThreadStopped(){
        return transactionThread == null || transactionThread.isInterrupted();
    }

    private class ClientThread extends Thread {

        ClientThread(String name) {
            super(name);
        }

        @Override
        public void run(){
            Runnable tx;
            while(!stop.get()) {
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
