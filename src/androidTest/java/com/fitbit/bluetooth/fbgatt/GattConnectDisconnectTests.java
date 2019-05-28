/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.fitbit.bluetooth.fbgatt.tx.mocks.CloseGattMockTransaction;
import com.fitbit.bluetooth.fbgatt.tx.mocks.GattClientDiscoverMockServicesTransaction;
import com.fitbit.bluetooth.fbgatt.tx.mocks.GattConnectMockTransaction;
import com.fitbit.bluetooth.fbgatt.tx.mocks.GattDisconnectMockTransaction;
import com.fitbit.bluetooth.fbgatt.tx.mocks.GattServerConnectMockTransaction;
import com.fitbit.bluetooth.fbgatt.tx.mocks.GattServerDisconnectMockTransaction;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class GattConnectDisconnectTests {

    private static final String MOCK_ADDRESS = "02:00:00:00:00:00";
    private static final String MOCK_ADDRESS_2 = "04:00:00:00:00:00";
    private static final String MOCK_ADDRESS_3 = "1D:00:00:00:00:00";
    private Context mockContext;

    @Before
    public void before() {
        this.mockContext = InstrumentationRegistry.getContext();
    }

    @After
    public void after() {
        FitbitGatt.getInstance().clearConnectionsMap();
    }

    @Test
    public void testConnect() throws Exception {
        // started
        FitbitGatt.getInstance().start(mockContext);
        Assert.assertTrue(FitbitGatt.getInstance().isStarted());
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(MOCK_ADDRESS, "fooDevice");
        GattConnection connection = FitbitGatt.getInstance().getConnection(device);
        if (connection == null) {
            connection = new GattConnection(device, mockContext.getMainLooper());
            FitbitGatt.getInstance().getConnectionMap().put(device, connection);
        }
        connection.setMockMode(true);
        connection.setState(GattState.DISCONNECTED);
        CountDownLatch cdl = new CountDownLatch(1);
        if (!connection.isConnected()) {
            GattConnectMockTransaction connectTransaction = new GattConnectMockTransaction(connection, GattState.CONNECTED, false);
            connection.runTx(connectTransaction, result -> {
                assertEquals(result.resultStatus, TransactionResult.TransactionResultStatus.SUCCESS);
                cdl.countDown();
            });
        } else {
            cdl.countDown();
            Assert.fail();
        }
        cdl.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void connectByInstantiatingAConnectionWhenOneExists() throws Exception {
        // started
        FitbitGatt.getInstance().start(mockContext);
        Assert.assertTrue(FitbitGatt.getInstance().isStarted());
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(MOCK_ADDRESS, "fooDevice");
        GattConnection connection = new GattConnection(device, mockContext.getMainLooper());
        connection.setMockMode(true);
        connection.setState(GattState.DISCONNECTED);
        FitbitGatt.getInstance().putConnectionIntoDevices(device, connection);
        GattConnection connection2 = new GattConnection(device, mockContext.getMainLooper());
        connection2.setMockMode(true);
        connection2.setState(GattState.DISCONNECTED);
        CountDownLatch cdl = new CountDownLatch(1);
        if (!connection2.isConnected()) {
            GattConnectMockTransaction connectTransaction = new GattConnectMockTransaction(connection2, GattState.CONNECTED, false);
            connection2.runTx(connectTransaction, result -> {
                assertEquals(result.resultStatus, TransactionResult.TransactionResultStatus.FAILURE);
                cdl.countDown();
            });
            cdl.await(2, TimeUnit.SECONDS);
        } else {
            Assert.fail();
        }
    }

    @Test
    public void connectByInstantiatingTwoIndependentConnections() {
        // started
        FitbitGatt.getInstance().start(mockContext);
        Assert.assertTrue(FitbitGatt.getInstance().isStarted());
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(MOCK_ADDRESS_3, "fooDevice");
        // unmanaged connections should be able to connect however they would like
        GattConnection connection = new GattConnection(device, mockContext.getMainLooper());
        connection.setMockMode(true);
        connection.setState(GattState.DISCONNECTED);
        GattConnection connection2 = new GattConnection(device, mockContext.getMainLooper());
        connection2.setMockMode(true);
        connection2.setState(GattState.DISCONNECTED);
        Assert.assertTrue("Should be true if connection attempt was successful", connection2.connect());
    }

    @Test
    public void connectByInstantiatingTwoDifferentConnectionsOneInMap() {
        // started
        FitbitGatt.getInstance().start(mockContext);
        Assert.assertTrue(FitbitGatt.getInstance().isStarted());
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(MOCK_ADDRESS, "fooDevice");
        FitbitBluetoothDevice device2 = new FitbitBluetoothDevice(MOCK_ADDRESS_2, "fooDevice2");
        GattConnection connection = new GattConnection(device, mockContext.getMainLooper());
        connection.setMockMode(true);
        connection.setState(GattState.DISCONNECTED);
        GattConnection connection2 = new GattConnection(device2, mockContext.getMainLooper());
        connection2.setMockMode(true);
        connection2.setState(GattState.DISCONNECTED);
        FitbitGatt.getInstance().putConnectionIntoDevices(device2, connection2);
        Assert.assertTrue("Should be true if connection attempt was successful", connection.connect());
    }

    @Test
    public void testConnectThreading() throws Exception {
        // started
        FitbitGatt.getInstance().start(mockContext);
        Assert.assertTrue(FitbitGatt.getInstance().isStarted());
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(MOCK_ADDRESS, "fooDevice");
        GattConnection connection = FitbitGatt.getInstance().getConnection(device);
        if (connection == null) {
            connection = new GattConnection(device, mockContext.getMainLooper());
            FitbitGatt.getInstance().getConnectionMap().put(device, connection);
        }
        connection.setMockMode(true);
        connection.setState(GattState.DISCONNECTED);
        CountDownLatch cdl = new CountDownLatch(1);
        if (!connection.isConnected()) {
            String threadName = connection.getMainHandler().getLooper().getThread().getName();
            Timber.v("Dispatch thread name: %s", threadName);
            GattConnectMockTransaction connectTransaction = new GattConnectMockTransaction(connection, GattState.CONNECTED, false);
            GattConnection finalConnection = connection;
            connection.runTx(connectTransaction, result -> {
                assertEquals(result.resultStatus, TransactionResult.TransactionResultStatus.SUCCESS);
                // we should be on the same thread from which we threw out the transaction ( connection thread )
                Assert.assertEquals(threadName, finalConnection.getMainHandler().getLooper().getThread().getName());
                cdl.countDown();
            });
        } else {
            cdl.countDown();
            Assert.fail();
        }
        cdl.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void testServerConnect() throws Exception {
        // started
        FitbitGatt.getInstance().start(mockContext);
        Assert.assertTrue(FitbitGatt.getInstance().isStarted());
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(MOCK_ADDRESS, "fooDevice");
        GattServerConnection connection = FitbitGatt.getInstance().getServer();
        connection.setMockMode(true);
        connection.setState(GattState.DISCONNECTED);
        CountDownLatch cdl = new CountDownLatch(1);
        GattServerConnectMockTransaction connectTransaction = new GattServerConnectMockTransaction(connection, GattState.CONNECTED, device, false);
        connection.runTx(connectTransaction, result -> {
            Timber.w("Transaction result %s", result);
            assertTrue(result.resultStatus.equals(TransactionResult.TransactionResultStatus.SUCCESS) && connection.getGattState().equals(GattState.CONNECTED));
            cdl.countDown();
        });
        cdl.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void testCloseGatt() throws Exception {
        // started
        FitbitGatt.getInstance().start(mockContext);
        Assert.assertTrue(FitbitGatt.getInstance().isStarted());
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(MOCK_ADDRESS, "fooDevice");
        GattConnection connection = FitbitGatt.getInstance().getConnection(device);
        if (connection == null) {
            connection = new GattConnection(device, mockContext.getMainLooper());
            FitbitGatt.getInstance().getConnectionMap().put(device, connection);
        }
        connection.setMockMode(true);
        connection.setState(GattState.CONNECTED);
        CountDownLatch cdl = new CountDownLatch(1);
        CloseGattMockTransaction closeMock = new CloseGattMockTransaction(connection, GattState.CLOSE_GATT_CLIENT_SUCCESS);
        GattConnection finalConnection = connection;
        connection.runTx(closeMock, result -> {
            Timber.d("Result %s", result);
            assertTrue(result.resultStatus.equals(TransactionResult.TransactionResultStatus.SUCCESS) && finalConnection.getGattState().equals(GattState.CLOSE_GATT_CLIENT_SUCCESS));
            cdl.countDown();
        });
        cdl.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void testDisconnect() throws Exception {
        // started
        FitbitGatt.getInstance().start(mockContext);
        Assert.assertTrue(FitbitGatt.getInstance().isStarted());
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(MOCK_ADDRESS, "fooDevice");
        GattConnection connection = FitbitGatt.getInstance().getConnection(device);
        if (connection == null) {
            connection = new GattConnection(device, mockContext.getMainLooper());
            FitbitGatt.getInstance().getConnectionMap().put(device, connection);
        }
        connection.setMockMode(true);
        connection.setState(GattState.CONNECTED);
        CountDownLatch latch = new CountDownLatch(1);
        GattDisconnectMockTransaction disconnectTransaction = new GattDisconnectMockTransaction(connection, GattState.DISCONNECTED, false);
        // shouldn't let go until transition to disconnected
        connection.runTx(disconnectTransaction, nuResult -> {
            assertEquals(GattState.DISCONNECTED, nuResult.getResultState());
            latch.countDown();
        });
        latch.await(2, TimeUnit.SECONDS);
    }

    @Test
    public void testIsConnectedWhenStateIsDisconnecting() {
        // started
        FitbitGatt.getInstance().start(mockContext);
        Assert.assertTrue(FitbitGatt.getInstance().isStarted());
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(MOCK_ADDRESS, "fooDevice");
        GattConnection connection = FitbitGatt.getInstance().getConnection(device);
        if (connection == null) {
            connection = new GattConnection(device, mockContext.getMainLooper());
            FitbitGatt.getInstance().getConnectionMap().put(device, connection);
        }
        connection.setMockMode(true);
        connection.setState(GattState.DISCONNECTING);
        Assert.assertFalse(connection.isConnected());
    }

    @Test
    public void testIsConnectedWhenStateIsConnecting() {
        // started
        FitbitGatt.getInstance().start(mockContext);
        Assert.assertTrue(FitbitGatt.getInstance().isStarted());
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(MOCK_ADDRESS, "fooDevice");
        GattConnection connection = FitbitGatt.getInstance().getConnection(device);
        if (connection == null) {
            connection = new GattConnection(device, mockContext.getMainLooper());
            FitbitGatt.getInstance().getConnectionMap().put(device, connection);
        }
        connection.setMockMode(true);
        connection.setState(GattState.CONNECTING);
        Assert.assertFalse(connection.isConnected());
    }

    @Test
    public void testServerDisconnect() throws Exception {
        // started
        FitbitGatt.getInstance().start(mockContext);
        Assert.assertTrue(FitbitGatt.getInstance().isStarted());
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(MOCK_ADDRESS, "fooDevice");
        GattServerConnection connection = FitbitGatt.getInstance().getServer();
        connection.setMockMode(true);
        connection.setState(GattState.IDLE);
        CountDownLatch cdl = new CountDownLatch(1);
        GattServerDisconnectMockTransaction connectTransaction = new GattServerDisconnectMockTransaction(connection, GattState.DISCONNECTED, device, false);
        connection.runTx(connectTransaction, result -> {
            assertTrue(result.resultStatus.equals(TransactionResult.TransactionResultStatus.SUCCESS) && connection.getGattState().equals(GattState.DISCONNECTED));
            cdl.countDown();
        });
        cdl.await(1, TimeUnit.SECONDS);
    }

    @Test
    @Ignore("Skipping until we sort how to make this non-flaky") // skip for now, we can't really reliably connect
    public void connectToScannedDevice() throws Exception {
        // started
        FitbitGatt.getInstance().start(mockContext);
        Assert.assertTrue(FitbitGatt.getInstance().isStarted());
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(MOCK_ADDRESS, "fooDevice");
        CountDownLatch latch = new CountDownLatch(1);
        FitbitGatt.getInstance().connectToScannedDevice(device, true, result -> {
            Assert.assertEquals(TransactionResult.TransactionResultStatus.SUCCESS, result.resultStatus);
            latch.countDown();
        });
        latch.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void filterConnectedDevices() {
        // started
        FitbitGatt.getInstance().start(mockContext);
        Assert.assertTrue(FitbitGatt.getInstance().isStarted());
        // populate fake connected devices
        for (int i = 0; i < 4; i++) {
            String address = String.format(Locale.ENGLISH, "02:00:00:00:00:1%s", Integer.toString(i));
            String name = String.format(Locale.ENGLISH, "fooDevice%s", i);
            FitbitBluetoothDevice device = new FitbitBluetoothDevice(address, name);
            GattConnection conn = new GattConnection(device, mockContext.getMainLooper());
            FitbitGatt.getInstance().putConnectionIntoDevices(device, conn);
        }
        // request matching conns
        ArrayList<String> names = new ArrayList<>(2);
        names.add("fooDevice1");
        names.add("fooDevice3");
        List<GattConnection> conns = FitbitGatt.getInstance().getMatchingConnectionsForDeviceNames(names);
        Assert.assertEquals(conns.size(), 2);
    }

    @Test
    public void filterConnectedDevicesAll() {
        // started
        FitbitGatt.getInstance().start(mockContext);
        Assert.assertTrue(FitbitGatt.getInstance().isStarted());
        UUID serviceUuidOne = UUID.fromString("5F1CBF47-4A8E-467F-9E55-BAFE00839CC1");
        UUID serviceUuidTwo = UUID.fromString("528B080E-6608-403F-8F39-2246650751D0");
        BluetoothGattService service1 = new BluetoothGattService(serviceUuidOne, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattService service2 = new BluetoothGattService(serviceUuidTwo, BluetoothGattService.SERVICE_TYPE_SECONDARY);
        // populate fake connected devices
        for (int i = 0; i < 4; i++) {
            String address = String.format(Locale.ENGLISH, "02:00:00:00:00:1%s", Integer.toString(i));
            String name = String.format(Locale.ENGLISH, "fooDevice%s", i);
            FitbitBluetoothDevice device = new FitbitBluetoothDevice(address, name);
            GattConnection conn = new GattConnection(device, mockContext.getMainLooper());
            conn.setMockMode(true);
            // for the purpose of this test we will just set connected to true
            conn.setState(GattState.CONNECTED);
            if (i % 2 == 0) {
                conn.addService(service1);
            } else {
                conn.addService(service2);
            }
            FitbitGatt.getInstance().putConnectionIntoDevices(device, conn);
        }
        // request matching conns
        ArrayList<UUID> services = new ArrayList<>(2);
        services.add(serviceUuidOne);
        List<GattConnection> conns = FitbitGatt.getInstance().getMatchingConnectionsForServices(services);
        Assert.assertEquals(2, conns.size());
    }

    @Test
    public void gattDiscoverServicesTest() throws Exception {
        FitbitGatt.getInstance().start(mockContext);

        UUID serviceUuidOne = UUID.randomUUID();
        UUID serviceUuidTwo = UUID.randomUUID();

        BluetoothGattService service1 = new BluetoothGattService(serviceUuidOne, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattService service2 = new BluetoothGattService(serviceUuidTwo, BluetoothGattService.SERVICE_TYPE_SECONDARY);
        ArrayList<BluetoothGattService> services = new ArrayList<>();
        services.add(service1);
        services.add(service2);
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(MOCK_ADDRESS, "fooDevice");
        GattConnection conn = new GattConnection(device, mockContext.getMainLooper());
        FitbitGatt.getInstance().putConnectionIntoDevices(device, conn);
        conn.setMockMode(true);
        conn.setState(GattState.IDLE);
        CountDownLatch latch = new CountDownLatch(1);
        GattClientDiscoverMockServicesTransaction discoverMockTx = new GattClientDiscoverMockServicesTransaction(conn, GattState.DISCOVERY_SUCCESS, services, false);
        conn.runTx(discoverMockTx, result -> {
            Assert.assertEquals(TransactionResult.TransactionResultStatus.SUCCESS, result.resultStatus);
            Assert.assertEquals(services.size(), result.getServices().size());
            latch.countDown();
        });
        latch.await(1, TimeUnit.SECONDS);
    }
}
