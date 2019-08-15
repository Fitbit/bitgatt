/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

import static org.junit.Assert.*;

/**
 * Primarily to test start idempotency, then because anyone can use the broadcast receiver, and it
 * might be started by the system as it could be added to the manifest ( is probably added to
 * the manifest ) and start() will try to manually register, it could register twice, for that
 * reason we'll ensure that we unregister first, then register it programmatically if desired.
 *
 * Created by iowens on 8/14/19.
 */

@RunWith(AndroidJUnit4.class)
public class FitbitGattTest {
    static Context context;

    @BeforeClass
    public static void beforeClass() {
        context = InstrumentationRegistry.getContext();
    }

    @Test
    public void testStartMultipleRegistrations(){
        ArrayList<Runnable> runnables = new ArrayList<>();
        for(int i=0; i < 1001; i++) {
            final int internalCount = i;
            runnables.add(() -> {
                Timber.i("Calling start #%d...", internalCount);
                FitbitGatt.getInstance().start(context);
            });
        }
        try {
            assertConcurrent("Testing start", runnables, 30);
            assertEquals("No matter how many times we call register it should be 1", 1, LowEnergyAclListener.timesRegistered.get());
        } catch (InterruptedException e) {
            fail("Test was interrupted");
        }
    }

    @Test
    public void testBroadcastReceiverAlone(){
        FitbitGatt.getInstance().start(context);
        LowEnergyAclListener listener = FitbitGatt.getInstance().aclListener;
        ArrayList<Runnable> runnables = new ArrayList<>();
        for(int i=0; i < 1001; i++) {
            final int internalCount = i;
            runnables.add(() -> {
                Timber.i("Calling start #%d...", internalCount);
                listener.register(context);
            });
        }
        try {
            assertConcurrent("Testing start", runnables, 30);
            assertEquals("No matter how many times we call register it should be 1", 1, LowEnergyAclListener.timesRegistered.get());
        } catch (InterruptedException e) {
            fail("Test was interrupted");
        }
    }

    private static void assertConcurrent(final String message, final List<? extends Runnable> runnables, final int maxTimeoutSeconds) throws InterruptedException {
        final int numThreads = runnables.size();
        final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());
        final ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        try {
            final CountDownLatch allExecutorThreadsReady = new CountDownLatch(numThreads);
            final CountDownLatch afterInitBlocker = new CountDownLatch(1);
            final CountDownLatch allDone = new CountDownLatch(numThreads);
            for (final Runnable submittedTestRunnable : runnables) {
                threadPool.submit(() -> {
                    allExecutorThreadsReady.countDown();
                    try {
                        afterInitBlocker.await();
                        submittedTestRunnable.run();
                    } catch (final Throwable e) {
                        exceptions.add(e);
                    } finally {
                        allDone.countDown();
                    }
                });
            }
            // wait until all threads are ready
            assertTrue("Timeout initializing threads! Perform long lasting initializations before passing runnables to assertConcurrent", allExecutorThreadsReady.await(runnables.size() * 10, TimeUnit.MILLISECONDS));
            // start all test runners
            afterInitBlocker.countDown();
            assertTrue(message +" timeout! More than" + maxTimeoutSeconds + "seconds", allDone.await(maxTimeoutSeconds, TimeUnit.SECONDS));
        } finally {
            threadPool.shutdownNow();
        }
        assertTrue(message + "failed with exception(s)" + exceptions, exceptions.isEmpty());
    }
}
