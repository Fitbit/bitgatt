/*
 *
 *  Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.util.NoOpGattCallback;

import android.content.Context;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import androidx.test.platform.app.InstrumentationRegistry;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * This test class is around {@link FitbitGatt} shutdown method.
 * To ensure isolation testing we shutdown the gatt singleton between runs.
 * And in oder to make sure we don't have hooks left in the system  we test the test method
 * <p>
 * Created by ilepadatescu on 09/30/19.
 */
public class ShutDownFitbitGattTest {
    static Context context;

    @BeforeClass
    public static void beforeClass() {
        context = InstrumentationRegistry.getInstrumentation().getContext();
    }

    @After
    public void after() {
        FitbitGatt.getInstance().shutdown();
        FitbitGatt.setInstance(null);
    }

    @Test
    public void testShutDown() throws InterruptedException {
        FitbitGatt fitbitGatt = FitbitGatt.getInstance();
        CountDownLatch cdl = new CountDownLatch(3);

        NoOpGattCallback cb = spy(new NoOpGattCallback() {

            @Override
            public void onGattClientStarted() {
                super.onGattClientStarted();
                cdl.countDown();
            }

            @Override
            public void onScanStarted() {
                super.onScanStarted();
                cdl.countDown();
            }

            @Override
            public void onGattServerStarted(GattServerConnection serverConnection) {
                super.onGattServerStarted(serverConnection);
                cdl.countDown();
            }
        });

        ArrayList<String> filters = new ArrayList<String>();
        filters.add("TEST");

        fitbitGatt.registerGattEventListener(cb);
        fitbitGatt.initializeScanner(context);
        //replace it with a spy
        fitbitGatt.startGattClient(context);

        //swapping instances to verify interactions
        PeripheralScanner peripheralScanner = fitbitGatt.getPeripheralScanner();
        PeripheralScanner scanner = spy(peripheralScanner);
        fitbitGatt.setPeripheralScanner(scanner);
        fitbitGatt.setDeviceNameScanFilters(filters);
        fitbitGatt.startPeriodicScan(context);
        fitbitGatt.startGattServer(context);

        cdl.await(10, TimeUnit.SECONDS);

        if (cdl.getCount() != 0) {
            fail("Failed to start fully remaining " + cdl.getCount());
        }

        //start verifications
        assertNotNull(fitbitGatt.getServer());
        assertNotNull(fitbitGatt.getClientCallback());
        assertNotNull(fitbitGatt.getServerCallback());
        assertTrue(fitbitGatt.isScanning());


        fitbitGatt.shutdown();

        //verify start interactions
        InOrder callbackOrder = inOrder(cb);
        InOrder scannerOder = inOrder(scanner);
        callbackOrder.verify(cb).onGattClientStarted();
        callbackOrder.verify(cb).onScanStarted();
        callbackOrder.verify(cb).onGattServerStarted(any());

        scannerOder.verify(scanner, times(1)).setDeviceNameFilters(filters);
        //one assertion and one start
        scannerOder.verify(scanner, times(1)).startPeriodicScan(context);


        //when we cancel
        scannerOder.verify(scanner, times(1)).isScanning();
        assertNull(fitbitGatt.getServer());
        assertNull(fitbitGatt.getClientCallback());
        assertNull(fitbitGatt.getServerCallback());
        assertFalse(fitbitGatt.isInitialized());
        assertFalse(fitbitGatt.isScanning());

        //verify shutdown interactions
        scannerOder.verify(scanner, times(1)).onDestroy(context.getApplicationContext());
        scannerOder.verify(scanner, times(1)).cancelPeriodicalScan(context.getApplicationContext());
        scannerOder.verify(scanner, times(1)).isScanning();
        scannerOder.verify(scanner, times(1)).cancelScan(context.getApplicationContext());
        scannerOder.verify(scanner, times(1)).stopScan(context.getApplicationContext());

        verifyNoMoreInteractions(cb);
        verifyNoMoreInteractions(scanner);
    }
}
