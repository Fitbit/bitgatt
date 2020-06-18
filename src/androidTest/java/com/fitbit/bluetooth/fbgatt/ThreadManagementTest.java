/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.util.NoOpGattCallback;

import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import androidx.test.platform.app.InstrumentationRegistry;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;


public class ThreadManagementTest {

    private static final String MOCK_ADDRESS = "02:00:00:00:00:00";
    private final ExecutorService service = Executors.newFixedThreadPool(100);


    @Before
    public void before() {
        FitbitGatt.getInstance().startGattClient(InstrumentationRegistry.getInstrumentation().getContext());
    }

    @After
    public void after() {
        FitbitGatt.getInstance().shutdown();
        FitbitGatt.setInstance(null);
    }

    @Test
    public void testTransactionQueueControllerThreadStoppage() {
        TransactionQueueController controller = new TransactionQueueController();
        controller.start();
        controller.stop();
        assertTrue("Thread should be stopped", controller.isQueueThreadStopped());
    }

    @Test
    public void testReleasedConnectionHasStoppedThread() {
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(MOCK_ADDRESS, "fooDevice");
        GattConnection conn = new GattConnection(device, InstrumentationRegistry.getInstrumentation().getTargetContext().getMainLooper());
        // finish is always called by ttl expiry
        conn.finish();
        assertTrue("Conn thread should be stopped", conn.getClientTransactionQueueController().isQueueThreadStopped());
    }

    @Test
    public void testRunningRunnableOnStoppedThreadShouldRestart() throws InterruptedException {
        TransactionQueueController controller = new TransactionQueueController();
        controller.start();
        controller.stop();
        CountDownLatch cdl = new CountDownLatch(1);
        final int[] total = new int[1];
        controller.queueTransaction(() -> {
            int i;
            for (i = 0; i < 100000; i++) {
                i++;
            }
            total[0] = i;
            cdl.countDown();
        });
        cdl.await(1000, TimeUnit.MILLISECONDS);
        assertEquals(100000, total[0]);
        assertFalse(controller.isQueueThreadStopped());
        controller.stop();
    }

    @Test
    public void testSynchronizedControllerStart() throws InterruptedException {
        TransactionQueueController controller = new TransactionQueueController();
        controller.start();
        controller.stop();
        final int MAX_THREAD_COUNT = 10;
        final int MAX_LOOP_COUNT = 5000;

        CountDownLatch cdl = new CountDownLatch(MAX_THREAD_COUNT);
        final int[] total = new int[1];
        total[0] = 0;
        for (int i = 0; i < MAX_THREAD_COUNT; i++) {
            service.submit(() -> {
                controller.queueTransaction(() -> {
                    int i1;
                    for (i1 = 0; i1 < MAX_LOOP_COUNT; i1++) {
                        total[0] += 1;
                    }
                    cdl.countDown();
                });
            });
        }
        cdl.await(1000, TimeUnit.MILLISECONDS);
        assertEquals(MAX_THREAD_COUNT * MAX_LOOP_COUNT, total[0]);
        controller.stop();
    }

    /**
     * Negative test for gatt start, worst case scenario, if synchronization isn't working
     * will crash the bluetooth service and will start returning calling back that start isn't
     * working
     */
    @Test
    public void testHammeringStartGattClientFromOneHundredThreads() {
        testHammerStart(new TestHammerRunner() {
            @Override
            public NoOpGattCallback getCB(CountDownLatch cdl, Context ctx) {
                return new NoOpGattCallback() {
                    @Override
                    public void onGattClientStarted() {
                        super.onGattClientStarted();
                        cdl.countDown();
                    }
                };
            }

            @Override
            public Runnable getRunnable(FitbitGatt gatt, CountDownLatch cdl, Context ctx) {
                return () -> {
                    gatt.startGattClient(ctx);
                    cdl.countDown();
                };
            }
        });
    }

    @Test
    public void testHammeringStartGattServerFromOneHundredThreads() {
        testHammerStart(new TestHammerRunner() {
            @Override
            public NoOpGattCallback getCB(CountDownLatch cdl, Context ctx) {
                return new NoOpGattCallback() {
                    @Override
                    public void onGattServerStarted(GattServerConnection connection) {
                        super.onGattClientStarted();
                        cdl.countDown();
                    }
                };
            }

            @Override
            public Runnable getRunnable(FitbitGatt gatt, CountDownLatch cdl, Context ctx) {
                return () -> {
                    gatt.startGattServer(ctx);
                    cdl.countDown();
                };
            }
        });
    }

    private interface TestHammerRunner {
        NoOpGattCallback getCB(CountDownLatch cdl, Context ctx);

        Runnable getRunnable(FitbitGatt gatt, CountDownLatch cdl, Context ctx);
    }


    private void testHammerStart(TestHammerRunner runner) {
        FitbitGatt.getInstance().shutdown();
        FitbitGatt.setInstance(null);
        CountDownLatch cdl = new CountDownLatch(1001);
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        FitbitGatt gatt = FitbitGatt.getInstance();
        gatt.registerGattEventListener(runner.getCB(cdl, context));
        for (int i = 0; i < 1000; i++) {
            service.execute(runner.getRunnable(gatt, cdl, context));
        }
        try {
            boolean timedout = !cdl.await(30, TimeUnit.SECONDS);
            if (timedout) {
                fail("test timed out waiting for start " + cdl.getCount());
            }
        } catch (InterruptedException ex) {
            fail("Thread was interrupted before all calls complete");
        }
        assertTrue(FitbitGatt.getInstance().isInitialized());
        FitbitGatt.getInstance().unregisterAllGattEventListeners();
    }
}
