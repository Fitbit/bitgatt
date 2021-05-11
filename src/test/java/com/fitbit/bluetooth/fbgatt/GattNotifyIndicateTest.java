/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.tx.mocks.SubscribeToCharacteristicNotificationsMockTransaction;
import com.fitbit.bluetooth.fbgatt.tx.mocks.UnSubscribeToGattCharacteristicNotificationsMockTransaction;
import com.fitbit.bluetooth.fbgatt.util.LooperWatchdog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.stubbing.Answer;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class GattNotifyIndicateTest {
    private static final String MOCK_ADDRESS = "02:00:00:00:00:00";
    private GattConnection conn;
    private FitbitBluetoothDevice device;

    @Before
    public void before() {
        Context ctx = mock(Context.class);
        Looper looper = mock(Looper.class);
        when(ctx.getApplicationContext()).thenReturn(ctx);
        Handler mockHandler = mock(Handler.class);
        Looper mockLooper = mock(Looper.class);
        Thread mockThread = mock(Thread.class);
        when(mockThread.getName()).thenReturn("Irvin's mock thread");
        when(mockLooper.getThread()).thenReturn(mockThread);
        when(mockHandler.getLooper()).thenReturn(mockLooper);
        when(mockHandler.postDelayed(any(Runnable.class), anyLong())).thenAnswer((Answer) invocation -> {
            Runnable msg = invocation.getArgument(0);
            msg.run();
            return null;
        });
        when(mockHandler.post(any(Runnable.class))).thenAnswer((Answer) invocation -> {
            Runnable msg = invocation.getArgument(0);
            msg.run();
            return null;
        });
        device = new FitbitBluetoothDevice(MOCK_ADDRESS, "fooDevice", mock(BluetoothDevice.class));
        conn = spy(new GattConnection(device, mockLooper));
        conn.setMockMode(true);
        when(conn.getMainHandler()).thenReturn(mockHandler);
        FitbitGatt.getInstance().putConnectionIntoDevices(device, conn);
        GattServerConnection serverConnection = spy(new GattServerConnection(null, looper));
        serverConnection.setMockMode(true);
        when(serverConnection.getMainHandler()).thenReturn(mockHandler);
        conn.setState(GattState.IDLE);
        serverConnection.setState(GattState.IDLE);
        FitbitGatt.getInstance().setAsyncOperationThreadWatchdog(mock(LooperWatchdog.class));
        FitbitGatt.getInstance().startGattServer(ctx);
        FitbitGatt.getInstance().setGattServerConnection(serverConnection);
    }

    @Test
    public void subscribeToNotificationTest() {
        final byte[] fakeData = new byte[]{'a','b','c','d','e','f','g','h','i', 'j'};
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID.randomUUID(), BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED);
        SubscribeToCharacteristicNotificationsMockTransaction subscribeChar = new SubscribeToCharacteristicNotificationsMockTransaction(conn, GattState.ENABLE_CHARACTERISTIC_NOTIFICATION_SUCCESS, characteristic, fakeData, false);
        conn.runTx(subscribeChar, result -> {
            assert(result.resultStatus.equals(TransactionResult.TransactionResultStatus.SUCCESS) && result.resultState.equals(subscribeChar.getSuccessState()));
        });
    }

    @Test
    public void unSubscribeToNotificationTest() {
        final byte[] fakeData = new byte[]{'a','b','c','d','e','f','g','h','i', 'j'};
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID.randomUUID(), BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED);
        UnSubscribeToGattCharacteristicNotificationsMockTransaction unSubscribeChar = new UnSubscribeToGattCharacteristicNotificationsMockTransaction(conn, GattState.ENABLE_CHARACTERISTIC_NOTIFICATION_SUCCESS, characteristic, fakeData, false);
        conn.runTx(unSubscribeChar, result -> {
            assert(result.resultStatus.equals(TransactionResult.TransactionResultStatus.SUCCESS) && result.resultState.equals(unSubscribeChar.getSuccessState()));
        });
    }

    @Test
    public void failToSubscribeToNotificationTest() {
        final byte[] fakeData = new byte[]{'a','b','c','d','e','f','g','h','i', 'j'};
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID.randomUUID(), BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED);
        SubscribeToCharacteristicNotificationsMockTransaction subscribeChar = new SubscribeToCharacteristicNotificationsMockTransaction(conn, GattState.ENABLE_CHARACTERISTIC_NOTIFICATION_SUCCESS, characteristic, fakeData, true);
        conn.runTx(subscribeChar, result -> {
            assert(result.resultStatus.equals(TransactionResult.TransactionResultStatus.TIMEOUT));
        });
    }

    @Test
    public void failToUnSubscribeToNotificationTest() {
        final byte[] fakeData = new byte[]{'a','b','c','d','e','f','g','h','i', 'j'};
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID.randomUUID(), BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED);
        UnSubscribeToGattCharacteristicNotificationsMockTransaction unSubscribeChar = new UnSubscribeToGattCharacteristicNotificationsMockTransaction(conn, GattState.ENABLE_CHARACTERISTIC_NOTIFICATION_SUCCESS, characteristic, fakeData, true);
        conn.runTx(unSubscribeChar, result -> {
            assert(result.resultStatus.equals(TransactionResult.TransactionResultStatus.TIMEOUT));
        });
    }
}
