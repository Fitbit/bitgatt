
/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.tx.AddGattServerServiceTransaction;
import com.fitbit.bluetooth.fbgatt.tx.ReadGattServerCharacteristicDescriptorValueTransaction;
import com.fitbit.bluetooth.fbgatt.tx.ReadGattServerCharacteristicValueTransaction;
import com.fitbit.bluetooth.fbgatt.tx.mocks.MockNoOpTransaction;
import com.fitbit.bluetooth.fbgatt.tx.mocks.MockServerNoOpTransaction;
import com.fitbit.bluetooth.fbgatt.tx.mocks.NotifyGattServerCharacteristicMockTransaction;
import com.fitbit.bluetooth.fbgatt.util.BluetoothUtils;
import com.fitbit.bluetooth.fbgatt.util.NoOpGattCallback;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.test.platform.app.InstrumentationRegistry;
import timber.log.Timber;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.junit.Assert.*;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GattServerTests {

    private static final String MOCK_ADDRESS = "02:00:00:00:00:00";
    private static Context mockContext;
    private static BluetoothGattService main;
    private static BluetoothGattService liveData;
    private static BluetoothGattService dncs;
    private static BluetoothGattCharacteristic cpChar;

    @BeforeClass
    public static void before() {
        Timber.plant(new Timber.DebugTree());
        mockContext = InstrumentationRegistry.getInstrumentation().getContext();
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
    @Ignore
    //we need a diffrent api for services added on start
    public void initWithServices() throws InterruptedException {
        ArrayList<BluetoothGattService> services = new ArrayList<>();
        services.add(main);
        services.add(liveData);
        services.add(dncs);
        CountDownLatch cdl = new CountDownLatch(1);
        FitbitGatt gatt = FitbitGatt.getInstance();
        gatt.registerGattEventListener(new NoOpGattCallback() {
            @Override
            public void onGattServerStarted(GattServerConnection serverConnection) {
                super.onGattServerStarted(serverConnection);
                Timber.v("all services up");
                assertNotNull(serverConnection.getServer().getService(main.getUuid()));
                assertNotNull(serverConnection.getServer().getService(liveData.getUuid()));
                assertNotNull(serverConnection.getServer().getService(dncs.getUuid()));
                serverConnection.setState(GattState.IDLE);
                cdl.countDown();
            }
        });
        gatt.startGattServerWithServices(mockContext, services);
        cdl.await(1, TimeUnit.SECONDS);
    }

    @After
    public void afterTest() {
        FitbitGatt.getInstance().shutdown();
        FitbitGatt.setInstance(null);
    }

    @Test
    public void readLocalGattServerCharacteristic() throws InterruptedException {
        CountDownLatch cdl = new CountDownLatch(1);
        NoOpGattCallback cb = new NoOpGattCallback() {
            @Override
            public void onGattServerStarted(GattServerConnection serverConnection) {
                super.onGattServerStarted(serverConnection);
                serverConnection.setState(GattState.IDLE);
                cpChar.setValue(new byte[]{27, 29});
                ReadGattServerCharacteristicValueTransaction rgscvt = new ReadGattServerCharacteristicValueTransaction(serverConnection, GattState.READ_CHARACTERISTIC_SUCCESS, dncs, cpChar);

                serverConnection.runTx(rgscvt, result -> {
                    assertEquals(TransactionResult.TransactionResultStatus.SUCCESS, result.resultStatus);
                    cdl.countDown();
                });
            }
        };
        FitbitGatt.getInstance().registerGattEventListener(cb);
        FitbitGatt.getInstance().startGattServer(mockContext);

        cdl.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void readLocalGattServerDescriptor() throws InterruptedException {
        TransactionResult.TransactionResultStatus[] results = new TransactionResult.TransactionResultStatus[1];
        CountDownLatch cdl = new CountDownLatch(1);
        NoOpGattCallback cb = new NoOpGattCallback() {
            @Override
            public void onGattServerStarted(GattServerConnection serverConnection) {
                super.onGattServerStarted(serverConnection);
                BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"), BluetoothGattDescriptor.PERMISSION_READ);
                cpChar.addDescriptor(descriptor);
                CountDownLatch cdl = new CountDownLatch(1);
                descriptor.setValue(new byte[]{82, 19});
                ReadGattServerCharacteristicDescriptorValueTransaction rgscvt = new ReadGattServerCharacteristicDescriptorValueTransaction(serverConnection, GattState.READ_CHARACTERISTIC_SUCCESS, dncs, cpChar, descriptor);
                serverConnection.runTx(rgscvt, result -> {
                    results[0] = result.resultStatus;
                    cdl.countDown();
                });
            }
        };
        FitbitGatt.getInstance().registerGattEventListener(cb);
        FitbitGatt.getInstance().startGattServer(mockContext);
        cdl.await(1, TimeUnit.SECONDS);
        assertEquals(TransactionResult.TransactionResultStatus.SUCCESS, results[0]);
    }

    @Test
    public void notifyGattServerWithNullPointerException() throws InterruptedException {
        CountDownLatch cdl = new CountDownLatch(1);
        TransactionResult.TransactionResultStatus[] results = new TransactionResult.TransactionResultStatus[1];
        FitbitGatt.FitbitGattCallback callback = new NoOpGattCallback() {
            @Override
            public void onGattServerStarted(GattServerConnection serverConnection) {
                super.onGattServerStarted(serverConnection);
                FitbitBluetoothDevice fbtDevice = new FitbitBluetoothDevice(MOCK_ADDRESS, "foobar");
                BluetoothGattCharacteristic notifChar = new BluetoothGattCharacteristic(UUID.fromString("16BCFD02-253F-C348-E831-0DB3E334D580"),
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED | BluetoothGattCharacteristic.PERMISSION_WRITE);
                NotifyGattServerCharacteristicMockTransaction notifyTx = new NotifyGattServerCharacteristicMockTransaction(serverConnection, fbtDevice, GattState.NOTIFY_CHARACTERISTIC_SUCCESS, notifChar, true, true);
                notifyTx.setShouldThrow(true);
                serverConnection.runTx(notifyTx, result -> {
                    results[0] = result.resultStatus;
                    cdl.countDown();
                });
            }
        };
        FitbitGatt.getInstance().registerGattEventListener(callback);
        FitbitGatt.getInstance().startGattServer(mockContext);

        cdl.await(1, TimeUnit.SECONDS);
        assertEquals(TransactionResult.TransactionResultStatus.FAILURE, results[0]);
        assertEquals(GattState.IDLE, FitbitGatt.getInstance().getServer().getGattState());
    }

    @Test
    public void testTransactionDelay() throws InterruptedException {
        final long[] txStart = new long[1];
        CountDownLatch cdl = new CountDownLatch(1);
        TransactionResult.TransactionResultStatus[] results = new TransactionResult.TransactionResultStatus[1];
        FitbitGatt.FitbitGattCallback callback = new NoOpGattCallback() {
            @Override
            public void onGattServerStarted(GattServerConnection serverConnection) {
                super.onGattServerStarted(serverConnection);
                MockServerNoOpTransaction firstBlockingTx = new MockServerNoOpTransaction(serverConnection, GattState.IDLE, 200);
                serverConnection.setIntraTransactionDelay(200);
                txStart[0] = System.currentTimeMillis();
                serverConnection.runTx(firstBlockingTx, result -> {
                    results[0] = result.resultStatus;
                    cdl.countDown();
                });

            }
        };
        FitbitGatt.getInstance().registerGattEventListener(callback);
        FitbitGatt.getInstance().startGattServer(mockContext);
        cdl.await(4, TimeUnit.SECONDS);
        long txStop = System.currentTimeMillis();
        long txTotal = txStop - txStart[0];
        Timber.i("Total time to unlatch, %dms", txTotal);
        // if this takes less than the tx 200 ms plus the delay then it didn't happen right
        assertTrue("Should take greater than or equal to 400 ms", txTotal >= 400);
        assertEquals(TransactionResult.TransactionResultStatus.SUCCESS, results[0]);
    }

    @Test
    public void btOffOnWillClearServicesAndThatGattServerIfStartedWillRetunrAfterToggle() throws InterruptedException {
        // should do nothing if already started
        final CountDownLatch cdl = new CountDownLatch(2);
        BluetoothAdapter adapter = new BluetoothUtils().getBluetoothAdapter(mockContext);
        assertNotNull("adapter is null", adapter);
        AtomicBoolean isFirst = new AtomicBoolean(true);
        FitbitGatt.FitbitGattCallback cb = new NoOpGattCallback() {
            @Override
            public void onGattServerStarted(GattServerConnection serverConnection) {
                super.onGattServerStarted(serverConnection);
                if (isFirst.get()) {
                    AddGattServerServiceTransaction transaction = new AddGattServerServiceTransaction(serverConnection, GattState.ADD_SERVICE_SUCCESS, liveData);
                    serverConnection.runTx(transaction, result -> {
                        assertEquals(result.resultStatus, TransactionResult.TransactionResultStatus.SUCCESS);
                        adapter.disable();
                        isFirst.set(false);
                        cdl.countDown();
                    });
                } else {
                    BluetoothGattServer server = FitbitGatt.getInstance().getServer().getServer();
                    List<BluetoothGattService> services = server.getServices();
                    Assert.assertTrue(services.isEmpty());
                    cdl.countDown();
                }
            }

            @Override
            public void onBluetoothOff() {
                super.onBluetoothOff();
                adapter.enable();
            }

        };
        FitbitGatt.getInstance().registerGattEventListener(cb);
        FitbitGatt.getInstance().startGattServer(mockContext);
        cdl.await(10, TimeUnit.SECONDS);
        if (cdl.getCount() != 0) {
            fail(String.format("Not all countdowns have been executed.Not Executed %d", cdl.getCount()));
        }
    }

    @Test
    public void btOffOnWillClearServicesAndThatGattServerIsStillUsable() throws InterruptedException {
        // should do nothing if already started
        final CountDownLatch cdl = new CountDownLatch(2);
        BluetoothAdapter adapter = new BluetoothUtils().getBluetoothAdapter(mockContext);
        assertNotNull("adapter is null", adapter);
        AtomicBoolean isFirst = new AtomicBoolean(true);
        FitbitGatt.FitbitGattCallback cb = new NoOpGattCallback() {
            @Override
            public void onGattServerStarted(GattServerConnection serverConnection) {
                super.onGattServerStarted(serverConnection);
                if (isFirst.get()) {
                    AddGattServerServiceTransaction transaction = new AddGattServerServiceTransaction(serverConnection, GattState.ADD_SERVICE_SUCCESS, liveData);
                    serverConnection.runTx(transaction, result -> {
                        assertEquals(result.resultStatus, TransactionResult.TransactionResultStatus.SUCCESS);
                        adapter.disable();
                        isFirst.set(false);
                        cdl.countDown();
                    });
                } else {
                    BluetoothGattServer server = FitbitGatt.getInstance().getServer().getServer();
                    List<BluetoothGattService> services = server.getServices();
                    Assert.assertTrue(services.isEmpty());
                    AddGattServerServiceTransaction transaction = new AddGattServerServiceTransaction(serverConnection, GattState.ADD_SERVICE_SUCCESS, liveData);
                    assertNull(server.getService(liveData.getUuid()));
                    serverConnection.runTx(transaction, result -> {
                        assertEquals(result.resultStatus, TransactionResult.TransactionResultStatus.SUCCESS);
                        assertNotNull(server.getService(liveData.getUuid()));
                        cdl.countDown();
                    });
                }
            }

            @Override
            public void onBluetoothOff() {
                super.onBluetoothOff();
                adapter.enable();
            }

        };
        FitbitGatt.getInstance().registerGattEventListener(cb);
        FitbitGatt.getInstance().startGattServer(mockContext);
        cdl.await(10, TimeUnit.SECONDS);
        if (cdl.getCount() != 0) {
            fail(String.format("Not all countdowns have been executed.Not Executed %d", cdl.getCount()));
        }
    }


    @Test
    public void btOffOnWillClearServicesAndThatGattServerIfStartedWillRetunrAfterToggleMultipleTimesInQuickSuccession() throws InterruptedException {
        // should do nothing if already started
        final CountDownLatch cdl = new CountDownLatch(2);
        BluetoothAdapter adapter = new BluetoothUtils().getBluetoothAdapter(mockContext);
        assertNotNull("adapter is null", adapter);
        AtomicBoolean isFirst = new AtomicBoolean(true);
        AtomicInteger countTest = new AtomicInteger(1);
        FitbitGatt.FitbitGattCallback cb = new NoOpGattCallback() {
            @Override
            public void onGattServerStarted(GattServerConnection serverConnection) {
                super.onGattServerStarted(serverConnection);
                if (isFirst.get()) {
                    AddGattServerServiceTransaction transaction = new AddGattServerServiceTransaction(serverConnection, GattState.ADD_SERVICE_SUCCESS, liveData);
                    serverConnection.runTx(transaction, result -> {
                        assertEquals(result.resultStatus, TransactionResult.TransactionResultStatus.SUCCESS);
                        countTest.incrementAndGet();
                        adapter.disable();
                        cdl.countDown();
                        isFirst.set(false);
                    });
                } else if (countTest.get() >= 20) {
                    BluetoothGattServer server = FitbitGatt.getInstance().getServer().getServer();
                    List<BluetoothGattService> services = server.getServices();
                    Assert.assertTrue(services.isEmpty());
                    cdl.countDown();
                }
            }

            @Override
            public void onBluetoothOn() {
                super.onBluetoothOn();
                if (countTest.getAndIncrement() > 0 && countTest.getAndIncrement() <= 20) {
                    adapter.disable();
                }
            }

            @Override
            public void onBluetoothOff() {
                super.onBluetoothOff();
                adapter.enable();
            }

        };

        FitbitGatt.getInstance().registerGattEventListener(cb);
        FitbitGatt.getInstance().startGattServer(mockContext);
        cdl.await(60, TimeUnit.SECONDS);
        Thread.sleep(2000);
        adapter.enable();
        if (cdl.getCount() != 0) {
            fail(String.format("Not all countdowns have been executed.Not Executed %d", cdl.getCount()));
        }
    }
}
