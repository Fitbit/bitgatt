/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import androidx.test.core.app.ApplicationProvider;
import com.fitbit.bluetooth.fbgatt.tx.mocks.WriteGattCharacteristicMockTransaction;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Test the composite transaction to make sure it works as expected and that it does things
 * on the proper threads, we can use the mocks because they all call back on the UI thread which
 * can create problems for the composite transaction
 */
@RunWith(RobolectricTestRunner.class)
@Config(
    minSdk = 21
)
@Ignore("Needs to allow running transactions under robolectric")
public class CompositeClientTransactionTest {

    private static final String MOCK_ADDRESS = "02:00:00:00:00:00";
    private static final UUID characteristicUuid = UUID.fromString("BE378669-3D69-43D8-BCC6-39E8769FAF13");
    FitbitBluetoothDevice device  = mock(FitbitBluetoothDevice.class);
    private GattConnection conn;
    private ScheduledExecutorService singleThreadExecutor = Executors.newSingleThreadScheduledExecutor();


    @Before
    public void before() {
        doReturn(MOCK_ADDRESS).when(device).getAddress();
        doReturn("Ionic").when(device).getName();

        Context context = ApplicationProvider.getApplicationContext();


        conn = spy(new GattConnection(device, context.getMainLooper()));
        conn.setMockMode(true);
        conn.setState(GattState.IDLE);
        FitbitGatt.getInstance().registerGattEventListener(new NoOpGattCallback());
        FitbitGatt.getInstance().startGattClient(context);
        FitbitGatt.getInstance().putConnectionIntoDevices(device, conn);
    }

    @After
    public void after() {
        FitbitGatt.getInstance().shutdown();
        FitbitGatt.setInstance(null);
    }

    @Test
    public void testSecondTransactionInCompositeExecutesOnConnectionThread()
        throws InterruptedException {
        byte[] someData = new byte[]{0x16, 0x11, 0x0F};
        BluetoothGattCharacteristic characteristic =
            new BluetoothGattCharacteristic(characteristicUuid,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        WriteGattCharacteristicMockTransaction writeGattCharacteristicMockTransaction =
            new WriteGattCharacteristicMockTransaction(conn, GattState.WRITE_CHARACTERISTIC_SUCCESS, characteristic, someData, false);
        WriteGattCharacteristicMockTransaction writeGattCharacteristicMockTransactionTwo =
            new WriteGattCharacteristicMockTransaction(conn, GattState.WRITE_CHARACTERISTIC_SUCCESS, characteristic, someData, false);
        ArrayList<GattClientTransaction> txList = new ArrayList<>(2);
        txList.add(writeGattCharacteristicMockTransaction);
        txList.add(writeGattCharacteristicMockTransactionTwo);
        CompositeClientTransaction compositeClientTransaction = new CompositeClientTransaction(conn, txList);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        conn.runTx(compositeClientTransaction, callback -> {
            // ensure that we are on the fake main thread
            Assert.assertEquals(Thread.currentThread().getName(), conn.getMainHandler().getLooper().getThread().getName());
            Assert.assertEquals(callback.resultStatus, TransactionResult.TransactionResultStatus.SUCCESS);
            countDownLatch.countDown();
        });
        countDownLatch.await(1, TimeUnit.SECONDS);
        assertEquals(0, countDownLatch.getCount());
    }

    @Test
    public void testFailSecondTransactionShouldHaveNoInterrupted() throws InterruptedException {
        byte[] someData = new byte[]{0x16, 0x11, 0x0F};
        BluetoothGattCharacteristic characteristic =
            new BluetoothGattCharacteristic(characteristicUuid,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        WriteGattCharacteristicMockTransaction writeGattCharacteristicMockTransaction =
            new WriteGattCharacteristicMockTransaction(conn, GattState.WRITE_CHARACTERISTIC_SUCCESS, characteristic, someData, false);
        WriteGattCharacteristicMockTransaction writeGattCharacteristicMockTransactionTwo =
            new WriteGattCharacteristicMockTransaction(conn, GattState.WRITE_CHARACTERISTIC_SUCCESS, characteristic, someData, true);
        ArrayList<GattClientTransaction> txList = new ArrayList<>(2);
        txList.add(writeGattCharacteristicMockTransaction);
        txList.add(writeGattCharacteristicMockTransactionTwo);
        CompositeClientTransaction compositeClientTransaction = new CompositeClientTransaction(conn, txList);
        CountDownLatch cdl = new CountDownLatch(1);
        conn.runTx(compositeClientTransaction, callback -> {
            // ensure that we are on the fake main thread
            Assert.assertEquals(Thread.currentThread().getName(), conn.getMainHandler().getLooper().getThread().getName());
            Assert.assertEquals(callback.resultStatus, TransactionResult.TransactionResultStatus.SUCCESS);
            Thread.currentThread().interrupt();
            cdl.countDown();
        });
        cdl.await(1, TimeUnit.SECONDS);
        singleThreadExecutor = Executors.newSingleThreadScheduledExecutor();
        assertEquals(0, cdl.getCount());
    }

    @Test
    public void testThatRunningTxOnWrongThreadWillThrow() throws InterruptedException {
        byte[] someData = new byte[]{0x16, 0x11, 0x0F};
        BluetoothGattCharacteristic characteristic =
            new BluetoothGattCharacteristic(characteristicUuid,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        CountDownLatch cdl = new CountDownLatch(1);
        WriteGattCharacteristicMockTransaction writeGattCharacteristicMockTransaction =
            new WriteGattCharacteristicMockTransaction(conn, GattState.WRITE_CHARACTERISTIC_SUCCESS, characteristic, someData, false);
        singleThreadExecutor.execute(() -> {
            try {
                writeGattCharacteristicMockTransaction.commit(callback -> {
                    Assert.fail("Should always throw illegal state exception");
                });
            } catch (IllegalStateException ex) {
                assertTrue(ex.getMessage().contains(device.getName()));
                cdl.countDown();
            }
        });
        cdl.await(1, TimeUnit.SECONDS);
        assertEquals(0, cdl.getCount());
    }
}
