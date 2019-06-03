/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(AndroidJUnit4.class)
public class PeripheralScannerTest {

    private static final String FITBIT_SERVICE_UUID_FORMAT = "adab%s-6e7d-4601-bda2-bffaa68956ba";
    private static final UUID FITBIT_SERVICE_UUID = UUID.fromString(String.format(FITBIT_SERVICE_UUID_FORMAT, "fb00"));
    private static final UUID FITBIT_SERVICE_MASK = UUID.fromString("FFFF0000-FFFF-FFFF-FFFF-FFFFFFFFFFFF");
    private static ArrayList<String> names;
    private static Context appContext;
    private static HandlerThread delayHandlerThread;
    private static Handler handler;
    private static HandlerThread otherDelayHandlerThread;
    private static Handler otherDelayHandler;
    private static HandleIntentBasedScanResult handleIntentBasedScanResult;
    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    @Before
    public void before(){
        // wait for 3s before each test so that we don't end up running afoul of the scanning
        // limit
        SystemClock.sleep(3000);
        FitbitGatt.getInstance().start(appContext);
        FitbitGatt.getInstance().getPeripheralScanner().setMockMode(false);
        FitbitGatt.getInstance().getPeripheralScanner().cancelScan(this.appContext);
        FitbitGatt.getInstance().getPeripheralScanner().cancelPendingIntentBasedBackgroundScan();
    }

    @BeforeClass
    public static void beforeClass(){
        delayHandlerThread = new HandlerThread("Scanner test handler thread", Thread.MAX_PRIORITY);
        delayHandlerThread.start();
        handler = new Handler(delayHandlerThread.getLooper());
        otherDelayHandlerThread = new HandlerThread("Scanner test other handler thread", Thread.MAX_PRIORITY);
        otherDelayHandlerThread.start();
        otherDelayHandler = new Handler(otherDelayHandlerThread.getLooper());
        handleIntentBasedScanResult = new HandleIntentBasedScanResult();
        names = new ArrayList<>();
        names.add("Ionic");
        names.add("Surge");
        names.add("Blaze");
        names.add("Versa");
        names.add("Antares");
        names.add("Charge 3");
        names.add("Charge 2");
        names.add("Charge HR");
        names.add("Alta");
        names.add("Alta HR");
        names.add("Flex");
        names.add("Flex 2");
        names.add("Versa Lite");
        names.add("Inspire");
        names.add("Inspire HR");
        names.add("Mira");
        appContext = InstrumentationRegistry.getContext();
        IntentFilter filter = new IntentFilter(PeripheralScanner.SCANNED_DEVICE_ACTION);
        appContext.registerReceiver(handleIntentBasedScanResult, filter);
    }

    @AfterClass
    public static void afterClass(){
        delayHandlerThread.quit();
        otherDelayHandlerThread.quit();
        appContext.unregisterReceiver(handleIntentBasedScanResult);
    }

    @Test
    public void testPeriodicScan() throws InterruptedException {
        // started
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
        assertEquals(true, FitbitGatt.getInstance().isStarted());
        FitbitGatt.getInstance().getPeripheralScanner().resetFilters();
        FitbitGatt.getInstance().getPeripheralScanner().addServiceUUIDWithMask(new ParcelUuid(FITBIT_SERVICE_UUID), new ParcelUuid(FITBIT_SERVICE_MASK));
        FitbitGatt.getInstance().getPeripheralScanner().startHighPriorityScan(appContext);
        CountDownLatch cdl = new CountDownLatch(1);
        handler.postDelayed(()->{
            assertEquals(true, FitbitGatt.getInstance().getPeripheralScanner().isScanning());
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                new Handler(Looper.getMainLooper()).postDelayed(()-> assertTrue(FitbitGatt.getInstance().getPeripheralScanner().isScanning()), PeripheralScanner.SCAN_INTERVAL);
                cdl.countDown();
            }, PeripheralScanner.SCAN_DURATION);
        }, 1000);
        cdl.await(2, TimeUnit.SECONDS);
    }

    @Test
    public void startPeriodicalScannerThenHighPriorityScannerThenStopHighPriority() throws InterruptedException {
        // started
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
        final boolean[] hasHPScanStarted = new boolean[1];
        FitbitGatt.getInstance().getPeripheralScanner().resetFilters();
        FitbitGatt.getInstance().getPeripheralScanner().addServiceUUIDWithMask(new ParcelUuid(FITBIT_SERVICE_UUID), new ParcelUuid(FITBIT_SERVICE_MASK));
        CountDownLatch cdl = new CountDownLatch(1);
        FitbitGatt.FitbitGattCallback callback = new FitbitGatt.FitbitGattCallback() {
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
            public void onScanStarted() {
                if(!hasHPScanStarted[0]) {
                    hasHPScanStarted[0] = true;
                    FitbitGatt.getInstance().getPeripheralScanner().startHighPriorityScan(appContext);
                } else {
                    FitbitGatt.getInstance().getPeripheralScanner().cancelHighPriorityScan(appContext);
                    hasHPScanStarted[0] = false;
                }
            }

            @Override
            public void onScanStopped() {
                if(!hasHPScanStarted[0]) {
                    cdl.countDown();
                    assertTrue(FitbitGatt.getInstance().getPeripheralScanner().isPeriodicalScanEnabled());
                    FitbitGatt.getInstance().unregisterAllGattEventListeners();
                }
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
        };
        FitbitGatt.getInstance().registerGattEventListener(callback);
        FitbitGatt.getInstance().getPeripheralScanner().startPeriodicScan(appContext);
        cdl.await(2, TimeUnit.SECONDS);
    }

    @Test
    public void testStopScan() throws InterruptedException {
        // started
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
        assertEquals(true, FitbitGatt.getInstance().isStarted());
        FitbitGatt.getInstance().getPeripheralScanner().startHighPriorityScan(appContext);
        CountDownLatch cdl = new CountDownLatch(1);
        handler.postDelayed(()->{
            FitbitGatt.getInstance().getPeripheralScanner().cancelScan(appContext);
            assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
            cdl.countDown();
        }, 1000);
        cdl.await(2, TimeUnit.SECONDS);
    }

    @Test
    public void testSetFilterName() throws InterruptedException {
        // started
        FitbitGatt.getInstance().start(appContext);
        assertEquals(true, FitbitGatt.getInstance().isStarted());
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
        FitbitGatt.getInstance().getPeripheralScanner().resetFilters();
        FitbitGatt.getInstance().getPeripheralScanner().setDeviceNameFilters(names);
        FitbitGatt.getInstance().getPeripheralScanner().startHighPriorityScan(appContext);
        CountDownLatch cdl = new CountDownLatch(1);
        handler.postDelayed(() -> {
            FitbitGatt.getInstance().getPeripheralScanner().cancelScan(appContext);
            List<GattConnection> connections = FitbitGatt.getInstance().getMatchingConnectionsForDeviceNames(names);
            Timber.d("Found %d devices", connections.size());
            assertTrue(connections.size() > 0);
            cdl.countDown();
        }, 1000);
        cdl.await(2, TimeUnit.SECONDS);
    }

    @Test
    public void testStartConcurrencyProtection() throws InterruptedException {
        assertFalse(FitbitGatt.getInstance().isScanning());
        FitbitGatt.getInstance().getPeripheralScanner().setDeviceNameFilters(names);
        FitbitGatt.getInstance().getPeripheralScanner().startHighPriorityScan(appContext);
        CountDownLatch cdl = new CountDownLatch(2);
        handler.postDelayed(() -> {
            assertTrue(FitbitGatt.getInstance().isScanning());
            cdl.countDown();
        }, 8);
        otherDelayHandler.postDelayed(() -> {
            assertTrue(FitbitGatt.getInstance().isScanning());
            cdl.countDown();
        }, 8);
        cdl.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void testStartPeriodicScanNotStartPendingIntent() throws InterruptedException {
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isPendingIntentScanning());
        FitbitGatt.getInstance().getPeripheralScanner().setDeviceNameFilters(names);
        FitbitGatt.getInstance().getPeripheralScanner().startPeriodicScan(appContext);
        CountDownLatch cdl = new CountDownLatch(1);
        handler.postDelayed(() -> {
            assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isPendingIntentScanning());
        }, 300);
        cdl.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void testStartPendingIntentScanNotPeriodicalScan() throws InterruptedException {
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isPendingIntentScanning());
        FitbitGatt.getInstance().getPeripheralScanner().setDeviceNameFilters(names);
        ArrayList<ScanFilter> filters = new ArrayList<>();
        for(String name : names) {
            filters.add(new ScanFilter.Builder().setDeviceName(name).build());
        }
        FitbitGatt.getInstance().getPeripheralScanner().startPendingIntentBasedBackgroundScan(filters, appContext);
        CountDownLatch cdl = new CountDownLatch(1);
        handler.postDelayed(() -> {
            assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
        }, 300);
        cdl.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void testStartHighPriorityScanNotStartPendingIntent() throws InterruptedException {
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isPendingIntentScanning());
        FitbitGatt.getInstance().getPeripheralScanner().setDeviceNameFilters(names);
        FitbitGatt.getInstance().getPeripheralScanner().startHighPriorityScan(appContext);
        CountDownLatch cdl = new CountDownLatch(1);
        handler.postDelayed(() -> {
            assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isPendingIntentScanning());
        }, 300);
        cdl.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void testStartPendingIntentScanNotHighPriorityScan() throws InterruptedException {
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isPendingIntentScanning());
        FitbitGatt.getInstance().getPeripheralScanner().setDeviceNameFilters(names);
        ArrayList<ScanFilter> filters = new ArrayList<>();
        for(String name : names) {
            filters.add(new ScanFilter.Builder().setDeviceName(name).build());
        }
        FitbitGatt.getInstance().getPeripheralScanner().startPendingIntentBasedBackgroundScan(filters, appContext);
        CountDownLatch cdl = new CountDownLatch(1);
        handler.postDelayed(() -> {
            assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
        }, 300);
        cdl.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void testStopConcurrencyProtection() throws InterruptedException {
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
        FitbitGatt.getInstance().getPeripheralScanner().setDeviceNameFilters(names);
        FitbitGatt.getInstance().getPeripheralScanner().startHighPriorityScan(appContext);
        CountDownLatch cdl = new CountDownLatch(2);
        FitbitGatt.getInstance().getPeripheralScanner().cancelScan(appContext);
        // we want to make sure that it has actually started, so we'll wait a bit
        // so that this isn't a flaky instrumented test
        handler.postDelayed(() -> {
            assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
            cdl.countDown();
        }, 8);
        otherDelayHandler.postDelayed(() -> {
            assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
            cdl.countDown();
        }, 8);
        cdl.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void ensureGetFiltersIsShallow() {
        FitbitGatt.getInstance().resetScanFilters();
        FitbitGatt.getInstance().addDeviceAddressFilter("00:00:00:00:00:00");
        ArrayList<ScanFilter> filtersCopy = (ArrayList<ScanFilter>)FitbitGatt.getInstance().getScanFilters();
        filtersCopy.remove(0);
        Assert.assertTrue(filtersCopy.isEmpty());
        Assert.assertTrue(!FitbitGatt.getInstance().getScanFilters().isEmpty());
    }

    @Test
    public void testEnsureCantStartMoreThanFiveScannersInThirtySeconds() throws InterruptedException {
        int i=1;
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
        FitbitGatt.getInstance().getPeripheralScanner().setDeviceNameFilters(names);
        CountDownLatch cdl = new CountDownLatch(6);
        while(i <= 5) {
            handler.postDelayed(() -> {
                        FitbitGatt.getInstance().getPeripheralScanner().startHighPriorityScan(appContext);
                        cdl.countDown();
                    }, 8);
            i++;
        }
        handler.postDelayed(() -> {
            assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
            cdl.countDown();
        }, 40);
        cdl.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void highPriorityScanTimeoutShouldNotStartBackgroundScan() throws InterruptedException {
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
        FitbitGatt.getInstance().getPeripheralScanner().setDeviceNameFilters(names);
        CountDownLatch cdl = new CountDownLatch(1);
        FitbitGatt.FitbitGattCallback callback = new FitbitGatt.FitbitGattCallback() {
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
            public void onScanStarted() {

            }

            @Override
            public void onScanStopped() {
                cdl.countDown();
                assertTrue(FitbitGatt.getInstance().getPeripheralScanner().mHandler.getLooper().getQueue().isIdle());
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
        };
        FitbitGatt.getInstance().registerGattEventListener(callback);
        FitbitGatt.getInstance().startHighPriorityScan(appContext);
        cdl.await(5, TimeUnit.MINUTES);
    }

    @Test
    @Ignore("We can't actually test pending intent this way in the library because the intent needs to be in the target manifest")
    public void testBackgroundScanner() {
        // only run this when fitbits are around
        final boolean[] didFindTracker = new boolean[1];
        CountDownLatch cdl = new CountDownLatch(1);
        if(FitbitGatt.getInstance().isBluetoothOn() ) {
            FitbitGatt.getInstance().cancelScan(appContext);
            FitbitGatt.getInstance().stopSystemManagedPendingIntentScan();
            FitbitGatt.FitbitGattCallback callback = new FitbitGatt.FitbitGattCallback() {
                @Override
                public void onBluetoothPeripheralDiscovered(@NonNull GattConnection connection) {
                    Timber.i("Found peripheral!");
                    FitbitGatt.getInstance().stopSystemManagedPendingIntentScan();
                    Timber.i("Successfully cleaned up");
                    FitbitGatt.getInstance().unregisterGattEventListener(this);
                    didFindTracker[0] = true;
                    cdl.countDown();
                }

                @Override
                public void onBluetoothPeripheralDisconnected(@NonNull GattConnection connection) {

                }

                @Override
                public void onFitbitGattReady() {

                }

                @Override
                public void onScanStarted() {

                }

                @Override
                public void onScanStopped() {

                }

                @Override
                public void onPendingIntentScanStopped() {
                    Timber.i("Pending intent scan stopped");
                }

                @Override
                public void onPendingIntentScanStarted() {
                    Timber.i("Pending intent scan started");
                }

                @Override
                public void onBluetoothOff() {

                }

                @Override
                public void onBluetoothOn() {

                }
            };

            FitbitGatt.getInstance().registerGattEventListener(callback);
            ArrayList<ScanFilter> filters = new ArrayList<>();
            for(String name : names) {
                filters.add(new ScanFilter.Builder().setDeviceName(name).build());
            }
            FitbitGatt.getInstance().startSystemManagedPendingIntentScan(appContext, filters);
            try {
                cdl.await(5, TimeUnit.MINUTES);
                FitbitGatt.getInstance().stopSystemManagedPendingIntentScan();
                FitbitGatt.getInstance().unregisterGattEventListener(callback);
                Assert.assertTrue(didFindTracker[0]);
            } catch (InterruptedException ex) {
                Timber.w(ex, "interrupted while waiting");
                FitbitGatt.getInstance().stopSystemManagedPendingIntentScan();
                FitbitGatt.getInstance().unregisterGattEventListener(callback);
                Assert.fail("Didn't find a fitbit by the allotted time");
            }
        } else {
            Assert.assertTrue("Bluetooth was off, so we are just going to be winning", !FitbitGatt.getInstance().isBluetoothOn());
        }
    }
}
