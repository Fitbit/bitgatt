/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

/*
 * Test gatt connection stuff
 */

import androidx.test.core.app.ApplicationProvider;
import com.fitbit.bluetooth.fbgatt.btcopies.BluetoothGattCharacteristicCopy;
import com.fitbit.bluetooth.fbgatt.btcopies.BluetoothGattDescriptorCopy;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.os.Looper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.ArrayList;
import java.util.List;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowBluetoothDevice;
import org.robolectric.shadows.ShadowBluetoothGatt;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
@Config(minSdk = 21)
@Ignore("We need to be able to run transactions under robolectric")
public class GattConnectionTest {

    private static final String MOCK_ADDRESS = "02:00:00:00:00:00";
    private static final String MOCK_NAME = "foobar";

    private static GattConnection connection;
    private static GattServerConnection serverConnection;


    @Before
    public void before(){
        Context context = ApplicationProvider.getApplicationContext();
        FitbitGatt instance = FitbitGatt.getInstance();

        instance.start(context);
        instance.getServer().setMockMode(true);

        Looper mockLooper = context.getMainLooper();
        FitbitBluetoothDevice fitbitDeviceMock = mock(FitbitBluetoothDevice.class);
        doReturn(MOCK_ADDRESS).when(fitbitDeviceMock).getAddress();
        doReturn(MOCK_NAME).when(fitbitDeviceMock).getName();
        connection = new GattConnection(fitbitDeviceMock, mockLooper);
        connection.setMockMode(true);

        FitbitGatt.getInstance().setClientCallback(new GattClientCallback());
    }

    @After
    public void after() {
        FitbitGatt.getInstance().setClientCallback(null);
        FitbitGatt.setInstance(null);
    }

    @Test
    public void testRemovingGattConnectionEventListeners(){
        ConnectionEventListener listener = new ConnectionEventListener() {
            @Override
            public void onClientCharacteristicChanged(@NonNull TransactionResult result, @NonNull GattConnection connection) {

            }

            @Override
            public void onClientConnectionStateChanged(@NonNull TransactionResult result, @NonNull GattConnection connection) {

            }

            @Override
            public void onServicesDiscovered(@NonNull TransactionResult result, @NonNull GattConnection connection) {

            }

            @Override
            public void onMtuChanged(@NonNull TransactionResult result, @NonNull GattConnection connection) {

            }

            @Override
            public void onPhyChanged(@NonNull TransactionResult result, @NonNull GattConnection connection) {

            }
        };
        connection.registerConnectionEventListener(listener);
        connection.unregisterConnectionEventListener(listener);
        Assert.assertTrue("Connection event listeners should be empty",
                connection.getConnectionEventListeners().isEmpty());
    }

    @Test
    public void testRemovingGattServerConnectionEventListeners(){
        ServerConnectionEventListener listener = new ServerConnectionEventListener() {
            @Override
            public void onServerMtuChanged(@NonNull BluetoothDevice device, @NonNull TransactionResult result, @NonNull GattServerConnection connection) {

            }

            @Override
            public void onServerConnectionStateChanged(@NonNull BluetoothDevice device, @NonNull TransactionResult result, @NonNull GattServerConnection connection) {

            }

            @Override
            public void onServerCharacteristicWriteRequest(@NonNull BluetoothDevice device, @NonNull TransactionResult result, @NonNull GattServerConnection connection) {

            }

            @Override
            public void onServerCharacteristicReadRequest(@NonNull BluetoothDevice device, @NonNull TransactionResult result, @NonNull GattServerConnection connection) {

            }

            @Override
            public void onServerDescriptorWriteRequest(@NonNull BluetoothDevice device, @NonNull TransactionResult result, @NonNull GattServerConnection connection) {

            }

            @Override
            public void onServerDescriptorReadRequest(@NonNull BluetoothDevice device, @NonNull TransactionResult result, @NonNull GattServerConnection connection) {

            }
        };
        serverConnection.registerConnectionEventListener(listener);
        serverConnection.unregisterConnectionEventListener(listener);
        Assert.assertTrue("Server Connection event listeners should be empty", serverConnection.getConnectionEventListeners().isEmpty());
    }

    @Test
    public void gattClientEventListenerShouldRegisterOne(){

        GattClientListener listener = new GattClientListener() {
            @Nullable
            @Override
            public FitbitBluetoothDevice getDevice() {
                return connection.getDevice();
            }

            @Override
            public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {

            }

            @Override
            public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {

            }

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristicCopy characteristic, int status) {

            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristicCopy characteristic, int status) {

            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristicCopy characteristic) {

            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptorCopy descriptor, int status) {

            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptorCopy descriptor, int status) {

            }

            @Override
            public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {

            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {

            }

            @Override
            public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {

            }
        };
        connection.registerGattClientListener(listener);
        List<GattClientListener> listnerList = FitbitGatt.getInstance().getClientCallback().getGattClientListeners();
        Assert.assertEquals(1, listnerList.size());
        connection.unregisterGattClientListener(listener);
        listnerList = FitbitGatt.getInstance().getClientCallback().getGattClientListeners();
        Assert.assertEquals(0, listnerList.size());
    }

    @Test
    public void gattClientEventListenerShouldReceiveCallback(){
        BluetoothDevice bluetoothDevice = ShadowBluetoothDevice.newInstance(MOCK_ADDRESS);
        BluetoothGatt gatt = ShadowBluetoothGatt.newInstance(bluetoothDevice);
        GattClientListener listener = new GattClientListener() {
            @Nullable
            @Override
            public FitbitBluetoothDevice getDevice() {
                return connection.getDevice();
            }

            @Override
            public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
                Assert.assertEquals(1, status);
            }

            @Override
            public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {

            }

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristicCopy characteristic, int status) {

            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristicCopy characteristic, int status) {

            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristicCopy characteristic) {

            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptorCopy descriptor, int status) {

            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptorCopy descriptor, int status) {

            }

            @Override
            public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {

            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {

            }

            @Override
            public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {

            }
        };
        connection.registerGattClientListener(listener);
        List<GattClientListener> listenerList = FitbitGatt.getInstance().getClientCallback().getGattClientListeners();
        Assert.assertEquals(1, listenerList.size());
        FitbitGatt.getInstance().getClientCallback().onPhyUpdate(gatt, 1, 1, 1);
        connection.unregisterGattClientListener(listener);
        listenerList = FitbitGatt.getInstance().getClientCallback().getGattClientListeners();
        Assert.assertEquals(0, listenerList.size());
    }

    @Test
    public void testNullEventListenerCrashes() {
        try {
            connection.registerConnectionEventListener(null);
            fail("A NullPointerException was expected");
        } catch (NullPointerException npe) {
            //This is expected.
        }

        try {
            connection.unregisterConnectionEventListener(null);
            fail("A NullPointerException was expected");
        } catch (NullPointerException npe) {
            //This is expected.
        }

        try {
            serverConnection.registerConnectionEventListener(null);
            fail("A NullPointerException was expected");
        } catch (NullPointerException npe) {
            //This is expected.
        }

        try {
            serverConnection.unregisterConnectionEventListener(null);
            fail("A NullPointerException was expected");
        } catch (NullPointerException npe) {
            //This is expected.
        }
    }

    @Test
    public void testHighlyConcurrentAccess() {
        List<Thread> threadList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            threadList.add(createRegisteringThread());
        }
        for (Thread thread : threadList) {
            thread.start();
        }
        for (Thread thread : threadList) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                fail();
            }
        }
        assertEquals(5000, connection.getConnectionEventListeners().size());
    }

    private Thread createRegisteringThread() {
        return new Thread() {
            @Override
            public void run() {
                for (int i = 0; i < 1000; i++) {
                    connection.registerConnectionEventListener(new ConnectionEventListener() {
                        @Override
                        public void onClientCharacteristicChanged(@NonNull TransactionResult result, @NonNull GattConnection connection) {

                        }

                        @Override
                        public void onClientConnectionStateChanged(@NonNull TransactionResult result, @NonNull GattConnection connection) {

                        }

                        @Override
                        public void onServicesDiscovered(@NonNull TransactionResult result, @NonNull GattConnection connection) {

                        }

                        @Override
                        public void onMtuChanged(@NonNull TransactionResult result, @NonNull GattConnection connection) {

                        }

                        @Override
                        public void onPhyChanged(@NonNull TransactionResult result, @NonNull GattConnection connection) {

                        }
                    });
                }
            }
        };
    }
}
