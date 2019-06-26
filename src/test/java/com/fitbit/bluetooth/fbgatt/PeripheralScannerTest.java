/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Looper;
import android.support.annotation.NonNull;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test the logic in the peripheral scanner
 *
 * Created by iowens on 9/4/18.
 */
public class PeripheralScannerTest {

    Looper mockLooper;
    public static Context mockContext;

    @BeforeClass
    public static void beforeClass(){
        mockContext = mock(Context.class);
        when(mockContext.getApplicationContext()).thenReturn(mockContext);
        when(mockContext.registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class))).thenReturn(new Intent());

        FitbitGatt.getInstance().start(mockContext);
        if(FitbitGatt.getInstance().getPeripheralScanner() != null) {
            FitbitGatt.getInstance().getPeripheralScanner().setMockMode(true);
        }
    }

    @Before
    public void before(){
        mockLooper = mock(Looper.class);
        FitbitGatt.getInstance().clearConnectionsMap();
    }

    @After
    public void after() {FitbitGatt.getInstance().unregisterAllGattEventListeners();}

    @Test
    public void testRssiFilteredScanNoResults() {
        if(FitbitGatt.getInstance().getPeripheralScanner() == null) {
            fail();
        }
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
        MockScanResultProvider provider = new MockScanResultProvider(10, -167, -40);
        FitbitGatt.getInstance().getPeripheralScanner().addRssiFilter(-10);
        List<ScanResult> results = provider.getAllResults();
        FitbitGatt.getInstance().getPeripheralScanner().populateMockScanResultBatchValues(results);
        Assert.assertEquals(0, FitbitGatt.getInstance().getNewlyScannedDevicesOnly().size());
    }

    @Test
    public void testRssiFilteredScanResults() {
        if(FitbitGatt.getInstance().getPeripheralScanner() == null) {
            fail();
        }
        assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
        MockScanResultProvider provider = new MockScanResultProvider(10, -60, -40);
        FitbitGatt.getInstance().getPeripheralScanner().addRssiFilter(-60);
        List<ScanResult> results = provider.getAllResults();
        FitbitGatt.FitbitGattCallback cb = new FitbitGatt.FitbitGattCallback() {

            @Override
            public void onBluetoothPeripheralDiscovered(@NonNull GattConnection connection) {
                Assert.assertTrue("RSSI must be above -60", connection.getDevice().getRssi() > -60);
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
        };
        FitbitGatt.getInstance().registerGattEventListener(cb);
        FitbitGatt.getInstance().getPeripheralScanner().populateMockScanResultBatchValues(results);
    }

    @Test
    public void testConnectionAlreadyInMapDisconnectedScanResult() {
        MockScanResultProvider provider = new MockScanResultProvider(10, -167, -40);
        if(FitbitGatt.getInstance().getPeripheralScanner() == null) {
            fail();
        }
        FitbitGatt.getInstance().getPeripheralScanner().addRssiFilter(-10);
        ScanResult result = provider.getAllResults().get(0);
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(result.getDevice());
        GattConnection conn = new GattConnection(device, mockLooper);
        conn.setMockMode(true);
        conn.setState(GattState.DISCONNECTED);
        FitbitGatt.getInstance().getConnectionMap().put(device, conn);
        FitbitGatt.FitbitGattCallback cb = new FitbitGatt.FitbitGattCallback() {

            @Override
            public void onBluetoothPeripheralDiscovered(@NonNull GattConnection connection) {
                Assert.assertEquals(conn, connection);
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
        };
        FitbitGatt.getInstance().registerGattEventListener(cb);
        FitbitGatt.getInstance().getPeripheralScanner().populateMockScanResultIndividualValue(ScanSettings.CALLBACK_TYPE_FIRST_MATCH, result);
    }

    @Test
    public void testConnectionAlreadyInMapScannedPropertiesChangedRssi() {
        MockScanResultProvider provider = new MockScanResultProvider(10, -167, -40);
        if(FitbitGatt.getInstance().getPeripheralScanner() == null) {
            fail();
        }
        FitbitGatt.getInstance().getPeripheralScanner().addRssiFilter(-10);
        ScanResult result = provider.getAllResults().get(0);
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(result.getDevice());
        device.setScanRecord(result.getScanRecord());
        GattConnection conn = new GattConnection(device, mockLooper);
        conn.setMockMode(true);
        conn.setState(GattState.DISCONNECTED);
        FitbitGatt.getInstance().getConnectionMap().put(device, conn);
        FitbitBluetoothDevice.DevicePropertiesChangedCallback propChanged = device1 -> Assert.assertEquals(1, device1.getRssi());
        device.addDevicePropertiesChangedListener(propChanged);
        device.setRssi(1);
        device.removeDevicePropertiesChangedListener(propChanged);
        FitbitGatt.FitbitGattCallback cb = new FitbitGatt.FitbitGattCallback() {

            @Override
            public void onBluetoothPeripheralDiscovered(@NonNull GattConnection connection) {
                Assert.assertEquals(conn, connection);
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
        };
        FitbitGatt.getInstance().registerGattEventListener(cb);
        FitbitGatt.getInstance().addScannedDevice(device);
    }

    @Test
    public void testConnectionAlreadyInMapScannedPropertiesChangedName() {
        MockScanResultProvider provider = new MockScanResultProvider(10, -167, -40);
        if(FitbitGatt.getInstance().getPeripheralScanner() == null) {
            fail();
        }
        FitbitGatt.getInstance().getPeripheralScanner().addRssiFilter(-10);
        ScanResult result = provider.getAllResults().get(0);
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(result.getDevice());
        device.setScanRecord(result.getScanRecord());
        GattConnection conn = new GattConnection(device, mockLooper);
        conn.setMockMode(true);
        conn.setState(GattState.DISCONNECTED);
        FitbitGatt.getInstance().getConnectionMap().put(device, conn);
        FitbitBluetoothDevice.DevicePropertiesChangedCallback propChanged = device1 -> Assert.assertEquals("Yogurt", device1.getName());
        device.addDevicePropertiesChangedListener(propChanged);
        device.setName("Yogurt");
        device.removeDevicePropertiesChangedListener(propChanged);
        FitbitGatt.FitbitGattCallback cb = new FitbitGatt.FitbitGattCallback() {

            @Override
            public void onBluetoothPeripheralDiscovered(@NonNull GattConnection connection) {
                Assert.assertEquals(conn, connection);
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
        };
        FitbitGatt.getInstance().registerGattEventListener(cb);
        FitbitGatt.getInstance().addScannedDevice(device);
    }

    @Test
    public void testConnectionAlreadyInMapScannedPropertiesChangedScanRecord() {
        MockScanResultProvider provider = new MockScanResultProvider(10, -167, -40);
        if(FitbitGatt.getInstance().getPeripheralScanner() == null) {
            fail();
        }
        FitbitGatt.getInstance().getPeripheralScanner().addRssiFilter(-10);
        ScanResult result = provider.getAllResults().get(0);
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(result.getDevice());
        device.setScanRecord(result.getScanRecord());
        GattConnection conn = new GattConnection(device, mockLooper);
        conn.setMockMode(true);
        conn.setState(GattState.DISCONNECTED);
        FitbitGatt.getInstance().getConnectionMap().put(device, conn);
        FitbitBluetoothDevice.DevicePropertiesChangedCallback propChanged = device1 -> Assert.assertNull(device1.getScanRecord());
        device.addDevicePropertiesChangedListener(propChanged);
        device.setScanRecord(null);
        device.removeDevicePropertiesChangedListener(propChanged);
        FitbitGatt.FitbitGattCallback cb = new FitbitGatt.FitbitGattCallback() {

            @Override
            public void onBluetoothPeripheralDiscovered(@NonNull GattConnection connection) {
                Assert.assertEquals(conn, connection);
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
        };
        FitbitGatt.getInstance().registerGattEventListener(cb);
        FitbitGatt.getInstance().addScannedDevice(device);
    }

    @Test
    public void testStopHighPriorityScanCallbackWorks(){
        final boolean[] startHP = {false};
        FitbitGatt.FitbitGattCallback cb = new FitbitGatt.FitbitGattCallback() {

            @Override
            public void onBluetoothPeripheralDiscovered(@NonNull GattConnection connection) {

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
                if(startHP[0]) {
                    if (FitbitGatt.getInstance().getPeripheralScanner() == null) {
                        fail();
                        return;
                    }
                    Assert.assertTrue(FitbitGatt.getInstance().getPeripheralScanner().isPeriodicalScanEnabled());
                    Assert.assertFalse(FitbitGatt.getInstance().getPeripheralScanner().isScanning());
                } else {
                    startHP[0] = true;
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

            @Override
            public void onBluetoothTurningOn() {

            }

            @Override
            public void onBluetoothTurningOff() {

            }
        };
        FitbitGatt.getInstance().registerGattEventListener(cb);
        if(FitbitGatt.getInstance().getPeripheralScanner() == null) {
            fail();
            return;
        }
        FitbitGatt.getInstance().getPeripheralScanner().addRssiFilter(-10);
        FitbitGatt.getInstance().getPeripheralScanner().startPeriodicScan(mockContext);
        FitbitGatt.getInstance().getPeripheralScanner().startHighPriorityScan(mockContext);
        FitbitGatt.getInstance().getPeripheralScanner().cancelHighPriorityScan(mockContext);
        FitbitGatt.getInstance().unregisterGattEventListener(cb);
    }
}
