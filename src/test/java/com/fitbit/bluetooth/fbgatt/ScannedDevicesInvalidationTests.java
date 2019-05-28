/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.fitbit.bluetooth.fbgatt.tx.mocks.ReadGattCharacteristicMockTransaction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * A delightful class, filled with stimulating code and whimsical
 * fancy for your reading pleasure
 *
 * Created by iowens on 5/10/18.
 */
public class ScannedDevicesInvalidationTests {

    private static final String MOCK_ADDRESS = "02:00:00:00:00:00";
    private GattConnection conn;
    private FitbitBluetoothDevice device;

    @Before
    public void before() {
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
        FitbitGatt.getInstance().putConnectionIntoDevices(device, conn);
        GattServerConnection serverConnection = spy(new GattServerConnection(null, mockLooper));
        serverConnection.setMockMode(true);
        when(serverConnection.getMainHandler()).thenReturn(mockHandler);
        FitbitGatt.getInstance().setGattServer(serverConnection);
        Context mockContext = mock(Context.class);
        when(mockContext.getApplicationContext()).thenReturn(mockContext);
        when(mockContext.registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class))).thenReturn(new Intent());
        FitbitGatt.getInstance().start(mockContext);
    }

    @After
    public void after(){
        FitbitGatt.getInstance().getConnectionMap().clear();
    }

    @Test
    public void testDisconnectedScannedDevicesRemoval() {
        conn.setState(GattState.DISCONNECTED);
        conn.setDisconnectedTTL(0);
        FitbitGatt.getInstance().doDecrementAndInvalidateClosedConnections();
        assertFalse("There are no remaining connections", FitbitGatt.getInstance().getConnectionMap().containsKey(device));
    }

    @Test
    public void testDisconnectedScannedDevicesNonRemoval(){
        conn.setState(GattState.DISCONNECTED);
        conn.setDisconnectedTTL(500);
        FitbitGatt.getInstance().doDecrementAndInvalidateClosedConnections();
        assertTrue("The connection remains", FitbitGatt.getInstance().getConnectionMap().containsKey(device));
    }

    @Test
    public void testConnectedDevicesAtZeroTtlNonRemoval(){
        conn.setState(GattState.CONNECTED);
        conn.setDisconnectedTTL(0);
        FitbitGatt.getInstance().doDecrementAndInvalidateClosedConnections();
        assertTrue("The connection remains", FitbitGatt.getInstance().getConnectionMap().containsKey(device));
    }

    @Test
    public void testConnectedDevicesNonRemoval(){
        conn.setState(GattState.CONNECTED);
        conn.setDisconnectedTTL(800);
        FitbitGatt.getInstance().doDecrementAndInvalidateClosedConnections();
        assertTrue("The connection remains", FitbitGatt.getInstance().getConnectionMap().containsKey(device));
    }

    @Test
    public void testConnectionTxResetTtl(){
        conn.setState(GattState.CONNECTED);
        conn.setDisconnectedTTL(300);
        final byte[] fakeData = new byte[]{'a','b','c','d','e','f','g','h','i', 'j'};
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID.randomUUID(), BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
        ReadGattCharacteristicMockTransaction readChar = new ReadGattCharacteristicMockTransaction(conn, GattState.READ_CHARACTERISTIC_SUCCESS, characteristic, fakeData, false);
        conn.runTx(readChar, result -> assertEquals("Tx result was successful", result.resultState, GattState.READ_CHARACTERISTIC_SUCCESS));
        FitbitGatt.getInstance().doDecrementAndInvalidateClosedConnections();
        assertEquals(FitbitGatt.MAX_TTL, conn.getDisconnectedTTL());
    }
}
