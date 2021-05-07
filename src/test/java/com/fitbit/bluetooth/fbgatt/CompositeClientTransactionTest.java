/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.tx.mocks.WriteGattCharacteristicMockTransaction;
import com.fitbit.bluetooth.fbgatt.util.BluetoothUtils;
import com.fitbit.bluetooth.fbgatt.util.LooperWatchdog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.internal.matchers.Any;
import org.mockito.stubbing.Answer;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Test the composite transaction to make sure it works as expected and that it does things
 * on the proper threads, we can use the mocks because they all call back on the UI thread which
 * can create problems for the composite transaction
 */
@RunWith(JUnit4.class)
public class CompositeClientTransactionTest {

    private static final String MOCK_ADDRESS = "02:00:00:00:00:00";
    private static final UUID characteristicUuid = UUID.fromString("BE378669-3D69-43D8-BCC6-39E8769FAF13");
    FitbitBluetoothDevice device = new FitbitBluetoothDevice(MOCK_ADDRESS, "fooDevice", mock(BluetoothDevice.class));
    private GattConnection conn;
    private ScheduledExecutorService singleThreadExecutor = Executors.newSingleThreadScheduledExecutor();
    @SuppressWarnings("FutureReturnValueIgnored")
    private Answer<Boolean> handlerPostAnswer = invocation -> {
        Long delay = 0L;
        if (invocation.getArguments().length > 1) {
            delay = invocation.getArgument(1);
        }
        Runnable msg = invocation.getArgument(0);
        if (msg != null) {
            singleThreadExecutor.schedule(msg, delay, TimeUnit.MILLISECONDS);
        }
        return true;
    };

    @Before
    public void before() {
        BluetoothUtils utilsMock = mock(BluetoothUtils.class);
        LowEnergyAclListener lowEnergyAclListenerMock = mock(LowEnergyAclListener.class);
        BluetoothRadioStatusListener bluetoothRadioStatusListenerMock = mock(BluetoothRadioStatusListener.class);
        BitGattDependencyProvider dependencyProviderMock = mock(BitGattDependencyProvider.class);
        Context mockContext = mock(Context.class);

        when(mockContext.getSystemService(Any.class)).thenReturn(null);
        when(mockContext.getApplicationContext()).thenReturn(mockContext);
        doReturn(bluetoothRadioStatusListenerMock).when(dependencyProviderMock).getNewBluetoothRadioStatusListener(any(), eq(false));
        doReturn(utilsMock).when(dependencyProviderMock).getBluetoothUtils();
        doReturn(lowEnergyAclListenerMock).when(dependencyProviderMock).getNewLowEnergyAclListener();
        doReturn(true).when(utilsMock).isBluetoothEnabled(mockContext);

        Looper mockMainThreadLooper = mock(Looper.class);
        Thread mockMainThread = mock(Thread.class);
        when(mockMainThread.getName()).thenReturn("Irvin's mock thread");
        when(mockMainThreadLooper.getThread()).thenReturn(mockMainThread);
        Context ctx = mock(Context.class);
        when(ctx.getApplicationContext()).thenReturn(ctx);
        when(ctx.getMainLooper()).thenReturn(mockMainThreadLooper);


        Handler mockHandler = mock(Handler.class);
        doAnswer(handlerPostAnswer).when(mockHandler).post(any(Runnable.class));
        doAnswer(handlerPostAnswer).when(mockHandler).postDelayed(any(Runnable.class), anyLong());
        when(mockHandler.getLooper()).thenReturn(mockMainThreadLooper);
        conn = spy(new GattConnection(device, ctx.getMainLooper()));
        conn.setMockMode(true);
        when(conn.getMainHandler()).thenReturn(mockHandler);
        conn.setState(GattState.IDLE);
        FitbitGatt.getInstance().setAsyncOperationThreadWatchdog(mock(LooperWatchdog.class));
        FitbitGatt.getInstance().registerGattEventListener(new NoOpGattCallback());
        FitbitGatt.getInstance().setDependencyProvider(dependencyProviderMock);
        FitbitGatt.getInstance().startGattClient(ctx);
        FitbitGatt.getInstance().putConnectionIntoDevices(device, conn);
    }

    @After
    public void after() {
        FitbitGatt.getInstance().shutdown();
        FitbitGatt.setInstance(null);
    }

    @Test
    public void testSecondTransactionInCompositeExecutesOnConnectionThread() {
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
        conn.runTx(compositeClientTransaction, callback -> {
            // ensure that we are on the fake main thread
            Assert.assertEquals(Thread.currentThread().getName(), conn.getMainHandler().getLooper().getThread().getName());
            Assert.assertEquals(callback.resultStatus, TransactionResult.TransactionResultStatus.SUCCESS);
        });
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
        assertTrue("No exceptions were thrown", true);
        singleThreadExecutor = Executors.newSingleThreadScheduledExecutor();
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
                    cdl.countDown();
                });
            } catch (IllegalStateException ex) {
                assertTrue(ex.getMessage().contains(device.getName()));
                cdl.countDown();
            }
        });
        cdl.await(1000, TimeUnit.MILLISECONDS);
    }
}
