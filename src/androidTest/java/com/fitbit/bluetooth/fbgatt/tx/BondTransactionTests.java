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
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.util.NoOpGattCallback;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
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

/**
 * A delightful class, filled with stimulating code and whimsical
 * fancy for your reading pleasure
 * <p>
 * Created by iowens on 8/30/18.
 */
public class BondTransactionTests {

    private static final String MOCK_ADDRESS = "02:00:00:00:00:00";
    private Context mockContext;
    private List<ParcelUuid> services;
    private GattConnection conn;
    private Handler delayedHandler;

    public BondTransactionTests() {
        delayedHandler = new Handler(Looper.getMainLooper());
    }


    @Before
    public void before() {
        this.mockContext = InstrumentationRegistry.getInstrumentation().getContext();
        services = new ArrayList<>();
        services.add(new ParcelUuid(UUID.fromString("adabfb00-6e7d-4601-bda2-bffaa68956ba")));
        CountDownLatch cd = new CountDownLatch(1);
        NoOpGattCallback cb = new NoOpGattCallback() {

            @Override
            public void onGattClientStarted() {
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
        FitbitGatt.getInstance().initializeScanner(mockContext);
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
    public void testBondTransaction() throws InterruptedException {
        CountDownLatch cdl = new CountDownLatch(1);
        CreateBondTransaction bondTx = new CreateBondTransaction(conn, GattState.CREATE_BOND_SUCCESS);
        final TransactionResult.TransactionResultStatus[] status = new TransactionResult.TransactionResultStatus[1];
        conn.runTx(bondTx, result -> {
            status[0] = result.getResultStatus();
            cdl.countDown();
        });
        delayedHandler.postDelayed(bondTx::bondSuccess, 1100);
        try {
            cdl.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timeout during test");
        }
        assertEquals("Bond success!", TransactionResult.TransactionResultStatus.SUCCESS, status[0]);
    }

    @Test
    public void testBondDisconnectTransaction() throws InterruptedException {
        CountDownLatch cdl = new CountDownLatch(1);
        CreateBondTransaction bondTx = new CreateBondTransaction(conn, GattState.CREATE_BOND_SUCCESS);
        final TransactionResult.TransactionResultStatus[] status = new TransactionResult.TransactionResultStatus[1];
        conn.runTx(bondTx, result -> {
            status[0] = result.getResultStatus();
            cdl.countDown();
        });
        delayedHandler.postDelayed(bondTx::bondSuccess, 1100);
        delayedHandler.postDelayed(() -> conn.simulateDisconnect(), 1000);
        try {
            cdl.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timeout during test");
        }
        assertEquals("Bond success!", TransactionResult.TransactionResultStatus.SUCCESS, status[0]);
    }
}
