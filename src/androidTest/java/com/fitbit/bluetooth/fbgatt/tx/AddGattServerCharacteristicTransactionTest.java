/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx;

import com.fitbit.bluetooth.fbgatt.FitbitBluetoothDevice;
import com.fitbit.bluetooth.fbgatt.FitbitGatt;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.TransactionResult;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddGattServerCharacteristicTransactionTest {
    private static final String MOCK_ADDRESS = "02:00:00:00:00:00";
    private static Context mockContext;
    private static List<ParcelUuid> services;
    private static GattConnection conn;
    private static BluetoothGattCharacteristic gattCharacteristic = new BluetoothGattCharacteristic(UUID.randomUUID(),
            BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
    private static BluetoothGattService service;


    @BeforeClass
    public static void beforeClass(){
        mockContext = InstrumentationRegistry.getContext();
        services= new ArrayList<>();
        services.add(new ParcelUuid(UUID.fromString("adabfb00-6e7d-4601-bda2-bffaa68956ba")));
        FitbitGatt.getInstance().start(mockContext);
        FitbitGatt.getInstance().setScanServiceUuidFilters(services);
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(MOCK_ADDRESS, "Stupid");
        conn = new GattConnection(device, InstrumentationRegistry.getTargetContext().getMainLooper());
        conn.setMockMode(true);
        conn.setState(GattState.CONNECTED);
        // idempotent, can't put the same connection into the map more than once
        FitbitGatt.getInstance().putConnectionIntoDevices(device, conn);
        service = new BluetoothGattService(services.get(0).getUuid(), BluetoothGattService.SERVICE_TYPE_PRIMARY);
        service.addCharacteristic(gattCharacteristic);
        FitbitGatt.getInstance().getServer().getServer().addService(service);
    }

    @Test
    public void testAddingCharacteristicWhenSameCharacteristicAlreadyExists(){
        AddGattServerServiceCharacteristicTransaction addCharacteristicTransaction =
                new AddGattServerServiceCharacteristicTransaction(FitbitGatt.getInstance().getServer(),
                        GattState.ADD_SERVICE_CHARACTERISTIC_SUCCESS,
                        service, gattCharacteristic);
        FitbitGatt.getInstance().getServer().runTx(addCharacteristicTransaction, new GattTransactionCallback() {
            @Override
            public void onTransactionComplete(@NonNull TransactionResult result) {
                Assert.assertEquals(TransactionResult.TransactionResultStatus.FAILURE, result.getResultStatus());
            }
        });
    }
}
