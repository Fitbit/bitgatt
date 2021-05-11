/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx;

import com.fitbit.bluetooth.fbgatt.FitbitGatt;
import com.fitbit.bluetooth.fbgatt.GattServerConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.util.NoOpGattCallback;

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

public class SetServerConnectionStateTransactionTest {

    private static final String MOCK_ADDRESS = "02:00:00:00:00:00";


    @Before
    public void before() {
        Context mockContext = InstrumentationRegistry.getInstrumentation().getContext();
        List<ParcelUuid> services = new ArrayList<>();
        services.add(new ParcelUuid(UUID.fromString("adabfb00-6e7d-4601-bda2-bffaa68956ba")));
        CountDownLatch cd = new CountDownLatch(1);
        NoOpGattCallback cb = new NoOpGattCallback() {
            @Override
            public void onGattServerStarted(GattServerConnection serverConnection) {
                super.onGattServerStarted(serverConnection);
                BluetoothGattService service = new BluetoothGattService(services.get(0).getUuid(), BluetoothGattService.SERVICE_TYPE_PRIMARY);
                serverConnection.getServer().addService(service);
                cd.countDown();
            }
        };
        FitbitGatt.getInstance().registerGattEventListener(cb);
        FitbitGatt.getInstance().startGattServer(mockContext);
        try {
            cd.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timeout during test setup");
        }
    }

    @After
    public void cleanUpState() {
        FitbitGatt.getInstance().shutdown();
        FitbitGatt.setInstance(null);
    }

    @Test
    public void testSettingConnectionToFailureState() throws Exception {
        final TransactionResult[] txResult = new TransactionResult[1];
        SetServerConnectionStateTransaction setServerConnectionStateTransaction =
            new SetServerConnectionStateTransaction(FitbitGatt.getInstance().getServer(),
                GattState.GATT_CONNECTION_STATE_SET_SUCCESSFULLY,
                GattState.FAILURE_DISCONNECTING);
        CountDownLatch cdl = new CountDownLatch(1);
        FitbitGatt.getInstance().getServer().runTx(setServerConnectionStateTransaction, getGattTransactionCallback(txResult, cdl));
        cdl.await(1, TimeUnit.SECONDS);
        assertEquals(TransactionResult.TransactionResultStatus.SUCCESS, txResult[0].getResultStatus());
        assertEquals(GattState.GATT_CONNECTION_STATE_SET_SUCCESSFULLY, txResult[0].getResultState());
        assertEquals(GattState.FAILURE_DISCONNECTING, FitbitGatt.getInstance().getServer().getGattState());
    }

    @Test
    public void testSettingConnectionToSuccessState() throws Exception {
        final TransactionResult[] txResult = new TransactionResult[1];
        SetServerConnectionStateTransaction setServerConnectionStateTransaction =
            new SetServerConnectionStateTransaction(FitbitGatt.getInstance().getServer(),
                GattState.GATT_CONNECTION_STATE_SET_SUCCESSFULLY,
                GattState.ADD_SERVICE_CHARACTERISTIC_SUCCESS);
        CountDownLatch cdl = new CountDownLatch(1);
        FitbitGatt.getInstance().getServer().runTx(setServerConnectionStateTransaction, getGattTransactionCallback(txResult, cdl));
        cdl.await(1, TimeUnit.SECONDS);
        assertEquals(TransactionResult.TransactionResultStatus.SUCCESS, txResult[0].getResultStatus());
        assertEquals(GattState.GATT_CONNECTION_STATE_SET_SUCCESSFULLY, txResult[0].getResultState());
        assertEquals(GattState.ADD_SERVICE_CHARACTERISTIC_SUCCESS, FitbitGatt.getInstance().getServer().getGattState());
    }

    @Test
    public void testSettingConnectionToCurrentState() throws Exception {
        final TransactionResult[] txResult = new TransactionResult[1];
        FitbitGatt.getInstance().getServer().setState(GattState.CONNECTED);
        SetServerConnectionStateTransaction setServerConnectionStateTransaction =
            new SetServerConnectionStateTransaction(FitbitGatt.getInstance().getServer(),
                GattState.GATT_CONNECTION_STATE_SET_SUCCESSFULLY,
                GattState.CONNECTED);
        CountDownLatch cdl = new CountDownLatch(1);
        FitbitGatt.getInstance().getServer().runTx(setServerConnectionStateTransaction, getGattTransactionCallback(txResult, cdl));
        cdl.await(1, TimeUnit.SECONDS);
        assertEquals(TransactionResult.TransactionResultStatus.FAILURE, txResult[0].getResultStatus());
        assertEquals(GattState.GATT_CONNECTION_STATE_SET_FAILURE, txResult[0].getResultState());
        assertEquals(GattState.CONNECTED, FitbitGatt.getInstance().getServer().getGattState());
    }

    @NonNull
    private GattTransactionCallback getGattTransactionCallback(TransactionResult[] txResult, CountDownLatch cdl) {
        return result -> {
            txResult[0] = result;
            cdl.countDown();
        };
    }
}
