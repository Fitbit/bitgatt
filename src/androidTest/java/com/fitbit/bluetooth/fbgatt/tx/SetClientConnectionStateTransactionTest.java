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
import com.fitbit.bluetooth.fbgatt.util.NoOpGattCallback;

import android.content.Context;
import android.os.ParcelUuid;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;

import static org.junit.Assert.*;

public class SetClientConnectionStateTransactionTest {

    private final String MOCK_ADDRESS = "02:00:00:00:00:00";
    private GattConnection conn;


    @Before
    public void before() {
        Context mockContext = InstrumentationRegistry.getInstrumentation().getContext();
        List<ParcelUuid> services = new ArrayList<>();
        services.add(new ParcelUuid(UUID.fromString("adabfb00-6e7d-4601-bda2-bffaa68956ba")));
        CountDownLatch cd = new CountDownLatch(2);
        NoOpGattCallback cb = new NoOpGattCallback() {
            @Override
            public void onGattClientStarted() {
                super.onGattClientStarted();
                FitbitBluetoothDevice device = new FitbitBluetoothDevice(MOCK_ADDRESS, "Stupid");
                conn = new GattConnection(device, mockContext.getMainLooper());
                conn.setMockMode(true);
                conn.setState(GattState.CONNECTED);
                // idempotent, can't put the same connection into the map more than once
                FitbitGatt.getInstance().putConnectionIntoDevices(device, conn);
                cd.countDown();
            }
        };
        FitbitGatt.getInstance().registerGattEventListener(cb);
        FitbitGatt.getInstance().startGattClient(mockContext);
        FitbitGatt.getInstance().setScanServiceUuidFilters(services);
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
        SetClientConnectionStateTransaction setClientConnectionStateTransaction =
            new SetClientConnectionStateTransaction(conn,
                GattState.GATT_CONNECTION_STATE_SET_SUCCESSFULLY,
                GattState.FAILURE_DISCONNECTING);
        CountDownLatch cdl = new CountDownLatch(1);
        conn.runTx(setClientConnectionStateTransaction, result -> {
            txResult[0] = result;
            cdl.countDown();
        });
        cdl.await(1, TimeUnit.SECONDS);
        assertEquals(TransactionResult.TransactionResultStatus.SUCCESS, txResult[0].getResultStatus());
        assertEquals(GattState.GATT_CONNECTION_STATE_SET_SUCCESSFULLY, txResult[0].getResultState());
        assertEquals(GattState.FAILURE_DISCONNECTING, conn.getGattState());
    }

    @Test
    public void testSettingConnectionToSuccessState() throws Exception {
        SetClientConnectionStateTransaction setClientConnectionStateTransaction =
            new SetClientConnectionStateTransaction(conn,
                GattState.GATT_CONNECTION_STATE_SET_SUCCESSFULLY,
                GattState.ADD_SERVICE_CHARACTERISTIC_SUCCESS);
        CountDownLatch cdl = new CountDownLatch(1);
        final TransactionResult[] txResult = new TransactionResult[1];
        conn.runTx(setClientConnectionStateTransaction, result -> {
            txResult[0] = result;
            cdl.countDown();
        });
        cdl.await(1, TimeUnit.SECONDS);
        assertEquals(TransactionResult.TransactionResultStatus.SUCCESS, txResult[0].getResultStatus());
        assertEquals(GattState.GATT_CONNECTION_STATE_SET_SUCCESSFULLY, txResult[0].getResultState());
        // this can't be CONNECTED, because upon success the connection state should be ADD_SERVICE_CHARACTERISTIC_SUCCESS
        // with the recent changes sha 99eb54c27009d40b0cbd86cafb7ae8e58f595761
        assertEquals(GattState.ADD_SERVICE_CHARACTERISTIC_SUCCESS, conn.getGattState());
    }

    @Test
    public void testSettingConnectionToCurrentState() throws Exception {
        final TransactionResult[] txResult = new TransactionResult[1];
        SetClientConnectionStateTransaction setClientConnectionStateTransaction =
            new SetClientConnectionStateTransaction(conn,
                GattState.GATT_CONNECTION_STATE_SET_SUCCESSFULLY,
                GattState.CONNECTED);
        CountDownLatch cdl = new CountDownLatch(1);
        conn.runTx(setClientConnectionStateTransaction, getGattTransactionCallback(txResult, cdl));
        cdl.await(1, TimeUnit.SECONDS);
        assertEquals(TransactionResult.TransactionResultStatus.FAILURE, txResult[0].getResultStatus());
        assertEquals(GattState.GATT_CONNECTION_STATE_SET_FAILURE, txResult[0].getResultState());
        assertEquals(GattState.CONNECTED, conn.getGattState());
    }


    @Test
    public void testSetStateDuringDisconnection() throws Exception {
        final TransactionResult[] txResult = new TransactionResult[1];
        SetClientConnectionStateTransaction setClientConnectionStateTransaction =
                new SetClientConnectionStateTransaction(conn,
                        GattState.GATT_CONNECTION_STATE_SET_SUCCESSFULLY,
                        GattState.IDLE);

        CountDownLatch cdl = new CountDownLatch(1);
        conn.setState(GattState.DISCONNECTING);
        conn.runTx(setClientConnectionStateTransaction, getGattTransactionCallback(txResult, cdl));

        cdl.await(1, TimeUnit.SECONDS);
        assertEquals(TransactionResult.TransactionResultStatus.FAILURE, txResult[0].getResultStatus());
        assertEquals(GattState.GATT_CONNECTION_STATE_SET_FAILURE, txResult[0].getResultState());
        assertEquals(GattState.DISCONNECTING, conn.getGattState());
    }

    @Test
    public void testSetStateDuringInvalidTransition() throws Exception {


        final List<GattState> notAllowedTransitions = Collections.unmodifiableList(
                Arrays.asList(GattState.DISCONNECTED,
                        GattState.DISCONNECTING,
                        GattState.FAILURE_CONNECTING,
                        GattState.CLOSED)
        );

        for(GattState state: notAllowedTransitions) {
            final TransactionResult[] txResult = new TransactionResult[1];
            SetClientConnectionStateTransaction setClientConnectionStateTransaction = new SetClientConnectionStateTransaction(conn,
                            GattState.GATT_CONNECTION_STATE_SET_SUCCESSFULLY,
                            GattState.IDLE);
            CountDownLatch cdl = new CountDownLatch(1);
            conn.setState(state);
            conn.runTx(setClientConnectionStateTransaction, getGattTransactionCallback(txResult, cdl));
            cdl.await(1, TimeUnit.SECONDS);

            assertEquals("Should not be able to set idle while in state: "+state,TransactionResult.TransactionResultStatus.FAILURE, txResult[0].getResultStatus());
            assertEquals(GattState.GATT_CONNECTION_STATE_SET_FAILURE, txResult[0].getResultState());
            assertEquals(state, conn.getGattState());
        }


    }


    @NonNull
    private GattTransactionCallback getGattTransactionCallback(TransactionResult[] txResult, CountDownLatch cdl) {
        return result -> {
            txResult[0] = result;
            cdl.countDown();
        };
    }
}
