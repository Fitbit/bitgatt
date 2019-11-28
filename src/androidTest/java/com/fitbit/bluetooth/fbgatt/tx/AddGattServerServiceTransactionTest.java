/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx;

import com.fitbit.bluetooth.fbgatt.FitbitGatt;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattServerConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
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

import androidx.test.platform.app.InstrumentationRegistry;

import static org.junit.Assert.*;

public class AddGattServerServiceTransactionTest {
    private final String MOCK_ADDRESS = "02:00:00:00:00:00";
    private Context mockContext;
    private List<ParcelUuid> services;
    private GattConnection conn;
    private BluetoothGattService service;


    @Before
    public void before() {
        mockContext = InstrumentationRegistry.getInstrumentation().getContext();
        services = new ArrayList<>();
        services.add(new ParcelUuid(UUID.fromString("adabfb00-6e7d-4601-bda2-bffaa68956ba")));
        service = new BluetoothGattService(services.get(0).getUuid(), BluetoothGattService.SERVICE_TYPE_PRIMARY);
        CountDownLatch cd = new CountDownLatch(1);
        NoOpGattCallback cb = new NoOpGattCallback() {

            @Override
            public void onGattServerStarted(GattServerConnection serverConnection) {
                super.onGattServerStarted(serverConnection);
                assertTrue(serverConnection.getServer().addService(service));
                cd.countDown();
            }
        };
        FitbitGatt.getInstance().registerGattEventListener(cb);
        FitbitGatt.getInstance().startGattServer(mockContext);
        try {
            cd.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timeout during test setup");
        }
        if (cd.getCount() != 0) {
            fail("We have not added the service before the test");
        }
    }

    @After
    public void after() {
        FitbitGatt.getInstance().shutdown();
        FitbitGatt.setInstance(null);
    }

    @Test
    public void testAddingServiceWhenSameServiceAlreadyExists() {
        CountDownLatch cd = new CountDownLatch(1);
        final TransactionResult.TransactionResultStatus[] status = new TransactionResult.TransactionResultStatus[1];
        AddGattServerServiceTransaction addServiceTransaction =
            new AddGattServerServiceTransaction(FitbitGatt.getInstance().getServer(),
                GattState.ADD_SERVICE_SUCCESS,
                service);
        FitbitGatt.getInstance().getServer().runTx(addServiceTransaction, result -> {
            status[0] = result.getResultStatus();
            cd.countDown();
        });
        try {
            cd.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timeout during test");
        }
        assertEquals(TransactionResult.TransactionResultStatus.FAILURE, status[0]);
    }
}
