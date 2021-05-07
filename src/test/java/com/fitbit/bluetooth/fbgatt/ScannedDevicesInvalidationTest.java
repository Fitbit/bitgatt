/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.tx.mocks.ReadGattCharacteristicMockTransaction;
import com.fitbit.bluetooth.fbgatt.util.BluetoothUtils;
import com.fitbit.bluetooth.fbgatt.util.LooperWatchdog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.stubbing.Answer;
import java.util.UUID;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * A delightful class, filled with stimulating code and whimsical
 * fancy for your reading pleasure
 * <p>
 * Created by iowens on 5/10/18.
 */
@RunWith(JUnit4.class)
public class ScannedDevicesInvalidationTest {

    private static final String MOCK_ADDRESS = "02:00:00:00:00:00";
    private GattConnection conn;
    private FitbitBluetoothDevice device;
    private FitbitGatt gatt;


    @Before
    public void before() {
        BluetoothUtils utilsMock = mock(BluetoothUtils.class);
        LowEnergyAclListener lowEnergyAclListenerMock = mock(LowEnergyAclListener.class);
        BluetoothAdapter adapterMock = mock(BluetoothAdapter.class);
        BluetoothRadioStatusListener bluetoothRadioStatusListenerMock = mock(BluetoothRadioStatusListener.class);
        BitGattDependencyProvider dependencyProviderMock = mock(BitGattDependencyProvider.class);
        Context mockContext = mock(Context.class);
        doReturn(mockContext).when(mockContext).getApplicationContext();
        doReturn(bluetoothRadioStatusListenerMock).when(dependencyProviderMock).getNewBluetoothRadioStatusListener(mockContext, false);
        doReturn(utilsMock).when(dependencyProviderMock).getBluetoothUtils();
        doReturn(lowEnergyAclListenerMock).when(dependencyProviderMock).getNewLowEnergyAclListener();
        doReturn(adapterMock).when(utilsMock).getBluetoothAdapter(mockContext);
        doCallRealMethod().when(dependencyProviderMock).getNewPeripheralScanner(eq(gatt), any());
        doReturn(true).when(adapterMock).isEnabled();
        Handler mockHandler = mock(Handler.class);
        Looper mockLooper = mock(Looper.class);
        Thread mockThread = mock(Thread.class);
        when(mockThread.getName()).thenReturn("Irvin's mock thread");
        when(mockLooper.getThread()).thenReturn(mockThread);
        when(mockHandler.getLooper()).thenReturn(mockLooper);
        when(mockHandler.sendMessageAtTime(any(Message.class), anyLong())).thenAnswer((Answer) invocation -> {
            Message msg = invocation.getArgument(0);
            msg.getCallback().run();
            return null;
        });
        device = new FitbitBluetoothDevice(MOCK_ADDRESS, "fooDevice", mock(BluetoothDevice.class));
        conn = spy(new GattConnection(device, mockLooper));
        conn.setMockMode(true);
        when(conn.getMainHandler()).thenReturn(mockHandler);
        doNothing().when(conn).finish();


        GattServerConnection serverConnection = spy(new GattServerConnection(null, mockLooper));
        serverConnection.setMockMode(true);
        when(serverConnection.getMainHandler()).thenReturn(mockHandler);
        when(mockContext.registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class))).thenReturn(new Intent());
        gatt = FitbitGatt.getInstance();
        gatt.setAsyncOperationThreadWatchdog(mock(LooperWatchdog.class));
        gatt.setConnectionCleanup(mock(Handler.class));
        gatt.setDependencyProvider(dependencyProviderMock);
        gatt.putConnectionIntoDevices(device, conn);
        gatt.startGattServer(mockContext);
        gatt.setGattServerConnection(serverConnection);

    }

    @After
    public void after(){
        gatt.getConnectionMap().clear();
        FitbitGatt.setInstance(null);
    }

    @Test
    public void testDisconnectedScannedDevicesRemoval() {
        conn.setState(GattState.DISCONNECTED);
        conn.setDisconnectedTTL(0);
        gatt.doDecrementAndInvalidateClosedConnections();
        assertFalse("There are no remaining connections", gatt.getConnectionMap().containsKey(device));
    }

    @Test
    public void testDisconnectedScannedDevicesNonRemoval(){
        conn.setState(GattState.DISCONNECTED);
        conn.setDisconnectedTTL(500);
        gatt.doDecrementAndInvalidateClosedConnections();
        assertTrue("The connection remains", gatt.getConnectionMap().containsKey(device));
    }

    @Test
    public void testConnectedDevicesAtZeroTtlNonRemoval(){
        conn.setState(GattState.CONNECTED);
        conn.setDisconnectedTTL(0);
        gatt.doDecrementAndInvalidateClosedConnections();
        assertTrue("The connection remains", gatt.getConnectionMap().containsKey(device));
    }

    @Test
    public void testConnectedDevicesNonRemoval(){
        conn.setState(GattState.CONNECTED);
        conn.setDisconnectedTTL(800);
        gatt.doDecrementAndInvalidateClosedConnections();
        assertTrue("The connection remains", gatt.getConnectionMap().containsKey(device));
    }

    @Test
    public void testConnectionTxResetTtl(){
        conn.setState(GattState.CONNECTED);
        conn.setDisconnectedTTL(300);
        final byte[] fakeData = new byte[]{'a','b','c','d','e','f','g','h','i', 'j'};
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID.randomUUID(), BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
        ReadGattCharacteristicMockTransaction readChar = new ReadGattCharacteristicMockTransaction(conn, GattState.READ_CHARACTERISTIC_SUCCESS, characteristic, fakeData, false);
        conn.runTx(readChar, result -> assertEquals("Tx result was successful", result.resultState, GattState.READ_CHARACTERISTIC_SUCCESS));
        gatt.doDecrementAndInvalidateClosedConnections();
        assertEquals(FitbitGatt.MAX_TTL, conn.getDisconnectedTTL());
    }
}
