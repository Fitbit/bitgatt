/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.btcopies.BluetoothGattCharacteristicCopy;
import com.fitbit.bluetooth.fbgatt.btcopies.BluetoothGattDescriptorCopy;
import com.fitbit.bluetooth.fbgatt.tx.mocks.AddGattServerServiceMockTransaction;
import com.fitbit.bluetooth.fbgatt.tx.mocks.SendGattServerResponseMockTransaction;
import com.fitbit.bluetooth.fbgatt.util.BluetoothManagerProvider;
import com.fitbit.bluetooth.fbgatt.util.BluetoothUtils;
import com.fitbit.bluetooth.fbgatt.util.LooperWatchdog;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.matchers.Any;
import org.mockito.stubbing.Answer;

import java.util.UUID;

import timber.log.Timber;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class GattServerTests {
    private static final String MOCK_ADDRESS = "02:00:00:00:00:00";


    @Before
    public void before(){
        BluetoothUtils utilsMock = mock(BluetoothUtils.class);
        LowEnergyAclListener lowEnergyAclListenerMock = mock(LowEnergyAclListener.class);
        BluetoothAdapter adapterMock = mock(BluetoothAdapter.class);
        BluetoothRadioStatusListener bluetoothRadioStatusListenerMock = mock(BluetoothRadioStatusListener.class);
        BitGattDependencyProvider dependencyProviderMock = mock(BitGattDependencyProvider.class);
        Context mockContext = mock(Context.class);
        BluetoothManager managerMock = mock(BluetoothManager.class);
        BluetoothManagerProvider mockBluetoothManagerProvider = mock(BluetoothManagerProvider.class);

        when(mockContext.getSystemService(Any.class)).thenReturn(null);
        when(mockContext.getApplicationContext()).thenReturn(mockContext);
        doReturn(bluetoothRadioStatusListenerMock).when(dependencyProviderMock).getNewBluetoothRadioStatusListener(mockContext, false);
        doReturn(utilsMock).when(dependencyProviderMock).getBluetoothUtils();
        doReturn(lowEnergyAclListenerMock).when(dependencyProviderMock).getNewLowEnergyAclListener();
        doReturn(true).when(utilsMock).isBluetoothEnabled(mockContext);
        doReturn(mockBluetoothManagerProvider).when(dependencyProviderMock).getBluetoothManagerProvider();
        doReturn(managerMock).when(mockBluetoothManagerProvider).get(mockContext);

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
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(MOCK_ADDRESS, "fooDevice", mock(BluetoothDevice.class));
        GattConnection conn = spy(new GattConnection(device, mockLooper));
        conn.setMockMode(true);
        when(conn.getMainHandler()).thenReturn(mockHandler);

        GattServerConnection serverConnection = spy(new GattServerConnection(null, mockLooper));
        serverConnection.setMockMode(true);
        when(serverConnection.getMainHandler()).thenReturn(mockHandler);
        FitbitGatt.getInstance().setAsyncOperationThreadWatchdog(mock(LooperWatchdog.class));
        FitbitGatt.getInstance().setDependencyProvider(dependencyProviderMock);
        FitbitGatt.getInstance().startGattServer(mockContext);
        FitbitGatt.getInstance().putConnectionIntoDevices(device, conn);
        FitbitGatt.getInstance().setGattServerConnection(serverConnection);
    }

    @Test
    public void addGattServiceToServerTest() {
        // Context of the app under test.
        BluetoothGattService service = new BluetoothGattService(UUID.randomUUID(), BluetoothGattService.SERVICE_TYPE_PRIMARY);
        AddGattServerServiceMockTransaction addServiceTransaction = new AddGattServerServiceMockTransaction(FitbitGatt.getInstance().getServer(), GattState.ADD_SERVICE_SUCCESS, service, false);
        GattServerConnection serverConn = FitbitGatt.getInstance().getServer();
        serverConn.runTx(addServiceTransaction, result -> Assert.assertEquals(result.resultStatus, TransactionResult.TransactionResultStatus.SUCCESS));
    }

    @Test
    public void shouldSendResponseTest() {
        GattServerConnection serverConn = FitbitGatt.getInstance().getServer();
        serverConn.setState(GattState.IDLE);
        FitbitBluetoothDevice fitbitDevice = new FitbitBluetoothDevice(MOCK_ADDRESS, "fooDevice", mock(BluetoothDevice.class));
        final GattServerListener gattServerListener = new GattServerListener() {
            @Override
            public void onServerConnectionStateChange(BluetoothDevice device, int status, int newState) {

            }

            @Override
            public void onServerServiceAdded(int status, BluetoothGattService service) {

            }

            @Override
            public void onServerCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristicCopy characteristic) {

            }

            @Override
            public void onServerCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristicCopy characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                if(requestId == 17) {
                    SendGattServerResponseMockTransaction sendMockResponse =
                            new SendGattServerResponseMockTransaction(serverConn,
                                    GattState.SEND_SERVER_RESPONSE_SUCCESS, fitbitDevice,
                                    requestId, BluetoothGatt.GATT_SUCCESS, offset, value,
                                    false);
                    serverConn.runTx(sendMockResponse, result1 -> {
                        Timber.d("Result state :%s", result1.resultStatus.name());
                        Assert.assertEquals(result1.resultStatus, TransactionResult.TransactionResultStatus.SUCCESS);
                    });
                }
            }

            @Override
            public void onServerDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptorCopy descriptor) {

            }

            @Override
            public void onServerDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptorCopy descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {

            }

            @Override
            public void onServerExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {

            }

            @Override
            public void onServerNotificationSent(BluetoothDevice device, int status) {

            }

            @Override
            public void onServerMtuChanged(BluetoothDevice device, int mtu) {

            }

            @Override
            public void onServerPhyUpdate(BluetoothDevice device, int txPhy, int rxPhy, int status) {

            }

            @Override
            public void onServerPhyRead(BluetoothDevice device, int txPhy, int rxPhy, int status) {

            }
        };
        FitbitGatt.getInstance().registerGattServerListener(gattServerListener);
        final byte[] fakeData = new byte[]{'a','b','c','d','e','f','g','h','i', 'j'};
        BluetoothGattCharacteristicCopy characteristic =
                new BluetoothGattCharacteristicCopy(UUID.randomUUID(),
                        BluetoothGattCharacteristicCopy.PROPERTY_READ,
                        BluetoothGattCharacteristicCopy.PERMISSION_READ | BluetoothGattCharacteristicCopy.PERMISSION_READ_ENCRYPTED);
        characteristic.setValue(fakeData);
        gattServerListener.onServerCharacteristicWriteRequest(null, 17, characteristic, false, true, 0, fakeData);
        FitbitGatt.getInstance().unregisterGattServerListener(gattServerListener);
    }

    @Test
    public void ensureNpeHandlingServerNoListenersSendResponse() {
        Looper mockLooper = mock(Looper.class);
        Context mockContext = mock(Context.class);
        when(mockContext.getMainLooper()).thenReturn(mockLooper);
        BluetoothDevice mockDevice = mock(BluetoothDevice.class);
        BluetoothGattServer mockServer = mock(BluetoothGattServer.class);
        GattServerConnection mockServerConnection = mock(GattServerConnection.class);
        when(mockServer.sendResponse(any(BluetoothDevice.class), any(Integer.class), any(Integer.class), any(Integer.class), any(byte[].class))).thenThrow(new NullPointerException("Parcel read exception"));
        when(mockServerConnection.getServer()).thenReturn(mockServer);
        GattServerCallback serverCallback = new GattServerCallback();
        NullPointerException exception = null;
        try {
            serverCallback.returnErrorToRemoteClient(mockServerConnection, mockDevice, 0, 1);
        }catch(NullPointerException e) {
            exception = e;
        }
        Assert.assertNull(exception);
    }
}
