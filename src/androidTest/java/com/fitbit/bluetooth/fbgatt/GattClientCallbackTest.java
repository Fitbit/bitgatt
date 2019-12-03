/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.btcopies.BluetoothGattCharacteristicCopy;
import com.fitbit.bluetooth.fbgatt.btcopies.BluetoothGattDescriptorCopy;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import android.util.Log;

import org.junit.BeforeClass;
import org.junit.Test;

import androidx.annotation.Nullable;
import androidx.test.platform.app.InstrumentationRegistry;

/**
 * Primarily to test GattClientCallback
 * Main issue is that we don't block the binder thread in onCharacteristicChanged
 *
 */

public class GattClientCallbackTest {
    static Context context;
    static FitbitBluetoothDevice device;
    static BluetoothDevice btDevice;
    static BluetoothGatt gatt;
    static String btAddr = "00:00:00:00:00:10";


    @BeforeClass
    public static void beforeClass() {
        context = InstrumentationRegistry.getInstrumentation().getContext();
        FitbitGatt.getInstance().start(context);
        device = new FitbitBluetoothDevice(btAddr, "fooDevice");
        btDevice = device.device;
        gatt = btDevice.connectGatt(context, false, new BluetoothGattCallback() {
            @Override
            public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
                super.onPhyUpdate(gatt, txPhy, rxPhy, status);
            }
        });
    }

    @Test(timeout = 10000)
    public void testPerformanceOnCharacteristicChange() {
        FitbitGatt fitbitGatt = FitbitGatt.getInstance();
        GattClientCallback alternateCB = new GattClientCallback();

        // when register multiple listeners
        for (int i = 0; i < 1000; i++) {
            GattClientListener listener = getNewListener();
            fitbitGatt.getClientCallback().addListener(listener);
            alternateCB.addListener(listener);
        }
        // call on Characteristic Change
        for (int i = 0; i < 50; i++) {
            fitbitGatt.getClientCallback().onCharacteristicChanged(gatt, null);
            Log.i("Test", "onCharacteristicChanged");
            alternateCB.onCharacteristicChanged(gatt, null);
            Log.i("Test", "onAltCharacteristicChanged");
        }
        // will fail if it takes longer than 10 seconds
    }

    private GattClientListener getNewListener() {
        return new GattClientListener() {
            @Nullable
            @Override
            public FitbitBluetoothDevice getDevice() {
                return device;
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
                Log.i("Test1", "charChanged");
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
    }

}
