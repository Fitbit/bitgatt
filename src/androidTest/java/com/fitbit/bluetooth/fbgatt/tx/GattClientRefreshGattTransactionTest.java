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

import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.ParcelUuid;
import androidx.annotation.NonNull;
import android.support.test.InstrumentationRegistry;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GattClientRefreshGattTransactionTest {

    private static final String MOCK_ADDRESS = "02:00:00:00:00:00";
    private static Context mockContext;
    private static List<ParcelUuid> services;
    private static GattConnection conn;
    private static BluetoothGattService service;


    @BeforeClass
    public static void beforeClass(){
        mockContext = InstrumentationRegistry.getContext();
        services= new ArrayList<>();
        services.add(new ParcelUuid(UUID.fromString("adabfb00-6e7d-4601-bda2-bffaa68956ba")));
        FitbitGatt.getInstance().start(mockContext);
        FitbitGatt.getInstance().setScanServiceUuidFilters(services);
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(MOCK_ADDRESS, "Stupid");
        conn = new GattConnection(device, mockContext.getMainLooper());
        conn.setMockMode(true);
        conn.setState(GattState.CONNECTED);
        // idempotent, can't put the same connection into the map more than once
        FitbitGatt.getInstance().putConnectionIntoDevices(device, conn);
        service = new BluetoothGattService(services.get(0).getUuid(), BluetoothGattService.SERVICE_TYPE_PRIMARY);
        FitbitGatt.getInstance().getServer().getServer().addService(service);
    }

    @After
    public void cleanUpState(){
        FitbitGatt.getInstance().getServer().setState(GattState.IDLE);
    }

    @Test
    public void testFailureToCallRefreshGattScenario(){
        // sadly we can only test the failure mode because we have a fake device, we will have to
        // test this in the real world to understand how it behaves
        GattClientRefreshGattTransaction refresh = new GattClientRefreshGattTransaction(conn, GattState.REFRESH_GATT_SUCCESS);
        conn.runTx(refresh, new GattTransactionCallback() {
            @Override
            public void onTransactionComplete(@NonNull TransactionResult result) {
                assertEquals(TransactionResult.TransactionResultStatus.SUCCESS, result.getResultStatus());
                assertEquals(GattState.REFRESH_GATT_FAILURE, result.getResultState());
            }
        });
    }
}
