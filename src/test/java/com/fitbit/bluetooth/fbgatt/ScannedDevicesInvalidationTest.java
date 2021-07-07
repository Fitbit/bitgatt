/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import androidx.test.core.app.ApplicationProvider;
import com.fitbit.bluetooth.fbgatt.tx.mocks.ReadGattCharacteristicMockTransaction;
import com.fitbit.bluetooth.fbgatt.util.LooperWatchdog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.UUID;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowBluetoothDevice;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
@RunWith(RobolectricTestRunner.class)
@Ignore("We need to emulate the gatt server")
public class ScannedDevicesInvalidationTest {

    private static final String MOCK_ADDRESS = "02:00:00:00:00:00";
    private GattConnection conn;
    private FitbitBluetoothDevice device;
    private FitbitGatt gatt;


    @Before
    public void before() {
        Context context = ApplicationProvider.getApplicationContext();
        Looper looper = context.getMainLooper();
        Handler handler = new Handler();
        device = new FitbitBluetoothDevice(MOCK_ADDRESS, "fooDevice", ShadowBluetoothDevice.newInstance("FI:I2:I3:I4"));
        conn = spy(new GattConnection(device, looper));
        conn.setMockMode(true);
        when(conn.getMainHandler()).thenReturn(handler);
        doNothing().when(conn).finish();


        GattServerConnection serverConnection = spy(new GattServerConnection(null, looper));
        serverConnection.setMockMode(true);
        when(serverConnection.getMainHandler()).thenReturn(handler);
        gatt = FitbitGatt.getInstance();
        gatt.setAsyncOperationThreadWatchdog(mock(LooperWatchdog.class));
        gatt.setConnectionCleanup(mock(Handler.class));
        gatt.putConnectionIntoDevices(device, conn);
        gatt.startGattServer(context);
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
