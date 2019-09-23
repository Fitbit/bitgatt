/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ThreadManagementTest {

    private static final String MOCK_ADDRESS = "02:00:00:00:00:00";
    private final ExecutorService service = Executors.newFixedThreadPool(100);


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
        GattConnection conn = new GattConnection(device, InstrumentationRegistry.getTargetContext().getMainLooper());
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
            for(i=0; i < 100000; i++) {
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

    /**
     * Negative test for gatt start, worst case scenario, if synchronization isn't working
     * will crash the bluetooth service and will start returning calling back that start isn't
     * working
     */
    @Test
    public void testHammeringStartFromOneHundredThreads(){
        CountDownLatch cdl = new CountDownLatch(1001);
        FitbitGatt.getInstance().registerGattEventListener(new NoOpGattCallback() {
            @Override
            public void onFitbitGattReady() {
                // this should only happen once
                cdl.countDown();
            }

            @Override
            public void onFitbitGattStartFailed() {
                fail("Gatt start should never fail");
            }
        });
        for(int i=0; i < 1000; i++) {
            service.execute(new Runnable() {
                @Override
                public void run() {
                    FitbitGatt.getInstance().start(InstrumentationRegistry.getTargetContext());
                    cdl.countDown();
                }
            });
        }
        try {
            boolean timedout = !cdl.await(30, TimeUnit.SECONDS);
            if(timedout) {
                fail("test timed out waiting for start");
            }
        } catch (InterruptedException ex) {
            fail("Thread was interrupted before all calls complete");
        }
        assertTrue(FitbitGatt.getInstance().isStarted());
        FitbitGatt.getInstance().unregisterAllGattEventListeners();
    }

    public class NoOpGattCallback implements FitbitGatt.FitbitGattCallback {

        @Override
        public void onBluetoothPeripheralDiscovered(GattConnection connection) {

        }

        @Override
        public void onBluetoothPeripheralDisconnected(GattConnection connection) {

        }

        @Override
        public void onFitbitGattReady() {

        }

        @Override
        public void onFitbitGattStartFailed() {

        }

        @Override
        public void onScanStarted() {

        }

        @Override
        public void onScanStopped() {

        }

        @Override
        public void onPendingIntentScanStopped() {

        }

        @Override
        public void onPendingIntentScanStarted() {

        }

        @Override
        public void onBluetoothOff() {

        }

        @Override
        public void onBluetoothOn() {

        }

        @Override
        public void onBluetoothTurningOn() {

        }

        @Override
        public void onBluetoothTurningOff() {

        }
    }
}
