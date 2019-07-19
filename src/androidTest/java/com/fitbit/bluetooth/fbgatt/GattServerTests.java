/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.tx.ReadGattServerCharacteristicDescriptorValueTransaction;
import com.fitbit.bluetooth.fbgatt.tx.ReadGattServerCharacteristicValueTransaction;
import com.fitbit.bluetooth.fbgatt.tx.mocks.MockNoOpTransaction;
import com.fitbit.bluetooth.fbgatt.tx.mocks.NotifyGattServerCharacteristicMockTransaction;
import com.fitbit.bluetooth.fbgatt.util.GattUtils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class GattServerTests {

    private static final String MOCK_ADDRESS = "02:00:00:00:00:00";
    private static Context mockContext;
    private static FitbitGatt gatt = FitbitGatt.getInstance();
    private static BluetoothGattService main;
    private static BluetoothGattService liveData;
    private static BluetoothGattService dncs;
    private static BluetoothGattCharacteristic cpChar;

    @BeforeClass
    public static void before() {
        Timber.plant(new Timber.DebugTree());
        gatt.isStarted.set(false);
        mockContext = InstrumentationRegistry.getContext();
        main = new BluetoothGattService(UUID.fromString("adabfb00-6e7d-4601-bda2-bffaa68956ba"), BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic txChar = new BluetoothGattCharacteristic(UUID.fromString("ADABFB01-6E7D-4601-BDA2-BFFAA68956BA"),
                BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED);
        main.addCharacteristic(txChar);
        BluetoothGattCharacteristic rxChar = new BluetoothGattCharacteristic(UUID.fromString("ADABFB02-6E7D-4601-BDA2-BFFAA68956BA"),
                BluetoothGattCharacteristic.PROPERTY_INDICATE | BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
        main.addCharacteristic(rxChar);
        liveData = new BluetoothGattService(UUID.fromString("558dfa00-4fa8-4105-9f02-4eaa93e62980"), BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic ldChar = new BluetoothGattCharacteristic(UUID.fromString("558DFA01-4FA8-4105-9F02-4EAA93E62980"),
                BluetoothGattCharacteristic.PROPERTY_INDICATE | BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
        liveData.addCharacteristic(ldChar);
        dncs = new BluetoothGattService(UUID.fromString("16bcfd00-253f-c348-e831-0db3e334d580"), BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic notifChar = new BluetoothGattCharacteristic(UUID.fromString("16BCFD02-253F-C348-E831-0DB3E334D580"),
                BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED | BluetoothGattCharacteristic.PERMISSION_WRITE);
        dncs.addCharacteristic(notifChar);
        cpChar = new BluetoothGattCharacteristic(UUID.fromString("16BCFD01-253F-C348-E831-0DB3E334D580"),
                BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        dncs.addCharacteristic(cpChar);
        BluetoothGattCharacteristic flsChar = new BluetoothGattCharacteristic(UUID.fromString("16BCFD04-253F-C348-E831-0DB3E334D580"),
                BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        dncs.addCharacteristic(flsChar);
    }

    @Test
    public void initWithServices() throws InterruptedException {
        ArrayList<BluetoothGattService> services = new ArrayList<>();
        services.add(main);
        services.add(liveData);
        services.add(dncs);
        CountDownLatch cdl = new CountDownLatch(1);
        gatt.startWithServices(mockContext, services, new FitbitGatt.FitbitGattCallback() {

            @Override
            public void onBluetoothPeripheralDiscovered(@NonNull GattConnection connection) {

            }

            @Override
            public void onBluetoothPeripheralDisconnected(GattConnection connection) {

            }

            @Override
            public void onFitbitGattReady() {
                Timber.v("all services up");
                Assert.assertTrue(FitbitGatt.getInstance().isStarted());
                gatt.getServer().setState(GattState.IDLE);
                cdl.countDown();
            }

            @Override
            public void onScanStarted() {

            }

            @Override
            public void onScanStopped() {

            }

            @Override
            public void onPendingIntentScanStopped() {

            }

            @Override
            public void onPendingIntentScanStarted() {

            }

            @Override
            public void onBluetoothOff() {

            }

            @Override
            public void onBluetoothOn() {

            }

            @Override
            public void onBluetoothTurningOn() {

            }

            @Override
            public void onBluetoothTurningOff() {

            }
        });
        cdl.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void readLocalGattServerCharacteristic() throws InterruptedException {
        FitbitGatt.getInstance().start(mockContext);
        gatt.getServer().setState(GattState.IDLE);
        cpChar.setValue(new byte[]{27, 29});
        ReadGattServerCharacteristicValueTransaction rgscvt = new ReadGattServerCharacteristicValueTransaction(gatt.getServer(), GattState.READ_CHARACTERISTIC_SUCCESS, dncs, cpChar);
        CountDownLatch cdl = new CountDownLatch(1);
        gatt.getServer().runTx(rgscvt, result -> {
            Assert.assertEquals(TransactionResult.TransactionResultStatus.SUCCESS, result.resultStatus);
            cdl.countDown();
        });
        cdl.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void readLocalGattServerDescriptor() throws InterruptedException {
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"), BluetoothGattDescriptor.PERMISSION_READ);
        cpChar.addDescriptor(descriptor);
        CountDownLatch cdl = new CountDownLatch(1);
        descriptor.setValue(new byte[]{82, 19});
        ReadGattServerCharacteristicDescriptorValueTransaction rgscvt = new ReadGattServerCharacteristicDescriptorValueTransaction(gatt.getServer(), GattState.READ_CHARACTERISTIC_SUCCESS, dncs, cpChar, descriptor);
        gatt.getServer().runTx(rgscvt, result -> {
            Assert.assertEquals(TransactionResult.TransactionResultStatus.SUCCESS, result.resultStatus);
            cdl.countDown();
        });
        cdl.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void notifyGattServerWithNullPointerException() throws InterruptedException {
        GattServerConnection gattConnection = gatt.getServer();
        FitbitBluetoothDevice fbtDevice = new FitbitBluetoothDevice(MOCK_ADDRESS, "foobar");
        BluetoothGattCharacteristic notifChar = new BluetoothGattCharacteristic(UUID.fromString("16BCFD02-253F-C348-E831-0DB3E334D580"),
                BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED | BluetoothGattCharacteristic.PERMISSION_WRITE);
        NotifyGattServerCharacteristicMockTransaction notifyTx = new NotifyGattServerCharacteristicMockTransaction(gattConnection, fbtDevice, GattState.NOTIFY_CHARACTERISTIC_SUCCESS, notifChar, true, true);
        notifyTx.setShouldThrow(true);
        CountDownLatch cdl = new CountDownLatch(1);
        gattConnection.runTx(notifyTx, result -> {
            Assert.assertTrue(result.getResultStatus().equals(TransactionResult.TransactionResultStatus.FAILURE));
            Assert.assertEquals(GattState.IDLE, gattConnection.getGattState());
            cdl.countDown();
        });
        cdl.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void testTransactionDelay() throws InterruptedException {
        GattServerConnection connection = gatt.getServer();
        CountDownLatch cdl = new CountDownLatch(1);
        MockNoOpTransaction firstBlockingTx = new MockNoOpTransaction(connection, GattState.IDLE, 200);
        connection.setIntraTransactionDelay(200);
        long txStart = System.currentTimeMillis();
        connection.runTx(firstBlockingTx, result -> {
            assertTrue(result.resultStatus.equals(TransactionResult.TransactionResultStatus.SUCCESS));
            cdl.countDown();
        });
        cdl.await(4, TimeUnit.SECONDS);
        long txStop = System.currentTimeMillis();
        long txTotal = txStop - txStart;
        Timber.i("Total time to unlatch, %dms", txTotal);
        // if this takes less than the tx 200 ms plus the delay then it didn't happen right
        assertTrue("Should take greater than or equal to 400 ms", txTotal >= 400);
        connection.setIntraTransactionDelay(0);
    }

    @Test
    public void btOffOnWillClearServices() throws InterruptedException {
        // should do nothing if already started
        final CountDownLatch cdl = new CountDownLatch(1);
        BluetoothAdapter adapter = new GattUtils().getBluetoothAdapter(mockContext);
        if(adapter == null) {
            // if adapter is null always pass we are probably running in the simulator
            Assert.assertTrue("adapter is null", true);
            return;
        }
        FitbitGatt.getInstance().start(mockContext);
        FitbitGatt.FitbitGattCallback callback = new FitbitGatt.FitbitGattCallback() {
            @Override
            public void onBluetoothPeripheralDiscovered(GattConnection connection) {

            }

            @Override
            public void onBluetoothPeripheralDisconnected(GattConnection connection) {

            }

            @Override
            public void onFitbitGattReady() {

            }

            @Override
            public void onScanStarted() {

            }

            @Override
            public void onScanStopped() {

            }

            @Override
            public void onPendingIntentScanStopped() {

            }

            @Override
            public void onPendingIntentScanStarted() {

            }

            @Override
            public void onBluetoothOff() {
                // turn bluetooth back on
                new Handler(Looper.getMainLooper()).postDelayed(adapter::enable, 250);
            }

            @Override
            public void onBluetoothOn() {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        BluetoothGattServer server = FitbitGatt.getInstance().getServer().getServer();
                        List<BluetoothGattService> services = server.getServices();
                        Assert.assertTrue(services.isEmpty());
                        cdl.countDown();
                    }
                }, 250);
            }

            @Override
            public void onBluetoothTurningOn() {

            }

            @Override
            public void onBluetoothTurningOff() {

            }
        };
        // let's listen for bt events
        FitbitGatt.getInstance().registerGattEventListener(callback);
        adapter.disable();
        cdl.await(10, TimeUnit.SECONDS);
    }
}
