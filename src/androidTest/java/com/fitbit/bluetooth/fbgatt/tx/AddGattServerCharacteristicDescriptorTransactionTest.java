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
import com.fitbit.bluetooth.fbgatt.GattServerConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.util.NoOpGattCallback;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.ParcelUuid;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;

import static org.junit.Assert.*;

public class AddGattServerCharacteristicDescriptorTransactionTest {
    private static final String MOCK_ADDRESS = "02:00:00:00:00:00";
    private static Context mockContext;
    private static List<ParcelUuid> services;
    private static GattConnection conn;
    private static BluetoothGattCharacteristic gattCharacteristic = new BluetoothGattCharacteristic(UUID.randomUUID(),
        BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
        BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
    private static BluetoothGattService service;


    @Before
    public void before() throws InterruptedException {
        mockContext = InstrumentationRegistry.getInstrumentation().getContext();
        services = new ArrayList<>();
        services.add(new ParcelUuid(UUID.fromString("adabfb00-6e7d-4601-bda2-bffaa68956ba")));
        CountDownLatch cd = new CountDownLatch(1);
        NoOpGattCallback cb = new NoOpGattCallback() {

            @Override
            public void onGattServerStarted(GattServerConnection serverConnection) {
                super.onGattServerStarted(serverConnection);
                service = new BluetoothGattService(services.get(0).getUuid(), BluetoothGattService.SERVICE_TYPE_PRIMARY);
                service.addCharacteristic(gattCharacteristic);
                serverConnection.getServer().addService(service);
                cd.countDown();
            }
        };
        FitbitGatt.getInstance().registerGattEventListener(cb);
        FitbitGatt.getInstance().startGattServer(mockContext);
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(MOCK_ADDRESS, "Stupid");

        conn = new GattConnection(device, InstrumentationRegistry.getInstrumentation().getTargetContext().getMainLooper());
        conn.setMockMode(true);
        conn.setState(GattState.CONNECTED);
        // idempotent, can't put the same connection into the map more than once
        FitbitGatt.getInstance().putConnectionIntoDevices(device, conn);
        try {
            cd.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timeout during test setup");
        }
    }

    @After
    public void after() {
        FitbitGatt.getInstance().shutdown();
        FitbitGatt.setInstance(null);
    }

    @Test
    public void testAddingDescriptorWhenSameDescriptorAlreadyExists() throws InterruptedException {
        CountDownLatch cd = new CountDownLatch(1);
        UUID descriptorUuid = UUID.randomUUID();
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(descriptorUuid, BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        gattCharacteristic.addDescriptor(descriptor);
        AddGattServerServiceCharacteristicDescriptorTransaction addDescriptor =
            new AddGattServerServiceCharacteristicDescriptorTransaction(FitbitGatt.getInstance().getServer(),
                GattState.ADD_SERVICE_CHARACTERISTIC_DESCRIPTOR_SUCCESS,
                service, gattCharacteristic, descriptor);
        final TransactionResult.TransactionResultStatus[] status = new TransactionResult.TransactionResultStatus[1];
        FitbitGatt.getInstance().getServer().runTx(addDescriptor, new GattTransactionCallback() {
            @Override
            public void onTransactionComplete(@NonNull TransactionResult result) {
                status[0] = result.getResultStatus();
                cd.countDown();
            }
        });
        try {
            cd.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timeout during test setup");
        }
        assertEquals(TransactionResult.TransactionResultStatus.FAILURE, status[0]);
    }


}
