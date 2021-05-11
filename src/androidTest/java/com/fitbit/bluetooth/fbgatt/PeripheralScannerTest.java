/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.util.NoOpGattCallback;

import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelUuid;
import android.os.SystemClock;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import timber.log.Timber;

import static com.fitbit.bluetooth.fbgatt.PeripheralScanner.TEST_SCAN_DURATION;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.*;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
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

    @BeforeClass
    public static void beforeClass() {
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
        names.add("Versa 2");
        appContext = InstrumentationRegistry.getInstrumentation().getContext();
        IntentFilter filter = new IntentFilter(PeripheralScanner.SCANNED_DEVICE_ACTION);
        appContext.registerReceiver(handleIntentBasedScanResult, filter);
    }

    @Before
    public void before() {
        // wait for 3s before each test so that we don't end up running afoul of the scanning
        // limit
        SystemClock.sleep(3000);
        FitbitGatt.getInstance().initializeScanner(appContext);
        FitbitGatt.getInstance().getPeripheralScanner().setInstrumentationTestMode(true);
    }

    @AfterClass
    public static void afterClass() {
        delayHandlerThread.quit();
        otherDelayHandlerThread.quit();
        appContext.unregisterReceiver(handleIntentBasedScanResult);
    }

    @After
    public void after() {
        Timber.tag("").d("==================================================================================");
        FitbitGatt.getInstance().shutdown();
        FitbitGatt.setInstance(null);
    }

    private void startHighPriorityScannerForTestWithFitbitFilter() {
        FitbitGatt.getInstance().initializeScanner(appContext);
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
        assertTrue(FitbitGatt.getInstance().isInitialized());
        FitbitGatt.getInstance().getPeripheralScanner().resetFilters();
        FitbitGatt.getInstance().getPeripheralScanner().addServiceUUIDWithMask(new ParcelUuid(FITBIT_SERVICE_UUID), new ParcelUuid(FITBIT_SERVICE_MASK));
        FitbitGatt.getInstance().getPeripheralScanner().startHighPriorityScan(appContext);
    }

    private void verifyCountDown(@NonNull CountDownLatch cdl, long seconds) {
        long initialCountDown = cdl.getCount();
        try {
            cdl.await(seconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Test interrupted");
        }
        if (cdl.getCount() != 0) {
            fail("Not all assertions have been executed. Executed countDown " + (initialCountDown - cdl.getCount()) + " of " + initialCountDown);
        }
    }

    private void verifyCountDown(@NonNull CountDownLatch cdl) {
        verifyCountDown(cdl, 1);
    }

    @Test
    @Ignore
    public void testPeriodicScan() {
        // started
        FitbitGatt.getInstance().initializeScanner(appContext);
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
        FitbitGatt.getInstance().getPeripheralScanner().resetFilters();
        FitbitGatt.getInstance().getPeripheralScanner().addServiceUUIDWithMask(new ParcelUuid(FITBIT_SERVICE_UUID), new ParcelUuid(FITBIT_SERVICE_MASK));
        FitbitGatt.getInstance().getPeripheralScanner().startPeriodicScan(appContext);
        CountDownLatch cdl = new CountDownLatch(1);
        assertTrue(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
        final boolean[] results = new boolean[3];
        handler.postDelayed(() -> {
            results[0] = FitbitGatt.getInstance().getPeripheralScanner().isScanning();
            handler.postDelayed(() -> {
                results[1] = FitbitGatt.getInstance().getPeripheralScanner().isScanning();
                handler.postDelayed(() -> {
                    results[2] = FitbitGatt.getInstance().getPeripheralScanner().isScanning();
                    cdl.countDown();
                }, 10000);
            }, 11000);
        }, 10000);
        verifyCountDown(cdl, 40);
        assertTrue(results[0]);
        assertTrue(results[1]);
        assertTrue(results[2]);
    }


    @Test
    public void testHighPriorityScanTimesOut() {
        // started
        startHighPriorityScannerForTestWithFitbitFilter();
        assertTrue(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
        CountDownLatch cdl = new CountDownLatch(1);
        handler.postDelayed(cdl::countDown, TEST_SCAN_DURATION + 250);
        verifyCountDown(cdl, TimeUnit.MILLISECONDS.toSeconds(TEST_SCAN_DURATION) + 1);
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
    }

    @Test
    public void startPeriodicalScannerThenHighPriorityScannerThenStopHighPriority() {
        final boolean[] hasHPScanStarted = new boolean[1];
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
        CountDownLatch cdl = new CountDownLatch(1);
        FitbitGatt.getInstance().initializeScanner(appContext);
        FitbitGatt.FitbitGattCallback callback = new NoOpGattCallback() {
            @Override
            public void onScanStarted() {
                if (!hasHPScanStarted[0]) {
                    hasHPScanStarted[0] = true;
                    FitbitGatt.getInstance().getPeripheralScanner().startHighPriorityScan(appContext);
                } else {
                    FitbitGatt.getInstance().getPeripheralScanner().cancelHighPriorityScan(appContext);
                    hasHPScanStarted[0] = false;
                }
                cdl.countDown();
            }

            @Override
            public void onScanStopped() {
                if (!hasHPScanStarted[0]) {
                    assertTrue(FitbitGatt.getInstance().getPeripheralScanner().isPeriodicalScanEnabled());
                    FitbitGatt.getInstance().unregisterAllGattEventListeners();
                }
            }

        };
        FitbitGatt.getInstance().registerGattEventListener(callback);
        FitbitGatt.getInstance().setDeviceNameScanFilters(names);
        FitbitGatt.getInstance().getPeripheralScanner().startPeriodicScan(appContext);
        verifyCountDown(cdl, 10);
    }

    @Test
    public void testStopScan() {
        // started
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
        assertTrue(FitbitGatt.getInstance().isInitialized());
        FitbitGatt.getInstance().getPeripheralScanner().addServiceUUIDWithMask(new ParcelUuid(FITBIT_SERVICE_UUID), new ParcelUuid(FITBIT_SERVICE_MASK));
        FitbitGatt.getInstance().initializeScanner(appContext);
        FitbitGatt.getInstance().getPeripheralScanner().startHighPriorityScan(appContext);
        CountDownLatch cdl = new CountDownLatch(1);
        handler.postDelayed(() -> {
            FitbitGatt.getInstance().getPeripheralScanner().cancelScan(appContext);
            assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
            cdl.countDown();
        }, 1000);
        verifyCountDown(cdl, 2);
    }

    @Test
    //this is flaky in instrumentation without a previous bonded device
    @Ignore
    public void testSetFilterName() {
        // started
        final List<GattConnection>[] connections = new List[]{new ArrayList()};
        assertTrue(FitbitGatt.getInstance().isInitialized());
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
        FitbitGatt.getInstance().getPeripheralScanner().resetFilters();
        FitbitGatt.getInstance().getPeripheralScanner().setDeviceNameFilters(names);
        FitbitGatt.getInstance().initializeScanner(appContext);
        FitbitGatt.getInstance().getPeripheralScanner().startHighPriorityScan(appContext);
        assertTrue(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
        CountDownLatch cdl = new CountDownLatch(1);
        handler.postDelayed(() -> {
            FitbitGatt.getInstance().getPeripheralScanner().cancelScan(appContext);
            connections[0] = FitbitGatt.getInstance().getMatchingConnectionsForDeviceNames(names);
            cdl.countDown();
        }, 10000);
        verifyCountDown(cdl, 11);
        assertFalse(FitbitGatt.getInstance().isScanning());
        for (GattConnection connection : connections[0]) {
            Timber.d(connection.getDevice().getName());
        }
        assertTrue(connections[0].size() > 0);
    }

    @Test
    public void testStartPeriodicScanNotStartPendingIntent() {
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isPendingIntentScanning());
        FitbitGatt.getInstance().getPeripheralScanner().setDeviceNameFilters(names);
        FitbitGatt.getInstance().getPeripheralScanner().startPeriodicScan(appContext);
        CountDownLatch cdl = new CountDownLatch(1);
        handler.postDelayed(() -> {
            assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isPendingIntentScanning());
            cdl.countDown();
        }, 300);
        verifyCountDown(cdl);
    }

    @Test
    public void testStartPendingIntentScanNotPeriodicalScan() {
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isPendingIntentScanning());
        FitbitGatt.getInstance().getPeripheralScanner().setDeviceNameFilters(names);
        ArrayList<ScanFilter> filters = new ArrayList<>();
        for (String name : names) {
            filters.add(new ScanFilter.Builder().setDeviceName(name).build());
        }
        FitbitGatt.getInstance().getPeripheralScanner().startPendingIntentBasedBackgroundScan(filters, appContext);
        CountDownLatch cdl = new CountDownLatch(1);
        handler.postDelayed(() -> {
            assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
            cdl.countDown();
        }, 300);
        verifyCountDown(cdl);
    }

    @Test
    public void testStartHighPriorityScanNotStartPendingIntent() {
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isPendingIntentScanning());
        FitbitGatt.getInstance().getPeripheralScanner().setDeviceNameFilters(names);
        FitbitGatt.getInstance().initializeScanner(appContext);
        FitbitGatt.getInstance().getPeripheralScanner().startHighPriorityScan(appContext);
        CountDownLatch cdl = new CountDownLatch(1);
        handler.postDelayed(() -> {
            assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isPendingIntentScanning());
            cdl.countDown();
        }, 300);
        verifyCountDown(cdl);
    }

    @Test
    public void testStartPendingIntentScanNotHighPriorityScan() {
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isPendingIntentScanning());
        FitbitGatt.getInstance().getPeripheralScanner().setDeviceNameFilters(names);
        ArrayList<ScanFilter> filters = new ArrayList<>();
        for (String name : names) {
            filters.add(new ScanFilter.Builder().setDeviceName(name).build());
        }
        FitbitGatt.getInstance().getPeripheralScanner().startPendingIntentBasedBackgroundScan(filters, appContext);
        CountDownLatch cdl = new CountDownLatch(1);
        handler.postDelayed(() -> {
            assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
            cdl.countDown();
        }, 300);
        verifyCountDown(cdl);
    }

    @Test
    public void testStopConcurrencyProtection() {
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
        verifyCountDown(cdl);
    }

    @Test
    public void ensureGetFiltersIsShallow() {
        FitbitGatt.getInstance().resetScanFilters();
        FitbitGatt.getInstance().addDeviceAddressFilter("00:00:00:00:00:00");
        ArrayList<ScanFilter> filtersCopy = (ArrayList<ScanFilter>) FitbitGatt.getInstance().getScanFilters();
        filtersCopy.remove(0);
        assertTrue(filtersCopy.isEmpty());
        assertFalse(FitbitGatt.getInstance().getScanFilters().isEmpty());
    }

    @Test
    public void testEnsureCantStartMoreThanFiveScannersInThirtySeconds() {
        int i = 1;
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
        FitbitGatt.getInstance().getPeripheralScanner().setDeviceNameFilters(names);
        CountDownLatch cdl = new CountDownLatch(6);
        while (i <= 5) {
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
        verifyCountDown(cdl);
    }

    @Test
    public void highPriorityScanTimeoutShouldNotStartBackgroundScan() {
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
        FitbitGatt.getInstance().getPeripheralScanner().setDeviceNameFilters(names);
        CountDownLatch cdl = new CountDownLatch(1);
        FitbitGatt.FitbitGattCallback callback = new NoOpGattCallback() {
            @Override
            public void onScanStopped() {
                cdl.countDown();
            }

        };
        FitbitGatt.getInstance().registerGattEventListener(callback);
        FitbitGatt.getInstance().startHighPriorityScan(appContext);
        verifyCountDown(cdl, TimeUnit.MINUTES.toMillis(5));
        assertTrue(FitbitGatt.getInstance().getPeripheralScanner().mHandler.getLooper().getQueue().isIdle());
    }

    @Test
    @Ignore("We can't actually test pending intent this way in the library because the intent needs to be in the target manifest")
    public void testBackgroundScanner() {
        // only run this when fitbits are around
        final boolean[] didFindTracker = new boolean[1];
        CountDownLatch cdl = new CountDownLatch(1);
        if (FitbitGatt.getInstance().isBluetoothOn()) {
            FitbitGatt.getInstance().cancelScan(appContext);
            FitbitGatt.getInstance().stopSystemManagedPendingIntentScan();
            FitbitGatt.FitbitGattCallback callback = new NoOpGattCallback() {
                @Override
                public void onBluetoothPeripheralDiscovered(@NonNull GattConnection connection) {
                    Timber.i("Found peripheral!");
                    FitbitGatt.getInstance().stopSystemManagedPendingIntentScan();
                    Timber.i("Successfully cleaned up");
                    FitbitGatt.getInstance().unregisterGattEventListener(this);
                    didFindTracker[0] = true;
                    cdl.countDown();
                }
            };

            FitbitGatt.getInstance().registerGattEventListener(callback);
            ArrayList<ScanFilter> filters = new ArrayList<>();
            for (String name : names) {
                filters.add(new ScanFilter.Builder().setDeviceName(name).build());
            }
            FitbitGatt.getInstance().startSystemManagedPendingIntentScan(appContext, filters);
            verifyCountDown(cdl, TimeUnit.MINUTES.toMillis(5));
            FitbitGatt.getInstance().stopSystemManagedPendingIntentScan();
            FitbitGatt.getInstance().unregisterGattEventListener(callback);
            assertTrue(didFindTracker[0]);
        } else {
            assertFalse(FitbitGatt.getInstance().isBluetoothOn());
        }
    }
}
