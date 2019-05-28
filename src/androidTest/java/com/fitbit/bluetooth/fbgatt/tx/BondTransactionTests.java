/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.support.test.InstrumentationRegistry;

import com.fitbit.bluetooth.fbgatt.FitbitBluetoothDevice;
import com.fitbit.bluetooth.fbgatt.FitbitGatt;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.TransactionResult;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A delightful class, filled with stimulating code and whimsical
 * fancy for your reading pleasure
 *
 * Created by iowens on 8/30/18.
 */
public class BondTransactionTests {

    private static final String MOCK_ADDRESS = "02:00:00:00:00:00";
    private Context mockContext;
    private List<ParcelUuid> services;
    private GattConnection conn;
    private Handler delayedHandler;

    public BondTransactionTests(){
        delayedHandler = new Handler(Looper.getMainLooper());
    }


    @Before
    public void before(){
        this.mockContext = InstrumentationRegistry.getContext();
        services= new ArrayList<>();
        services.add(new ParcelUuid(UUID.fromString("adabfb00-6e7d-4601-bda2-bffaa68956ba")));
        FitbitGatt.getInstance().start(this.mockContext);
        FitbitGatt.getInstance().setScanServiceUuidFilters(services);
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(MOCK_ADDRESS, "Stupid");
        conn = new GattConnection(device, mockContext.getMainLooper());
        conn.setMockMode(true);
        conn.setState(GattState.CONNECTED);
        // idempotent, can't put the same connection into the map more than once
        FitbitGatt.getInstance().putConnectionIntoDevices(device, conn);
    }

    @Test
    public void testBondTransaction() throws InterruptedException {
        CountDownLatch cdl = new CountDownLatch(1);
        CreateBondTransaction bondTx = new CreateBondTransaction(conn, GattState.CREATE_BOND_SUCCESS);
        conn.runTx(bondTx, result -> {
            Assert.assertTrue("Bond success!",
                    result.getResultStatus().equals(TransactionResult.TransactionResultStatus.SUCCESS));
            cdl.countDown();
        });
        delayedHandler.postDelayed(bondTx::bondSuccess, 1100);
        cdl.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void testBondDisconnectTransaction() throws InterruptedException {
        CountDownLatch cdl = new CountDownLatch(1);
        CreateBondTransaction bondTx = new CreateBondTransaction(conn, GattState.CREATE_BOND_SUCCESS);
        conn.runTx(bondTx, result -> {
            Assert.assertTrue("Bond success!",
                    result.getResultStatus().equals(TransactionResult.TransactionResultStatus.SUCCESS));
            cdl.countDown();
        });
        delayedHandler.postDelayed(bondTx::bondSuccess, 1100);
        delayedHandler.postDelayed(() -> conn.simulateDisconnect(), 1000);
        cdl.await(1, TimeUnit.SECONDS);
    }
}
