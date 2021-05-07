/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.tx.mocks.GattServerConnectMockTransaction;
import com.fitbit.bluetooth.fbgatt.tx.mocks.GattServerDisconnectMockTransaction;
import com.fitbit.bluetooth.fbgatt.tx.mocks.SubscribeToCharacteristicNotificationsMockTransaction;
import com.fitbit.bluetooth.fbgatt.tx.mocks.WriteGattCharacteristicMockTransaction;
import com.fitbit.bluetooth.fbgatt.tx.mocks.WriteGattDescriptorMockTransaction;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
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
import java.util.List;
import java.util.UUID;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@SuppressWarnings("deprecation")
@RunWith(JUnit4.class)
public class TxPreCommitTest {

    private static final String MOCK_ADDRESS = "02:00:00:00:00:00";
    private FitbitBluetoothDevice device;

    @Before
    public void before() {
        Context mockContext = mock(Context.class);
        when(mockContext.getSystemService(Any.class)).thenReturn(null);
        when(mockContext.getApplicationContext()).thenReturn(mockContext);
        FitbitGatt.getInstance().setStarted();
        FitbitGatt.getInstance().setAppContext(mockContext);
        Handler mockHandler = mock(Handler.class);
        Looper mockLooper = mock(Looper.class);
        Thread mockThread = mock(Thread.class);
        when(mockThread.getName()).thenReturn("Irvin's mock thread");
        when(mockLooper.getThread()).thenReturn(mockThread);
        when(mockHandler.getLooper()).thenReturn(mockLooper);
        when(mockHandler.postDelayed(any(Runnable.class), anyLong())).thenAnswer((Answer) invocation -> {
            Runnable msg = invocation.getArgument(0);
            msg.run();
            return null;
        });
        when(mockHandler.post(any(Runnable.class))).thenAnswer((Answer) invocation -> {
            Runnable msg = invocation.getArgument(0);
            msg.run();
            return null;
        });
        device = new FitbitBluetoothDevice(MOCK_ADDRESS, "fooDevice", mock(BluetoothDevice.class));
        GattConnection conn = spy(new GattConnection(device, mockLooper));
        conn.setMockMode(true);
        // you are mocking get connection handler, but runTx uses the property
        when(conn.getMainHandler()).thenReturn(mockHandler);
        FitbitGatt.getInstance().putConnectionIntoDevices(device, conn);
        GattServerConnection serverConnection = spy(new GattServerConnection(null, mockLooper));
        serverConnection.setMockMode(true);
        when(serverConnection.getMainHandler()).thenReturn(mockHandler);
        FitbitGatt.getInstance().setGattServerConnection(serverConnection);
    }

    @After
    public void after() {
        FitbitGatt.getInstance().shutdown();
        FitbitGatt.setInstance(null);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void gattPreCommitTest() {
        Assert.assertTrue(FitbitGatt.getInstance().isInitialized());
        final byte[] fakeData = new byte[]{'a','b','c','d','e','f','g','h','i', 'j'};
        BluetoothGattCharacteristic characteristic = mock(BluetoothGattCharacteristic.class);
        when(characteristic.getUuid()).thenReturn(UUID.randomUUID());
        when(characteristic.getProperties()).thenReturn(PROPERTY_WRITE);
        when(characteristic.getPermissions()).thenReturn(BluetoothGattCharacteristic.PERMISSION_WRITE);
        BluetoothGattDescriptor descriptor = mock(BluetoothGattDescriptor.class);
        // this should be the same as the real thing, the data inside could change as it's pointing
        // to something else.
        when(descriptor.getValue()).thenReturn(fakeData);
        when(descriptor.getUuid()).thenReturn(UUID.randomUUID());
        when(descriptor.getPermissions()).thenReturn(BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED);
        GattConnection conn = FitbitGatt.getInstance().getConnection(device);
        if(conn == null) {
            fail();
        }
        conn.setState(GattState.CONNECTED);
        WriteGattDescriptorMockTransaction writeGattDescriptorMockTransaction = new WriteGattDescriptorMockTransaction(conn, GattState.WRITE_DESCRIPTOR_SUCCESS, descriptor, fakeData, false);
        WriteGattCharacteristicMockTransaction writeGattCharacteristicMockTransaction = new WriteGattCharacteristicMockTransaction(conn, GattState.WRITE_CHARACTERISTIC_SUCCESS, characteristic, fakeData, false);
        writeGattCharacteristicMockTransaction.addPreCommitHook(writeGattDescriptorMockTransaction);
        conn.runTx(writeGattCharacteristicMockTransaction, result -> {
            assertTrue(result.getTransactionName().equals(writeGattCharacteristicMockTransaction.getName()) && result.resultStatus.equals(TransactionResult.TransactionResultStatus.SUCCESS));
        });
    }

    @Test
    public void gattPrecommitOrderingTest(){
        final byte[] fakeData = new byte[]{'a','b','c','d','e','f','g','h','i', 'j'};
        BluetoothGattCharacteristic characteristic = mock(BluetoothGattCharacteristic.class);
        when(characteristic.getUuid()).thenReturn(UUID.randomUUID());
        when(characteristic.getProperties()).thenReturn(PROPERTY_WRITE);
        when(characteristic.getPermissions()).thenReturn(BluetoothGattCharacteristic.PERMISSION_WRITE);
        BluetoothGattDescriptor descriptor = mock(BluetoothGattDescriptor.class);
        // this should be the same as the real thing, the data inside could change as it's pointing
        // to something else.
        when(descriptor.getValue()).thenReturn(fakeData);
        when(descriptor.getUuid()).thenReturn(UUID.randomUUID());
        when(descriptor.getPermissions()).thenReturn(BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED);
        GattConnection conn = FitbitGatt.getInstance().getConnection(device);
        assert conn != null;
        conn.setState(GattState.CONNECTED);
        WriteGattCharacteristicMockTransaction writeGattCharacteristicMockTransaction = new WriteGattCharacteristicMockTransaction(conn, GattState.WRITE_CHARACTERISTIC_SUCCESS, characteristic, fakeData, false);
        SubscribeToCharacteristicNotificationsMockTransaction subscribe = new SubscribeToCharacteristicNotificationsMockTransaction(conn, GattState.ENABLE_CHARACTERISTIC_NOTIFICATION_SUCCESS, characteristic, fakeData, false);
        WriteGattDescriptorMockTransaction writeDescriptor = new WriteGattDescriptorMockTransaction(conn, GattState.WRITE_DESCRIPTOR_SUCCESS, descriptor, fakeData, false);
        writeGattCharacteristicMockTransaction.addPreCommitHook(subscribe);
        writeGattCharacteristicMockTransaction.addPreCommitHook(writeDescriptor);
        conn.runTx(writeGattCharacteristicMockTransaction, result -> {
            assertEquals(3, writeGattCharacteristicMockTransaction.getExecutedTransactions());
            assertTrue(writeGattCharacteristicMockTransaction.hasStarted());
            assertTrue(subscribe.hasStarted());
            assertTrue(writeDescriptor.hasStarted());
            assertTrue(result.getTransactionName().equals(writeGattCharacteristicMockTransaction.getName()) && result.resultStatus.equals(TransactionResult.TransactionResultStatus.SUCCESS));
        });
    }

    @Test
    public void gattPostCommitTest() {
        final byte[] fakeData = new byte[]{'a','b','c','d','e','f','g','h','i', 'j'};
        BluetoothGattCharacteristic characteristic = mock(BluetoothGattCharacteristic.class);
        when(characteristic.getUuid()).thenReturn(UUID.randomUUID());
        when(characteristic.getProperties()).thenReturn(PROPERTY_WRITE);
        when(characteristic.getPermissions()).thenReturn(BluetoothGattCharacteristic.PERMISSION_WRITE);
        BluetoothGattDescriptor descriptor = mock(BluetoothGattDescriptor.class);
        // this should be the same as the real thing, the data inside could change as it's pointing
        // to something else.
        when(descriptor.getValue()).thenReturn(fakeData);
        when(descriptor.getUuid()).thenReturn(UUID.randomUUID());
        when(descriptor.getPermissions()).thenReturn(BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED);
        GattConnection conn = FitbitGatt.getInstance().getConnection(device);
        assert conn != null;
        conn.setState(GattState.CONNECTED);
        WriteGattCharacteristicMockTransaction writeGattCharacteristicMockTransaction = new WriteGattCharacteristicMockTransaction(conn, GattState.WRITE_CHARACTERISTIC_SUCCESS, characteristic, fakeData, false);
        SubscribeToCharacteristicNotificationsMockTransaction subscribe = new SubscribeToCharacteristicNotificationsMockTransaction(conn, GattState.ENABLE_CHARACTERISTIC_NOTIFICATION_SUCCESS, characteristic, fakeData, false);
        WriteGattDescriptorMockTransaction writeDescriptor = new WriteGattDescriptorMockTransaction(conn, GattState.WRITE_DESCRIPTOR_SUCCESS, descriptor, fakeData, false);
        writeGattCharacteristicMockTransaction.addPostCommitHook(subscribe);
        writeGattCharacteristicMockTransaction.addPostCommitHook(writeDescriptor);
        conn.runTx(writeGattCharacteristicMockTransaction, result -> {
            assertTrue(result.getTransactionName().equals(writeDescriptor.getName()) && result.resultStatus.equals(TransactionResult.TransactionResultStatus.SUCCESS));
        });
    }

    @Test
    public void compositeTransactionTest(){
        final byte[] fakeData = new byte[]{'a','b','c','d','e','f','g','h','i', 'j'};
        BluetoothGattCharacteristic characteristic = mock(BluetoothGattCharacteristic.class);
        when(characteristic.getUuid()).thenReturn(UUID.randomUUID());
        when(characteristic.getProperties()).thenReturn(PROPERTY_WRITE);
        when(characteristic.getPermissions()).thenReturn(BluetoothGattCharacteristic.PERMISSION_WRITE);
        BluetoothGattDescriptor descriptor = mock(BluetoothGattDescriptor.class);
        // this should be the same as the real thing, the data inside could change as it's pointing
        // to something else.
        when(descriptor.getValue()).thenReturn(fakeData);
        when(descriptor.getUuid()).thenReturn(UUID.randomUUID());
        when(descriptor.getPermissions()).thenReturn(BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED);
        GattConnection conn = FitbitGatt.getInstance().getConnection(device);
        assert conn != null;
        conn.setState(GattState.CONNECTED);
        WriteGattCharacteristicMockTransaction writeGattCharacteristicMockTransaction = new WriteGattCharacteristicMockTransaction(conn, GattState.WRITE_CHARACTERISTIC_SUCCESS, characteristic, fakeData, false);
        SubscribeToCharacteristicNotificationsMockTransaction subscribe = new SubscribeToCharacteristicNotificationsMockTransaction(conn, GattState.ENABLE_CHARACTERISTIC_NOTIFICATION_SUCCESS, characteristic, fakeData, false);
        WriteGattDescriptorMockTransaction writeDescriptor = new WriteGattDescriptorMockTransaction(conn, GattState.WRITE_DESCRIPTOR_SUCCESS, descriptor, fakeData, false);
        ArrayList<GattClientTransaction> transactions = new ArrayList<>(3);
        transactions.add(writeGattCharacteristicMockTransaction);
        transactions.add(subscribe);
        transactions.add(writeDescriptor);
        CompositeClientTransaction composite = new CompositeClientTransaction(conn, transactions);
        conn.runTx(composite, result -> {
            List<TransactionResult> results = result.getTransactionResults();
            for(TransactionResult txResult : results) {
                Assert.assertEquals(txResult.resultStatus, TransactionResult.TransactionResultStatus.SUCCESS);
            }
            Assert.assertEquals(results.get(0).getTransactionName(), writeGattCharacteristicMockTransaction.getName());
            Assert.assertEquals(results.get(1).getTransactionName(), subscribe.getName());
            Assert.assertEquals(results.get(2).getTransactionName(), writeDescriptor.getName());
        });
    }

    @Test
    public void compositeTransactionFailHaltChainTest(){
        final byte[] fakeData = new byte[]{'a','b','c','d','e','f','g','h','i', 'j'};
        BluetoothGattCharacteristic characteristic = mock(BluetoothGattCharacteristic.class);
        when(characteristic.getUuid()).thenReturn(UUID.randomUUID());
        when(characteristic.getProperties()).thenReturn(PROPERTY_WRITE);
        when(characteristic.getPermissions()).thenReturn(BluetoothGattCharacteristic.PERMISSION_WRITE);
        BluetoothGattDescriptor descriptor = mock(BluetoothGattDescriptor.class);
        // this should be the same as the real thing, the data inside could change as it's pointing
        // to something else.
        when(descriptor.getValue()).thenReturn(fakeData);
        when(descriptor.getUuid()).thenReturn(UUID.randomUUID());
        when(descriptor.getPermissions()).thenReturn(BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED);
        GattConnection conn = FitbitGatt.getInstance().getConnection(device);
        assert conn != null;
        conn.setState(GattState.CONNECTED);
        WriteGattCharacteristicMockTransaction writeGattCharacteristicMockTransaction = new WriteGattCharacteristicMockTransaction(conn, GattState.WRITE_CHARACTERISTIC_SUCCESS, characteristic, fakeData, false);
        SubscribeToCharacteristicNotificationsMockTransaction subscribe = new SubscribeToCharacteristicNotificationsMockTransaction(conn, GattState.ENABLE_CHARACTERISTIC_NOTIFICATION_SUCCESS, characteristic, fakeData, true);
        WriteGattDescriptorMockTransaction writeDescriptor = new WriteGattDescriptorMockTransaction(conn, GattState.WRITE_DESCRIPTOR_SUCCESS, descriptor, fakeData, false);
        ArrayList<GattClientTransaction> transactions = new ArrayList<>(3);
        transactions.add(writeGattCharacteristicMockTransaction);
        transactions.add(subscribe);
        transactions.add(writeDescriptor);
        CompositeClientTransaction composite = new CompositeClientTransaction(conn, transactions);
        conn.runTx(composite, result -> {
            Assert.assertEquals(result.resultStatus, TransactionResult.TransactionResultStatus.FAILURE);
            List<TransactionResult> results = result.getTransactionResults();
            Assert.assertEquals(2, results.size());
            Assert.assertEquals(results.get(0).resultStatus, TransactionResult.TransactionResultStatus.SUCCESS);
            Assert.assertNotEquals(results.get(1).resultStatus, TransactionResult.TransactionResultStatus.SUCCESS);
            Assert.assertEquals(results.get(0).getTransactionName(), writeGattCharacteristicMockTransaction.getName());
            Assert.assertEquals(results.get(1).getTransactionName(), subscribe.getName());
        });
    }

    @Test
    public void compositeServerTransactionTest(){
        GattServerConnection conn = FitbitGatt.getInstance().getServer();
        conn.setState(GattState.CONNECTED);
        GattServerConnectMockTransaction connect1 = new GattServerConnectMockTransaction(conn, GattState.CONNECTED,  device, false);
        GattServerDisconnectMockTransaction connect2 = new GattServerDisconnectMockTransaction(conn, GattState.DISCONNECTED,  device, false);
        GattServerConnectMockTransaction connect3 = new GattServerConnectMockTransaction(conn, GattState.CONNECTED,  device, false);
        ArrayList<GattServerTransaction> transactions = new ArrayList<>(3);
        transactions.add(connect1);
        transactions.add(connect2);
        transactions.add(connect3);
        CompositeServerTransaction composite = new CompositeServerTransaction(conn, transactions);
        conn.runTx(composite, result -> {
            List<TransactionResult> results = result.getTransactionResults();
            for(TransactionResult txResult : results) {
                Assert.assertEquals(txResult.resultStatus, TransactionResult.TransactionResultStatus.SUCCESS);
            }
            Assert.assertEquals(results.get(0).getTransactionName(), connect1.getName());
            Assert.assertEquals(results.get(1).getTransactionName(), connect2.getName());
            Assert.assertEquals(results.get(2).getTransactionName(), connect3.getName());
        });
    }

    @Test
    public void compositeServerTransactionFailHaltChainTest(){
        GattServerConnection conn = FitbitGatt.getInstance().getServer();
        conn.setState(GattState.CONNECTED);
        GattServerConnectMockTransaction connect1 = new GattServerConnectMockTransaction(conn, GattState.CONNECTED,  device, false);
        GattServerDisconnectMockTransaction connect2 = new GattServerDisconnectMockTransaction(conn, GattState.DISCONNECTED,  device, true);
        GattServerConnectMockTransaction connect3 = new GattServerConnectMockTransaction(conn, GattState.CONNECTED,  device, false);
        ArrayList<GattServerTransaction> transactions = new ArrayList<>(3);
        transactions.add(connect1);
        transactions.add(connect2);
        transactions.add(connect3);
        CompositeServerTransaction composite = new CompositeServerTransaction(conn, transactions);
        conn.runTx(composite, result -> {
            Assert.assertEquals(result.resultStatus, TransactionResult.TransactionResultStatus.FAILURE);
            List<TransactionResult> results = result.getTransactionResults();
            Assert.assertEquals(2, results.size());
            Assert.assertEquals(results.get(0).resultStatus, TransactionResult.TransactionResultStatus.SUCCESS);
            Assert.assertNotEquals(results.get(1).resultStatus, TransactionResult.TransactionResultStatus.SUCCESS);
            Assert.assertEquals(results.get(0).getTransactionName(), connect1.getName());
            Assert.assertEquals(results.get(1).getTransactionName(), connect2.getName());
        });
    }
}
