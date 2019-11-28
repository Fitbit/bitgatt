/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.tx.mocks.BlockingTaskTestMockTransaction;
import com.fitbit.bluetooth.fbgatt.tx.mocks.MockNoOpTransaction;
import com.fitbit.bluetooth.fbgatt.tx.mocks.ReadGattCharacteristicMockTransaction;
import com.fitbit.bluetooth.fbgatt.tx.mocks.TimeoutTestMockTransaction;
import com.fitbit.bluetooth.fbgatt.tx.mocks.WriteGattDescriptorMockTransaction;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import androidx.test.platform.app.InstrumentationRegistry;
import timber.log.Timber;

import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.*;


public class TxConcurrencyTests {

    private static final String MOCK_ADDRESS = "02:00:00:00:00:00";
    private static final long SLOW_TIME_FOR_TX = 500; // time in ms until completion
    private byte[] fakeData;
    static private BluetoothGattCharacteristic characteristic;
    private static final UUID CHAR_NAME = UUID.randomUUID();
    private static final UUID DESCRIPTOR_NAME = UUID.randomUUID();
    static private BluetoothGattDescriptor descriptor;
    static private GattConnection connection;
    private Handler handler;
    private static HandlerThread handlerThread;


    @Before
    public void before() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getContext();
        FitbitGatt.getInstance().startGattClient(appContext);
        // started
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(MOCK_ADDRESS, "fooDevice");
        fakeData = new byte[]{'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j'};
        characteristic = new BluetoothGattCharacteristic(CHAR_NAME, PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);
        descriptor = new BluetoothGattDescriptor(DESCRIPTOR_NAME, BluetoothGattDescriptor.PERMISSION_WRITE);
        connection = FitbitGatt.getInstance().getConnection(device);
        if (connection == null) {
            connection = new GattConnection(device, appContext.getMainLooper());
            FitbitGatt.getInstance().getConnectionMap().put(device, connection);
        }
        connection.setMockMode(true);
        connection.setState(GattState.CONNECTED);
        handler = new Handler(handlerThread.getLooper());
    }

    @BeforeClass
    public static void beforeClass() {
        handlerThread = new HandlerThread("Test Handler Thread");
        handlerThread.start();
        HandlerThread secondThread = new HandlerThread("Second Handler Thread");
        secondThread.start();
    }

    @AfterClass
    public static void after() {
        characteristic = null;
        descriptor = null;
        connection.setState(GattState.DISCONNECTED);
        connection = null;
        handlerThread.quit();
    }

    @Test
    public void twoTxEnqueuedAtTheSameTimeShouldNotGetIllegalStateTest() throws Exception {
        CountDownLatch cdl = new CountDownLatch(2);
        WriteGattDescriptorMockTransaction writeGattDescriptorMockTransaction = new WriteGattDescriptorMockTransaction(connection, GattState.WRITE_DESCRIPTOR_SUCCESS, descriptor, fakeData, false, SLOW_TIME_FOR_TX);
        connection.runTx(writeGattDescriptorMockTransaction, result -> {
            Timber.v("Result from write provided %s", result);
            assertEquals(result.resultStatus, TransactionResult.TransactionResultStatus.SUCCESS);
            assertEquals(result.resultState, GattState.WRITE_DESCRIPTOR_SUCCESS);
            cdl.countDown();
        });
        ReadGattCharacteristicMockTransaction readGattCharacteristicMockTransaction = new ReadGattCharacteristicMockTransaction(connection, GattState.READ_CHARACTERISTIC_SUCCESS, characteristic, fakeData, false);
        connection.runTx(readGattCharacteristicMockTransaction, result -> {
            Timber.v("Result from read provided %s", result);
            assertNotEquals(result.resultStatus, TransactionResult.TransactionResultStatus.INVALID_STATE);
            assertEquals(result.resultState, GattState.READ_CHARACTERISTIC_SUCCESS);
            cdl.countDown();
        });
        cdl.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void txEnqueuedWhileTxRunningShouldNotGetIllegalStateTest() throws InterruptedException {
        CountDownLatch cdl = new CountDownLatch(2);
        WriteGattDescriptorMockTransaction writeGattDescriptorMockTransaction = new WriteGattDescriptorMockTransaction(connection, GattState.WRITE_DESCRIPTOR_SUCCESS, descriptor, fakeData, false, SLOW_TIME_FOR_TX);
        connection.runTx(writeGattDescriptorMockTransaction, result -> {
            Timber.v("Result from write provided %s", result);
            assertEquals(result.resultStatus, TransactionResult.TransactionResultStatus.SUCCESS);
            assertEquals(result.resultState, GattState.WRITE_DESCRIPTOR_SUCCESS);
            cdl.countDown();
        });
        handler.postDelayed(() -> {
            ReadGattCharacteristicMockTransaction readGattCharacteristicMockTransaction = new ReadGattCharacteristicMockTransaction(connection, GattState.READ_CHARACTERISTIC_SUCCESS, characteristic, fakeData, false);
            connection.runTx(readGattCharacteristicMockTransaction, result -> {
                Timber.v("Result from read provided %s", result);
                assertNotEquals(result.resultStatus, TransactionResult.TransactionResultStatus.INVALID_STATE);
                assertEquals(result.resultState, GattState.READ_CHARACTERISTIC_SUCCESS);
                cdl.countDown();
            });
        }, 50);
        cdl.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void twoTxShouldNotBeAllowedToExecuteAtTheSameTime() throws InterruptedException {
        CountDownLatch cdl = new CountDownLatch(2);
        // will just wait for a second and then timeout, then the next tx will run, even though
        // they are both queued at nearly the same time
        TimeoutTestMockTransaction testTx = new TimeoutTestMockTransaction(connection, GattState.IDLE, characteristic);
        Timber.i("Queueing time: %d", System.currentTimeMillis());
        connection.runTx(testTx, result -> {
            assertEquals(TransactionResult.TransactionResultStatus.TIMEOUT, result.resultStatus);
            cdl.countDown();
        });
        WriteGattDescriptorMockTransaction writeGattDescriptorMockTransaction = new WriteGattDescriptorMockTransaction(connection, GattState.WRITE_DESCRIPTOR_SUCCESS, descriptor, fakeData, false, SLOW_TIME_FOR_TX);
        long txStart = System.currentTimeMillis();
        Timber.i("Queueing time: %d", System.currentTimeMillis());
        connection.runTx(writeGattDescriptorMockTransaction, result -> {
            Timber.v("Result from write provided %s", result);
            assertEquals(result.resultStatus, TransactionResult.TransactionResultStatus.SUCCESS);
            assertEquals(result.resultState, GattState.WRITE_DESCRIPTOR_SUCCESS);
            Timber.i("Time for execution: %dms", (System.currentTimeMillis() - txStart));
            assertTrue("Time for execution was greater than one second", (System.currentTimeMillis() - txStart) > 1000);
            cdl.countDown();
        });
        cdl.await(2, TimeUnit.SECONDS);
    }

    @Test
    public void twoBlockingTransactionsShouldBeExecutedSequentiallyUntilCompletion() throws InterruptedException {
        CountDownLatch cdl = new CountDownLatch(2);
        BlockingTaskTestMockTransaction firstBlockingTx = new BlockingTaskTestMockTransaction(connection, GattState.IDLE, characteristic);
        BlockingTaskTestMockTransaction secondBlockingTx = new BlockingTaskTestMockTransaction(connection, GattState.IDLE, characteristic);
        long txStart = System.currentTimeMillis();
        connection.runTx(firstBlockingTx, result -> {
            assertEquals(result.resultStatus, TransactionResult.TransactionResultStatus.TIMEOUT);
            cdl.countDown();
        });
        connection.runTx(secondBlockingTx, result -> {
            assertEquals(result.resultStatus, TransactionResult.TransactionResultStatus.TIMEOUT);
            cdl.countDown();
        });
        cdl.await(4, TimeUnit.SECONDS);
        long txStop = System.currentTimeMillis();
        long txTotal = txStop - txStart;
        Timber.i("Total time to unlatch, %dms", txTotal);
        // if this takes less than the sum of the two tx 2000 ms then it didn't happen right
        assertTrue("Should take greater than or equal to 2000 ms", txTotal >= 2000);
    }

    @Test
    public void testTransactionDelay() throws InterruptedException {
        CountDownLatch cdl = new CountDownLatch(1);
        MockNoOpTransaction firstBlockingTx = new MockNoOpTransaction(connection, GattState.IDLE, 200);
        connection.setIntraTransactionDelay(200);
        long txStart = System.currentTimeMillis();
        connection.runTx(firstBlockingTx, result -> {
            assertEquals(result.resultStatus, TransactionResult.TransactionResultStatus.SUCCESS);
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
}
