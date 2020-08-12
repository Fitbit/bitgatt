/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.exception.BitGattStartException;
import com.fitbit.bluetooth.fbgatt.tx.mocks.CloseGattMockTransaction;
import com.fitbit.bluetooth.fbgatt.tx.mocks.GattClientDiscoverMockServicesTransaction;
import com.fitbit.bluetooth.fbgatt.tx.mocks.GattConnectMockTransaction;
import com.fitbit.bluetooth.fbgatt.tx.mocks.GattDisconnectMockTransaction;
import com.fitbit.bluetooth.fbgatt.tx.mocks.GattServerConnectMockTransaction;
import com.fitbit.bluetooth.fbgatt.tx.mocks.GattServerDisconnectMockTransaction;
import com.fitbit.bluetooth.fbgatt.util.NoOpGattCallback;

import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Looper;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import androidx.test.platform.app.InstrumentationRegistry;
import timber.log.Timber;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;


public class GattConnectDisconnectTests {

    private static final String MOCK_ADDRESS = "02:00:00:00:00:00";
    private static final String MOCK_ADDRESS_2 = "04:00:00:00:00:00";
    private static final String MOCK_ADDRESS_3 = "1D:00:00:00:00:00";
    private Context mockContext;

    @Before
    public void before() {
        this.mockContext = InstrumentationRegistry.getInstrumentation().getContext();
    }

    @After
    public void after() {
        FitbitGatt.getInstance().shutdown();
        FitbitGatt.setInstance(null);
    }

    @Test
    public void testConnect() throws Exception {
        // started
        FitbitGatt.getInstance().startGattClient(mockContext);
        final TransactionResult[] resultTx = new TransactionResult[1];
        Assert.assertTrue(FitbitGatt.getInstance().isInitialized());
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
                resultTx[0] = result;
                cdl.countDown();
            });
        } else {
            cdl.countDown();
            Assert.fail();
        }
        cdl.await(20, TimeUnit.SECONDS);
        assertEquals(TransactionResult.TransactionResultStatus.SUCCESS, resultTx[0].resultStatus);
    }

    @Test
    public void connectByInstantiatingAConnectionWhenOneExists() throws Exception {
        // started
        FitbitGatt.getInstance().startGattClient(mockContext);
        Assert.assertTrue(FitbitGatt.getInstance().isInitialized());
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
        FitbitGatt.getInstance().startGattClient(mockContext);
        Assert.assertTrue(FitbitGatt.getInstance().isInitialized());
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
        FitbitGatt.getInstance().startGattClient(mockContext);
        Assert.assertTrue(FitbitGatt.getInstance().isInitialized());
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
        FitbitGatt.getInstance().startGattClient(mockContext);
        Assert.assertTrue(FitbitGatt.getInstance().isInitialized());
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
        FitbitGatt.getInstance().startGattServer(mockContext);
        Assert.assertTrue(FitbitGatt.getInstance().isInitialized());
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(MOCK_ADDRESS, "fooDevice");
        GattServerConnection connection = new GattServerConnection(null, Looper.getMainLooper());
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
        FitbitGatt.getInstance().startGattClient(mockContext);
        Assert.assertTrue(FitbitGatt.getInstance().isInitialized());
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
        FitbitGatt.getInstance().startGattClient(mockContext);
        Assert.assertTrue(FitbitGatt.getInstance().isInitialized());
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(MOCK_ADDRESS, "fooDevice");
        GattConnection connection = FitbitGatt.getInstance().getConnection(device);
        final TransactionResult[] resultTx = new TransactionResult[1];
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
            resultTx[0] = nuResult;
            latch.countDown();
        });
        latch.await(2, TimeUnit.SECONDS);
        assertEquals(GattState.DISCONNECTED, resultTx[0].getResultState());
    }

    @Test
    public void testIsConnectedWhenStateIsDisconnecting() {
        // started
        FitbitGatt.getInstance().startGattClient(mockContext);
        Assert.assertTrue(FitbitGatt.getInstance().isInitialized());
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
    @Ignore("flakey")
    public void testIsConnectedWhenStateIsConnecting() {
        // started
        FitbitGatt.getInstance().startGattClient(mockContext);
        Assert.assertTrue(FitbitGatt.getInstance().isInitialized());
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
        CountDownLatch cdl = new CountDownLatch(1);
        final TransactionResult[] resultTx = new TransactionResult[1];
        NoOpGattCallback cb = new NoOpGattCallback() {
            @Override
            public void onGattServerStarted(GattServerConnection serverConnection) {
                super.onGattServerStarted(serverConnection);
                serverConnection.setMockMode(true);
                serverConnection.setState(GattState.IDLE);
                FitbitBluetoothDevice device = new FitbitBluetoothDevice(MOCK_ADDRESS, "fooDevice");
                GattServerDisconnectMockTransaction connectTransaction = new GattServerDisconnectMockTransaction(serverConnection, GattState.DISCONNECTED, device, false);
                serverConnection.runTx(connectTransaction, result -> {
                    resultTx[0] = result;
                    cdl.countDown();
                });
            }
        };
        FitbitGatt.getInstance().registerGattEventListener(cb);
        FitbitGatt.getInstance().startGattServer(mockContext);
        Assert.assertTrue(FitbitGatt.getInstance().isInitialized());
        cdl.await(3, TimeUnit.SECONDS);
        assertEquals(TransactionResult.TransactionResultStatus.SUCCESS, resultTx[0].resultStatus);
        assertEquals(GattState.DISCONNECTED, FitbitGatt.getInstance().getServer().getGattState());
    }

    @Test
    // skip for now, we can't really reliably connect
    public void connectToScannedDevice() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        NoOpGattCallback cb = new NoOpGattCallback() {
            @Override
            public void onGattClientStarted() {
                super.onGattClientStarted();
                FitbitBluetoothDevice device = new FitbitBluetoothDevice(MOCK_ADDRESS, "fooDevice");
                FitbitGatt.getInstance().connectToScannedDevice(device, true, result -> {
                    Assert.assertEquals(TransactionResult.TransactionResultStatus.SUCCESS, result.resultStatus);
                    latch.countDown();
                });
            }

            @Override
            public void onGattClientStartError(BitGattStartException error) {
                super.onGattClientStartError(error);
                fail("Gatt Client Error start " + error.getMessage());
            }
        };
        // started
        FitbitGatt.getInstance().startGattClient(mockContext);
        Assert.assertTrue(FitbitGatt.getInstance().isInitialized());
        latch.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void filterConnectedDevices() {
        // started
        FitbitGatt.getInstance().startGattClient(mockContext);
        Assert.assertTrue(FitbitGatt.getInstance().isInitialized());
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
        FitbitGatt.getInstance().startGattClient(mockContext);
        Assert.assertTrue(FitbitGatt.getInstance().isInitialized());
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
        FitbitGatt.getInstance().startGattClient(mockContext);
        UUID serviceUuidOne = UUID.randomUUID();
        UUID serviceUuidTwo = UUID.randomUUID();
        final TransactionResult[] resultTx = new TransactionResult[1];
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
            resultTx[0] = result;
            latch.countDown();
        });
        latch.await(1, TimeUnit.SECONDS);
        assertEquals(TransactionResult.TransactionResultStatus.SUCCESS, resultTx[0].resultStatus);
        assertEquals(services.size(), resultTx[0].getServices().size());
    }

    @Test
    public void testAddConnectedDevice() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        FitbitGatt.FitbitGattCallback cb = new NoOpGattCallback() {
            @Override
            public void onBluetoothPeripheralDiscovered(GattConnection connection) {
                latch.countDown();
            }
        };

        // started
        FitbitGatt.getInstance().registerGattEventListener(cb);
        FitbitGatt.getInstance().startGattClient(mockContext);
        Assert.assertTrue(FitbitGatt.getInstance().isInitialized());
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(MOCK_ADDRESS, "fooDevice");
        GattConnection connection = FitbitGatt.getInstance().getConnection(device);
        if (connection == null) {
            FitbitGatt
                .getInstance()
                .addConnectedDevice(device.device);
        }

        latch.await(10, TimeUnit.SECONDS);

        assertEquals(0, latch.getCount());
        connection = FitbitGatt.getInstance().getConnection(device);
        assertNotNull(connection);
    }
}
