/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.tx.GattConnectTransaction;

import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Will test the behavior of the always connected scanner with the mock lollipop scanner
 */
public class AlwaysConnectedScannerTest {

    private static MockLollipopScanner mockScanner;
    private FitbitGatt gatt;
    private Context mockContext;
    private PeripheralScanner peripheralScanner;
    private AlwaysConnectedScanner alwaysConnectedScanner;
    private Handler mockHandler;
    private ScheduledExecutorService singleThreadExecutor = Executors.newSingleThreadScheduledExecutor();
    private Answer<Boolean> handlerPostAnswer = invocation -> {
        Long delay = 0L;
        if (invocation.getArguments().length > 1) {
            delay = invocation.getArgument(1);
        }
        Runnable msg = invocation.getArgument(0);
        if (msg != null) {
            singleThreadExecutor.schedule(msg, delay, TimeUnit.MILLISECONDS);
        }
        return true;
    };

    @BeforeClass
    public static void beforeClass() {
        mockScanner = MockLollipopScanner.BluetoothAdapter.getBluetoothLeScanner();
    }

    @Before
    public void before(){
        Looper mockMainThreadLooper = mock(Looper.class);
        Thread mockMainThread = mock(Thread.class);
        when(mockMainThread.getName()).thenReturn("Irvin's mock thread");
        when(mockMainThreadLooper.getThread()).thenReturn(mockMainThread);
        mockContext = mock(Context.class);
        when(mockContext.getApplicationContext()).thenReturn(mockContext);
        when(mockContext.registerReceiver(any(), any())).thenReturn(new Intent("some custom action"));
        when(mockContext.getMainLooper()).thenReturn(mockMainThreadLooper);
        mockHandler = mock(Handler.class);
        doAnswer(handlerPostAnswer).when(mockHandler).post(any(Runnable.class));
        doAnswer(handlerPostAnswer).when(mockHandler).postDelayed(any(Runnable.class), anyLong());
        when(mockHandler.getLooper()).thenReturn(mockMainThreadLooper);
        gatt = FitbitGatt.getInstance();
        gatt.start(mockContext);
        gatt.setScannerMockMode(true);
        MockLollipopScanner.BluetoothAdapter.turnBluetoothOn();
        alwaysConnectedScanner = gatt.getAlwaysConnectedScanner();
        alwaysConnectedScanner.setHandler(mockHandler);
        peripheralScanner = gatt.getPeripheralScanner();
        if(peripheralScanner == null) {
            fail("No peripheral scanner, must not have started");
            return;
        }
        peripheralScanner.injectMockScanner(mockScanner);
        peripheralScanner.setHandler(mockHandler);
        peripheralScanner.cancelScan(mockContext);
        peripheralScanner.cancelPeriodicalScan(mockContext);
        peripheralScanner.cancelHighPriorityScan(mockContext);
        peripheralScanner.cancelPendingIntentBasedBackgroundScan();
        alwaysConnectedScanner.stop(mockContext);
        alwaysConnectedScanner.restartCanStart();
    }

    @After
    public void after(){
        // dunno yet
    }

    /**
     * Will test to make sure that we can't do ad-hoc scanning while the scanner is already engaged
     */

    @Test
    public void testTryToScanAlwaysConnectedOnShouldNotStart(){
        // generic scan filter
        ScanFilter filter = new ScanFilter.Builder().build();
        alwaysConnectedScanner.setNumberOfExpectedDevices(1);
        alwaysConnectedScanner.setShouldKeepLooking(false);
        alwaysConnectedScanner.addScanFilter(mockContext, filter);
        assertTrue(alwaysConnectedScanner.start(mockContext));
        // there is a single set of filters for all scanners
        assertFalse(gatt.startHighPriorityScan(mockContext));
    }

    @Test
    public void testTryToScanAlwaysConnectedButRegularScannerRunning(){
        // generic scan filter
        ScanFilter filter = new ScanFilter.Builder().build();
        alwaysConnectedScanner.setNumberOfExpectedDevices(1);
        alwaysConnectedScanner.setShouldKeepLooking(false);
        alwaysConnectedScanner.addScanFilter(mockContext, filter);
        // there is a single set of filters for all scanners
        peripheralScanner.isScanning.set(true);
        assertFalse(alwaysConnectedScanner.start(mockContext));
    }

    @Test
    public void testTryToScanAwaysConnectedButPeriodicalScannerRunning(){
        // generic scan filter
        ScanFilter filter = new ScanFilter.Builder().build();
        alwaysConnectedScanner.setNumberOfExpectedDevices(1);
        alwaysConnectedScanner.setShouldKeepLooking(false);
        alwaysConnectedScanner.addScanFilter(mockContext, filter);
        // there is a single set of filters for all scanners
        peripheralScanner.periodicalScanEnabled.set(true);
        assertFalse(alwaysConnectedScanner.start(mockContext));
    }

    @Test
    public void testAlwaysConnectedFindOneDeviceThenStop() throws InterruptedException {
        // generic scan filter
        ScanFilter filter = new ScanFilter.Builder().build();
        alwaysConnectedScanner.setNumberOfExpectedDevices(1);
        alwaysConnectedScanner.setShouldKeepLooking(false);
        alwaysConnectedScanner.addScanFilter(mockContext, filter);
        CountDownLatch cdl = new CountDownLatch(1);
        AlwaysConnectedScannerListener callback = new AlwaysConnectedScannerListener() {
            @Override
            public void onPeripheralConnected(GattConnection connection) {
                cdl.countDown();
                assertFalse(peripheralScanner.isScanning());
                assertFalse(peripheralScanner.isPeriodicalScanEnabled());
                assertFalse(peripheralScanner.isPendingIntentScanning());
            }

            @Override
            public void onPeripheralConnectionError(TransactionResult result) {

            }
        };
        alwaysConnectedScanner.registerAlwaysConnectedScannerListener(callback);
        alwaysConnectedScanner.start(mockContext);
        mockHandler.postDelayed(() -> {
            GattConnection connection = new GattConnection(mock(FitbitBluetoothDevice.class), mockContext.getMainLooper());
            connection.setMockMode(true);
            connection.setState(GattState.CONNECTED);
            alwaysConnectedScanner.onBluetoothPeripheralDiscovered(connection);
        }, 1000);
        boolean result = cdl.await(5, TimeUnit.SECONDS);
        if(!result) {
            alwaysConnectedScanner.unregisterAlwaysConnectedScannerListener(callback);
            fail("Test timed out");
        } else {
            alwaysConnectedScanner.unregisterAlwaysConnectedScannerListener(callback);
        }
    }

    @Test
    public void testPriorityScanAfterStartOnlyRunsForSetScanTime() throws InterruptedException{
        // generic scan filter
        ScanFilter filter = new ScanFilter.Builder().build();
        alwaysConnectedScanner.setNumberOfExpectedDevices(1);
        alwaysConnectedScanner.setShouldKeepLooking(false);
        alwaysConnectedScanner.addScanFilter(mockContext, filter);
        CountDownLatch cdl = new CountDownLatch(3);
        long startTime = new Date().getTime();
        NoOpFitbitGattCallback callback = new NoOpFitbitGattCallback() {
            @Override
            public void onScanStarted() {
                cdl.countDown();
            }

            @Override
            public void onScanStopped() {
                if(cdl.getCount() == 1) {
                    // should be called within PeripheralScanner.SCAN_DURATION
                    // and high priority scan should not be continuing
                    long currentTime = new Date().getTime();
                    long diff = currentTime - startTime;
                    assertTrue(diff <= PeripheralScanner.SCAN_DURATION);
                    assertFalse(peripheralScanner.isScanning());
                    cdl.countDown();
                } else {
                    cdl.countDown();
                }
            }
        };
        gatt.registerGattEventListener(callback);
        alwaysConnectedScanner.start(mockContext);
        boolean didStart = alwaysConnectedScanner.startHighPriorityScan(mockContext);
        if(!didStart) {
            gatt.unregisterGattEventListener(callback);
            fail("Always connected scanner didn't start high priority scan");
            return;
        }
        boolean result = cdl.await(5, TimeUnit.SECONDS);
        if(!result) {
            gatt.unregisterGattEventListener(callback);
            fail("Test timed out");
        } else {
            gatt.unregisterGattEventListener(callback);
        }
    }

    @Test
    public void expectedZeroDevicesShouldNotStopScanIfContinueEnabled() throws InterruptedException {
        // generic scan filter
        ScanFilter filter = new ScanFilter.Builder().build();
        alwaysConnectedScanner.setNumberOfExpectedDevices(0);
        alwaysConnectedScanner.setShouldKeepLooking(false);
        alwaysConnectedScanner.addScanFilter(mockContext, filter);
        assertTrue(alwaysConnectedScanner.start(mockContext));
        // there is a single set of filters for all scanners
        CountDownLatch cdl = new CountDownLatch(1);
        AlwaysConnectedScannerListener callback = new AlwaysConnectedScannerListener() {
            @Override
            public void onPeripheralConnected(GattConnection connection) {
                cdl.countDown();
                assertTrue(peripheralScanner.isPeriodicalScanEnabled());
            }

            @Override
            public void onPeripheralConnectionError(TransactionResult result) {

            }
        };
        alwaysConnectedScanner.registerAlwaysConnectedScannerListener(callback);
        mockHandler.postDelayed(() -> {
            GattConnection connection = new GattConnection(mock(FitbitBluetoothDevice.class), mockContext.getMainLooper());
            connection.setMockMode(true);
            connection.setState(GattState.CONNECTED);
            alwaysConnectedScanner.onBluetoothPeripheralDiscovered(connection);
        }, 1000);
        boolean result = cdl.await(5, TimeUnit.SECONDS);
        if(!result) {
            alwaysConnectedScanner.unregisterAlwaysConnectedScannerListener(callback);
            fail("Test timed out");
        } else {
            alwaysConnectedScanner.unregisterAlwaysConnectedScannerListener(callback);
        }
    }

    @Test
    public void expectedOneDevicesShouldStopScanIfContinueDisabled() throws InterruptedException {
        // generic scan filter
        ScanFilter filter = new ScanFilter.Builder().build();
        alwaysConnectedScanner.setNumberOfExpectedDevices(1);
        alwaysConnectedScanner.setShouldKeepLooking(false);
        alwaysConnectedScanner.addScanFilter(mockContext, filter);
        assertTrue(alwaysConnectedScanner.start(mockContext));
        // there is a single set of filters for all scanners
        CountDownLatch cdl = new CountDownLatch(1);
        AlwaysConnectedScannerListener callback = new AlwaysConnectedScannerListener() {
            @Override
            public void onPeripheralConnected(GattConnection connection) {
                assertFalse(peripheralScanner.isPeriodicalScanEnabled());
                cdl.countDown();
            }

            @Override
            public void onPeripheralConnectionError(TransactionResult result) {

            }
        };
        alwaysConnectedScanner.registerAlwaysConnectedScannerListener(callback);
        mockHandler.postDelayed(() -> {
            GattConnection connection = new GattConnection(mock(FitbitBluetoothDevice.class), mockContext.getMainLooper());
            connection.setMockMode(true);
            connection.setState(GattState.CONNECTED);
            alwaysConnectedScanner.onBluetoothPeripheralDiscovered(connection);
        }, 1000);
        boolean result = cdl.await(5, TimeUnit.SECONDS);
        if(!result) {
            alwaysConnectedScanner.unregisterAlwaysConnectedScannerListener(callback);
            fail("Test timed out");
        } else {
            alwaysConnectedScanner.unregisterAlwaysConnectedScannerListener(callback);
        }
    }

    @Test
    public void expectedOneDevicesShouldNotStopScanIfContinueEnabled() throws InterruptedException {
        // generic scan filter
        ScanFilter filter = new ScanFilter.Builder().build();
        alwaysConnectedScanner.setNumberOfExpectedDevices(1);
        alwaysConnectedScanner.setShouldKeepLooking(false);
        alwaysConnectedScanner.addScanFilter(mockContext, filter);
        assertTrue(alwaysConnectedScanner.start(mockContext));
        // there is a single set of filters for all scanners
        CountDownLatch cdl = new CountDownLatch(1);
        AlwaysConnectedScannerListener callback = new AlwaysConnectedScannerListener() {
            @Override
            public void onPeripheralConnected(GattConnection connection) {
                cdl.countDown();
                assertTrue(peripheralScanner.isPeriodicalScanEnabled());
            }

            @Override
            public void onPeripheralConnectionError(TransactionResult result) {

            }
        };
        alwaysConnectedScanner.registerAlwaysConnectedScannerListener(callback);
        mockHandler.postDelayed(() -> {
            GattConnection connection = new GattConnection(mock(FitbitBluetoothDevice.class), mockContext.getMainLooper());
            connection.setMockMode(true);
            connection.setState(GattState.CONNECTED);
            alwaysConnectedScanner.onBluetoothPeripheralDiscovered(connection);
        }, 1000);
        boolean result = cdl.await(5, TimeUnit.SECONDS);
        if(!result) {
            alwaysConnectedScanner.unregisterAlwaysConnectedScannerListener(callback);
            fail("Test timed out");
        } else {
            alwaysConnectedScanner.unregisterAlwaysConnectedScannerListener(callback);
        }
    }

    /**
     * Since its likely that we will continue to use the callback to manage various test verifications
     * as an efficiency we will create a no op class so that we don't have to constantly re-implement
     * these methods.
     */

    private class NoOpFitbitGattCallback implements FitbitGatt.FitbitGattCallback {

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
