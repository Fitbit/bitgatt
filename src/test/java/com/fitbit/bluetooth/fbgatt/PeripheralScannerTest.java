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
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.stubbing.Answer;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import androidx.annotation.NonNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test the logic in the peripheral scanner
 *
 * Created by iowens on 9/4/18.
 */
@RunWith(JUnit4.class)
public class PeripheralScannerTest {

    private MockLollipopScanner mockScanner;
    private Looper mockLooper = mock(Looper.class);
    public Context mockContext;
    private FitbitGatt gatt;
    private PeripheralScanner peripheralScanner;
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


    @Before
    public void before(){
        mockScanner = MockLollipopScanner.BluetoothAdapter.getBluetoothLeScanner();
        BluetoothUtils utilsMock = mock(BluetoothUtils.class);
        LowEnergyAclListener lowEnergyAclListenerMock = mock(LowEnergyAclListener.class);
        BluetoothAdapter adapterMock = mock(BluetoothAdapter.class);
        BluetoothRadioStatusListener bluetoothRadioStatusListenerMock = mock(BluetoothRadioStatusListener.class);
        BitGattDependencyProvider dependencyProviderMock = mock(BitGattDependencyProvider.class);

        MockLollipopScanner.BluetoothAdapter.turnBluetoothOn();
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
        doReturn(mockContext).when(mockContext).getApplicationContext();
        doReturn(bluetoothRadioStatusListenerMock).when(dependencyProviderMock).getNewBluetoothRadioStatusListener(mockContext, false);
        doReturn(utilsMock).when(dependencyProviderMock).getBluetoothUtils();
        doReturn(lowEnergyAclListenerMock).when(dependencyProviderMock).getNewLowEnergyAclListener();
        doCallRealMethod().when(dependencyProviderMock).getNewPeripheralScanner(any(), any());
        doReturn(true).when(utilsMock).isBluetoothEnabled(mockContext);
        gatt = FitbitGatt.getInstance();
        gatt.setDependencyProvider(dependencyProviderMock);
        gatt.setAsyncOperationThreadWatchdog(mock(LooperWatchdog.class));
        gatt.initializeScanner(mockContext);
        gatt.setScannerMockMode(true);
        gatt.clearConnectionsMap();

        if(gatt.getPeripheralScanner() == null) {
            fail();
            return;
        }
        peripheralScanner = gatt.getPeripheralScanner();
        peripheralScanner.setHandler(mockHandler);
        peripheralScanner.injectMockScanner(mockScanner);
        peripheralScanner.cancelPeriodicalScan(mockContext);
        peripheralScanner.cancelHighPriorityScan(mockContext);
        peripheralScanner.cancelPendingIntentBasedBackgroundScan();
    }

    @BeforeClass
    public static void beforeClass() {
        FitbitGatt.setInstance(null);
    }

    @After
    public void after() {
        gatt.unregisterAllGattEventListeners();
        gatt.shutdown();
        FitbitGatt.setInstance(null);
    }

    @Test
    public void testRssiFilteredScanNoResults() {
        assertFalse(peripheralScanner.isScanning());
        MockScanResultProvider provider = new MockScanResultProvider(10, -167, -40);
        peripheralScanner.addRssiFilter(-10);
        List<ScanResult> results = provider.getAllResults();
        peripheralScanner.populateMockScanResultBatchValues(results);
        Assert.assertEquals(0, gatt.getNewlyScannedDevicesOnly().size());
    }

    @Test
    public void testRssiFilteredScanResults() {
        assertFalse(peripheralScanner.isScanning());
        MockScanResultProvider provider = new MockScanResultProvider(10, -60, -40);
        peripheralScanner.addRssiFilter(-60);
        List<ScanResult> results = provider.getAllResults();
        NoOpGattCallback cb = new NoOpGattCallback() {

            @Override
            public void onBluetoothPeripheralDiscovered(@NonNull GattConnection connection) {
                Assert.assertTrue("RSSI must be above -60", connection.getDevice().getRssi() > -60);
                gatt.unregisterGattEventListener(this);
            }
        };
        gatt.registerGattEventListener(cb);
        gatt.getPeripheralScanner().populateMockScanResultBatchValues(results);
    }

    @Test
    public void testConnectionAlreadyInMapDisconnectedScanResult() {
        MockScanResultProvider provider = new MockScanResultProvider(10, -167, -40);
        peripheralScanner.addRssiFilter(-10);
        ScanResult result = provider.getAllResults().get(0);
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(result.getDevice());
        GattConnection conn = new GattConnection(device, mockLooper);
        conn.setMockMode(true);
        conn.setState(GattState.DISCONNECTED);
        gatt.getConnectionMap().put(device, conn);
        NoOpGattCallback cb = new NoOpGattCallback() {

            @Override
            public void onBluetoothPeripheralDiscovered(@NonNull GattConnection connection) {
                Assert.assertEquals(conn, connection);
                gatt.unregisterGattEventListener(this);
            }

        };
        gatt.registerGattEventListener(cb);
        peripheralScanner.populateMockScanResultIndividualValue(ScanSettings.CALLBACK_TYPE_FIRST_MATCH, result);
    }

    @Test
    public void testConnectionAlreadyInMapScannedPropertiesChangedRssi() {
        MockScanResultProvider provider = new MockScanResultProvider(10, -167, -40);
        peripheralScanner.addRssiFilter(-10);
        ScanResult result = provider.getAllResults().get(0);
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(result.getDevice());
        device.setScanRecord(result.getScanRecord());
        GattConnection conn = new GattConnection(device, mockLooper);
        conn.setMockMode(true);
        conn.setState(GattState.DISCONNECTED);
        gatt.getConnectionMap().put(device, conn);
        FitbitBluetoothDevice.DevicePropertiesChangedCallback propChanged = device1 -> Assert.assertEquals(1, device1.getRssi());
        device.addDevicePropertiesChangedListener(propChanged);
        device.setRssi(1);
        device.removeDevicePropertiesChangedListener(propChanged);
        NoOpGattCallback cb = new NoOpGattCallback() {

            @Override
            public void onBluetoothPeripheralDiscovered(@NonNull GattConnection connection) {
                Assert.assertEquals(conn, connection);
                gatt.unregisterGattEventListener(this);
            }

        };
        gatt.registerGattEventListener(cb);
        gatt.addScannedDevice(device);
    }

    @Test
    public void testConnectionAlreadyInMapScannedPropertiesChangedName() {
        MockScanResultProvider provider = new MockScanResultProvider(10, -167, -40);
        peripheralScanner.addRssiFilter(-10);
        ScanResult result = provider.getAllResults().get(0);
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(result.getDevice());
        device.setScanRecord(result.getScanRecord());
        GattConnection conn = new GattConnection(device, mockLooper);
        conn.setMockMode(true);
        conn.setState(GattState.DISCONNECTED);
        gatt.getConnectionMap().put(device, conn);
        FitbitBluetoothDevice.DevicePropertiesChangedCallback propChanged = device1 -> Assert.assertEquals("Yogurt", device1.getName());
        device.addDevicePropertiesChangedListener(propChanged);
        device.setName("Yogurt");
        device.removeDevicePropertiesChangedListener(propChanged);
        NoOpGattCallback cb = new NoOpGattCallback() {

            @Override
            public void onBluetoothPeripheralDiscovered(@NonNull GattConnection connection) {
                Assert.assertEquals(conn, connection);
                gatt.unregisterGattEventListener(this);
            }

        };
        gatt.registerGattEventListener(cb);
        gatt.addScannedDevice(device);
    }

    @Test
    public void testConnectionAlreadyInMapScannedPropertiesChangedScanRecord() {
        MockScanResultProvider provider = new MockScanResultProvider(10, -167, -40);
        peripheralScanner.addRssiFilter(-10);
        ScanResult result = provider.getAllResults().get(0);
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(result.getDevice());
        device.setScanRecord(result.getScanRecord());
        GattConnection conn = new GattConnection(device, mockLooper);
        conn.setMockMode(true);
        conn.setState(GattState.DISCONNECTED);
        gatt.getConnectionMap().put(device, conn);
        FitbitBluetoothDevice.DevicePropertiesChangedCallback propChanged = device1 -> Assert.assertNull(device1.getScanRecord());
        device.addDevicePropertiesChangedListener(propChanged);
        device.setScanRecord(null);
        device.removeDevicePropertiesChangedListener(propChanged);
        NoOpGattCallback cb = new NoOpGattCallback() {

            @Override
            public void onBluetoothPeripheralDiscovered(@NonNull GattConnection connection) {
                Assert.assertEquals(conn, connection);
                gatt.unregisterGattEventListener(this);
            }

        };
        gatt.registerGattEventListener(cb);
        gatt.addScannedDevice(device);
    }

    @Test
    public void testStopHighPriorityScanCallbackWorks(){
        final boolean[] startHP = {false};
        NoOpGattCallback cb = new NoOpGattCallback() {


            @Override
            public void onScanStopped() {
                if(startHP[0]) {
                    if (gatt.getPeripheralScanner() == null) {
                        fail();
                        return;
                    }
                    Assert.assertTrue(gatt.getPeripheralScanner().isPeriodicalScanEnabled());
                    Assert.assertFalse(gatt.getPeripheralScanner().isScanning());
                } else {
                    startHP[0] = true;
                }
            }

        };
        gatt.registerGattEventListener(cb);
        peripheralScanner.addRssiFilter(-10);
        peripheralScanner.startPeriodicScan(mockContext);
        peripheralScanner.startHighPriorityScan(mockContext);
        peripheralScanner.cancelHighPriorityScan(mockContext);
        gatt.unregisterGattEventListener(cb);
    }

    @Test
    public void testPeripheralScannerStartLowLatencyScanTimeoutPeriodicShouldNotBeRunning() {
        // we want to do this so that we don't end up with a super long wait
        peripheralScanner.setInstrumentationTestMode(true);
        peripheralScanner.addRssiFilter(-10);
        boolean didHighPriorityScanStart = peripheralScanner.startHighPriorityScan(mockContext);
        if(!didHighPriorityScanStart) {
            fail("Couldn't start high priority scan");
        }
        peripheralScanner.scanTimeoutRunnable.run();
        assertFalse(peripheralScanner.isPeriodicalScanEnabled());
        peripheralScanner.setInstrumentationTestMode(false);
    }

    @Test
    public void testPeripheralScannerStartHighAndLowLatencyScanTimeoutPeriodicShouldBeRunning() {
        // we want to do this so that we don't end up with a super long wait
        peripheralScanner.setInstrumentationTestMode(true);
        peripheralScanner.addRssiFilter(-10);
        boolean started = peripheralScanner.startPeriodicScan(mockContext);
        if(!started) {
            fail("The periodical scan never started");
        }
        started = peripheralScanner.startHighPriorityScan(mockContext);
        if(!started) {
            fail("The high priority scan never started");
        }
        peripheralScanner.scanTimeoutRunnable.run();
        assertTrue(peripheralScanner.isPeriodicalScanEnabled());
        peripheralScanner.setInstrumentationTestMode(false);
    }
}
