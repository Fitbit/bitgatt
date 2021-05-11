/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.util.BluetoothUtils;
import com.fitbit.bluetooth.fbgatt.util.LooperWatchdog;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.stubbing.Answer;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Will test the behavior of the always connected scanner with the mock lollipop scanner
 */
@RunWith(JUnit4.class)
public class AlwaysConnectedScannerTest {

    private static MockLollipopScanner mockScanner;
    private FitbitGatt gatt;
    private Context mockContext = mock(Context.class);
    private PeripheralScanner peripheralScanner;
    private AlwaysConnectedScanner alwaysConnectedScanner;
    private Handler mockHandler;
    private ScheduledExecutorService singleThreadExecutor = Executors.newSingleThreadScheduledExecutor();
    @SuppressWarnings("FutureReturnValueIgnored")
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
    private BluetoothUtils utilsMock = mock(BluetoothUtils.class);
    private LowEnergyAclListener lowEnergyAclListenerMock = mock(LowEnergyAclListener.class);
    private BluetoothRadioStatusListener bluetoothRadioStatusListenerMock = mock(BluetoothRadioStatusListener.class);
    private BitGattDependencyProvider dependencyProviderMock = mock(BitGattDependencyProvider.class);

    @BeforeClass
    public static void beforeClass() {
        mockScanner = MockLollipopScanner.BluetoothAdapter.getBluetoothLeScanner();
    }

    @Before
    public void before(){
        FitbitGatt.setInstance(null); // It seems another test is influencing this one. Haven't yet pinpointed witch one. This fixes the issue.
        doReturn(mockContext).when(mockContext).getApplicationContext();
        doReturn(bluetoothRadioStatusListenerMock).when(dependencyProviderMock).getNewBluetoothRadioStatusListener(mockContext, false);
        doReturn(utilsMock).when(dependencyProviderMock).getBluetoothUtils();
        doReturn(lowEnergyAclListenerMock).when(dependencyProviderMock).getNewLowEnergyAclListener();
        doCallRealMethod().when(dependencyProviderMock).getNewPeripheralScanner(any(), any());
        doReturn(true).when(utilsMock).isBluetoothEnabled(mockContext);

        Looper mockMainThreadLooper = mock(Looper.class);
        Thread mockMainThread = mock(Thread.class);
        when(mockMainThread.getName()).thenReturn("Irvin's mock thread");
        when(mockMainThreadLooper.getThread()).thenReturn(mockMainThread);
        when(mockContext.registerReceiver(any(), any())).thenReturn(new Intent("some custom action"));
        when(mockContext.getMainLooper()).thenReturn(mockMainThreadLooper);
        mockHandler = mock(Handler.class);
        doAnswer(handlerPostAnswer).when(mockHandler).post(any(Runnable.class));
        doAnswer(handlerPostAnswer).when(mockHandler).postDelayed(any(Runnable.class), anyLong());
        when(mockHandler.getLooper()).thenReturn(mockMainThreadLooper);

        gatt = FitbitGatt.getInstance();
        gatt.setDependencyProvider(dependencyProviderMock);
        gatt.setAsyncOperationThreadWatchdog(mock(LooperWatchdog.class));
        gatt.initializeScanner(mockContext);
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
        FitbitGatt.getInstance().shutdown();
        FitbitGatt.setInstance(null);
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
        NoOpGattCallback callback = new NoOpGattCallback() {
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
}
