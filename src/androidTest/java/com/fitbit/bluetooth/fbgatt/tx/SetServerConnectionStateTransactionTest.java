/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx;

import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.ParcelUuid;
import android.support.test.InstrumentationRegistry;

import com.fitbit.bluetooth.fbgatt.FitbitBluetoothDevice;
import com.fitbit.bluetooth.fbgatt.FitbitGatt;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.TransactionResult;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class SetServerConnectionStateTransactionTest {

    private static final String MOCK_ADDRESS = "02:00:00:00:00:00";


    @BeforeClass
    public static void beforeClass(){
        Context mockContext = InstrumentationRegistry.getContext();
        List<ParcelUuid> services = new ArrayList<>();
        services.add(new ParcelUuid(UUID.fromString("adabfb00-6e7d-4601-bda2-bffaa68956ba")));
        FitbitGatt.getInstance().start(mockContext);
        FitbitGatt.getInstance().setScanServiceUuidFilters(services);
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(MOCK_ADDRESS, "Stupid");
        GattConnection conn = new GattConnection(device, mockContext.getMainLooper());
        conn.setMockMode(true);
        conn.setState(GattState.CONNECTED);
        // idempotent, can't put the same connection into the map more than once
        FitbitGatt.getInstance().putConnectionIntoDevices(device, conn);
        BluetoothGattService service = new BluetoothGattService(services.get(0).getUuid(), BluetoothGattService.SERVICE_TYPE_PRIMARY);
        FitbitGatt.getInstance().getServer().getServer().addService(service);
    }

    @After
    public void cleanUpState(){
        FitbitGatt.getInstance().getServer().setState(GattState.IDLE);
    }

    @Test
    public void testSettingConnectionToFailureState() throws Exception{
        SetServerConnectionStateTransaction setServerConnectionStateTransaction =
                new SetServerConnectionStateTransaction(FitbitGatt.getInstance().getServer(),
                        GattState.GATT_CONNECTION_STATE_SET_SUCCESSFULLY,
                        GattState.FAILURE_DISCONNECTING);
        CountDownLatch cdl = new CountDownLatch(1);
        FitbitGatt.getInstance().getServer().runTx(setServerConnectionStateTransaction, result -> {
            assertEquals(TransactionResult.TransactionResultStatus.SUCCESS, result.getResultStatus());
            assertEquals(GattState.GATT_CONNECTION_STATE_SET_SUCCESSFULLY, result.getResultState());
            assertEquals(GattState.FAILURE_DISCONNECTING, FitbitGatt.getInstance().getServer().getGattState());
            cdl.countDown();
        });
        cdl.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void testSettingConnectionToSuccessState() throws Exception {
        SetServerConnectionStateTransaction setServerConnectionStateTransaction =
                new SetServerConnectionStateTransaction(FitbitGatt.getInstance().getServer(),
                        GattState.GATT_CONNECTION_STATE_SET_SUCCESSFULLY,
                        GattState.ADD_SERVICE_CHARACTERISTIC_SUCCESS);
        CountDownLatch cdl = new CountDownLatch(1);
        FitbitGatt.getInstance().getServer().runTx(setServerConnectionStateTransaction, result -> {
            assertEquals(TransactionResult.TransactionResultStatus.SUCCESS, result.getResultStatus());
            assertEquals(GattState.GATT_CONNECTION_STATE_SET_SUCCESSFULLY, result.getResultState());
            assertEquals(GattState.ADD_SERVICE_CHARACTERISTIC_SUCCESS, FitbitGatt.getInstance().getServer().getGattState());
            cdl.countDown();
        });
        cdl.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void testSettingConnectionToCurrentState() throws Exception {
        FitbitGatt.getInstance().getServer().setState(GattState.CONNECTED);
        SetServerConnectionStateTransaction setServerConnectionStateTransaction =
                new SetServerConnectionStateTransaction(FitbitGatt.getInstance().getServer(),
                        GattState.GATT_CONNECTION_STATE_SET_SUCCESSFULLY,
                        GattState.CONNECTED);
        CountDownLatch cdl = new CountDownLatch(1);
        FitbitGatt.getInstance().getServer().runTx(setServerConnectionStateTransaction, result -> {
            assertEquals(TransactionResult.TransactionResultStatus.FAILURE, result.getResultStatus());
            assertEquals(GattState.GATT_CONNECTION_STATE_SET_FAILURE, result.getResultState());
            assertEquals(GattState.CONNECTED, FitbitGatt.getInstance().getServer().getGattState());
            cdl.countDown();
        });
        cdl.await(1, TimeUnit.SECONDS);
    }
}
