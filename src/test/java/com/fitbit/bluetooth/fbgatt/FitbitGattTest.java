/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.exception.BluetoothNotEnabledException;
import com.fitbit.bluetooth.fbgatt.exception.MissingGattServerErrorException;
import com.fitbit.bluetooth.fbgatt.tx.ClearServerServicesTransaction;
import com.fitbit.bluetooth.fbgatt.util.BluetoothManagerProvider;
import com.fitbit.bluetooth.fbgatt.util.BluetoothUtils;
import com.fitbit.bluetooth.fbgatt.util.LooperWatchdog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Responsible for testing the {@link FitbitGatt} api
 *
 * Created by ilepadatescu on 09/20/2019
 */
@RunWith(JUnit4.class)
public class FitbitGattTest {
    private BluetoothRadioStatusListener bluetoothRadioStatusListenerMock = mock(BluetoothRadioStatusListener.class);
    private BluetoothManager managerMock = mock(BluetoothManager.class);
    private BluetoothAdapter adapterMock = mock(BluetoothAdapter.class);
    private Handler fitbitGattAsyncOperationHandlerMock = mock(Handler.class);
    private AlwaysConnectedScanner alwaysConnectedScannerMock = mock(AlwaysConnectedScanner.class);
    private Looper looperMock = mock(Looper.class);
    private Context contextMock = mock(Context.class);
    private BluetoothUtils utilsMock = mock(BluetoothUtils.class);
    private BluetoothManagerProvider mockBluetoothManagerProvider = mock(BluetoothManagerProvider.class);
    private Handler connectionCleanUpHandler = mock(Handler.class);
    private LowEnergyAclListener lowEnergyAclListenerMock = mock(LowEnergyAclListener.class);
    private PeripheralScanner scannerMock = mock(PeripheralScanner.class);
    private BitGattDependencyProvider dependencyProviderMock = mock(BitGattDependencyProvider.class);
    private PendingIntent scanIntentMock = mock(PendingIntent.class);
    private FitbitGatt fitbitGatt = new FitbitGatt(
        alwaysConnectedScannerMock,
        fitbitGattAsyncOperationHandlerMock,
        connectionCleanUpHandler,
        mock(LooperWatchdog.class));

    @Before
    public void before() {
        doReturn(contextMock).when(contextMock).getApplicationContext();
        doReturn(looperMock).when(contextMock).getMainLooper();
        doReturn(adapterMock).when(utilsMock).getBluetoothAdapter(contextMock);
        doReturn(true).when(utilsMock).isBluetoothEnabled(contextMock);
        doReturn(adapterMock).when(managerMock).getAdapter();


        doReturn(scanIntentMock).when(dependencyProviderMock).getNewScanPendingIntent(eq(contextMock), any());
        doReturn(bluetoothRadioStatusListenerMock).when(dependencyProviderMock).getNewBluetoothRadioStatusListener(contextMock, false);
        doReturn(utilsMock).when(dependencyProviderMock).getBluetoothUtils();
        doReturn(mockBluetoothManagerProvider).when(dependencyProviderMock).getBluetoothManagerProvider();
        doReturn(managerMock).when(mockBluetoothManagerProvider).get(contextMock);
        doReturn(lowEnergyAclListenerMock).when(dependencyProviderMock).getNewLowEnergyAclListener();
        fitbitGatt.setDependencyProvider(dependencyProviderMock);
        FitbitGatt.setInstance(fitbitGatt);
    }

    @After
    public void after() {
        FitbitGatt.getInstance().shutdown();
        FitbitGatt.setInstance(null);
    }

    @Test
    public void testSetScanSettings() {
        ScanSettings mock = mock(ScanSettings.class);
        fitbitGatt.setPeripheralScanner(scannerMock);

        fitbitGatt.setScanSettings(mock);

        verify(scannerMock).setScanSettings(mock);
    }


    @Test
    public void testGattStartWithBluetoothOn() {
        FitbitGatt.FitbitGattCallback cb = mock(FitbitGatt.FitbitGattCallback.class);

        fitbitGatt.registerGattEventListener(cb);
        fitbitGatt.startGattClient(contextMock);

        verify(cb).onGattClientStarted();
        verify(contextMock).getApplicationContext();
        verify(bluetoothRadioStatusListenerMock).startListening();
        verify(bluetoothRadioStatusListenerMock).setListener(fitbitGatt);
        assertTrue(fitbitGatt.isInitialized());
        verifyNoMoreInteractions(cb);
    }

    @Test
    public void testGattClientStartWithBluetoothOff() {
        doReturn(false).when(utilsMock).isBluetoothEnabled(contextMock);

        FitbitGatt.FitbitGattCallback cb = mock(FitbitGatt.FitbitGattCallback.class);
        fitbitGatt.registerGattEventListener(cb);
        fitbitGatt.startGattClient(contextMock);

        verify(cb).onGattClientStartError(any(BluetoothNotEnabledException.class));
        verify(contextMock).getApplicationContext();
        verify(lowEnergyAclListenerMock, never()).register(contextMock);
        assertTrue(fitbitGatt.isInitialized());
        verifyNoMoreInteractions(cb);
    }

    @Test
    public void startGattServerWithBitgattStarted() {
        FitbitGatt.FitbitGattCallback cb = mock(FitbitGatt.FitbitGattCallback.class);
        fitbitGatt.registerGattEventListener(cb);
        fitbitGatt.setStarted(true);
        fitbitGatt.setAppContext(contextMock);
        fitbitGatt.startGattServer(contextMock);

        verify(cb, never()).onGattServerStartError(any());
        verify(fitbitGattAsyncOperationHandlerMock).post(any());
        verifyNoMoreInteractions(cb);
    }


    @Test
    public void startGattServerWithMissingBluetoothManager() {
        doReturn(null).when(mockBluetoothManagerProvider).get(contextMock);

        FitbitGatt.FitbitGattCallback cb = mock(FitbitGatt.FitbitGattCallback.class);
        fitbitGatt.registerGattEventListener(cb);
        fitbitGatt.startGattServer(contextMock);

        verify(cb, times(1)).onGattServerStartError(any(MissingGattServerErrorException.class));
        verifyNoMoreInteractions(cb);
    }

    @Test
    public void openServerGattCallbackFailedStart() {
        List<BluetoothGattService> services = Collections.emptyList();

        FitbitGatt.FitbitGattCallback cb = mock(FitbitGatt.FitbitGattCallback.class);
        fitbitGatt.registerGattEventListener(cb);

        FitbitGatt.OpenGattServerCallback opencb = fitbitGatt.getOpenGattServerCallback(services);
        opencb.onGattServerStatus(false);

        verify(cb, times(1)).onGattServerStartError(any(MissingGattServerErrorException.class));
        verifyNoMoreInteractions(cb);
    }

    @Test
    public void openServerGattCallbackSuccessAndAddServices() {
        BluetoothGattService gattService1 = mock(BluetoothGattService.class);
        BluetoothGattService gattService2 = mock(BluetoothGattService.class);
        GattServerConnection serverConnection = mock(GattServerConnection.class);
        List<BluetoothGattService> services = new ArrayList<>();
        services.add(gattService1);
        services.add(gattService2);
        FitbitGatt.FitbitGattCallback cb = mock(FitbitGatt.FitbitGattCallback.class);

        fitbitGatt.registerGattEventListener(cb);
        fitbitGatt.setAppContext(contextMock);
        fitbitGatt.setGattServerConnection(serverConnection);
        verify(cb, times(1)).onGattServerStarted(serverConnection);

        FitbitGatt.OpenGattServerCallback opencb = fitbitGatt.getOpenGattServerCallback(services);
        opencb.onGattServerStatus(true);


        verify(serverConnection).runTx(any(CompositeServerTransaction.class), any());
        verifyNoMoreInteractions(cb);
    }

    @Test
    public void startingTheGattServerWithoutIssues() {
        BluetoothGattServer serverMock = mock(BluetoothGattServer.class);
        doReturn(serverMock).when(managerMock).openGattServer(eq(contextMock), any());
        FitbitGatt.OpenGattServerCallback openServerCB = mock(FitbitGatt.OpenGattServerCallback.class);
        FitbitGatt.FitbitGattCallback cb = mock(FitbitGatt.FitbitGattCallback.class);

        fitbitGatt.registerGattEventListener(cb);
        Runnable startGattServer = fitbitGatt.tryAndStartGattServer(contextMock, openServerCB, managerMock);
        startGattServer.run();

        verify(cb, times(1)).onGattServerStarted(any());
        verify(serverMock, never()).clearServices();
        verify(openServerCB, times(1)).onGattServerStatus(true);
        verifyNoMoreInteractions(cb);
        verifyNoMoreInteractions(openServerCB);
        assertSame(GattState.IDLE, fitbitGatt.getServer().getGattState());
    }

    @Test
    public void startingTheGattServerWithCachedServices() {
        BluetoothGattServer serverMock = mock(BluetoothGattServer.class);
        ArrayList<BluetoothGattService> services = new ArrayList<BluetoothGattService>();
        services.add(mock(BluetoothGattService.class));
        doReturn(services).when(serverMock).getServices();
        doReturn(serverMock).when(managerMock).openGattServer(eq(contextMock), any());
        FitbitGatt.OpenGattServerCallback openServerCB = mock(FitbitGatt.OpenGattServerCallback.class);
        FitbitGatt.FitbitGattCallback cb = mock(FitbitGatt.FitbitGattCallback.class);

        fitbitGatt.registerGattEventListener(cb);
        Runnable startGattServer = fitbitGatt.tryAndStartGattServer(contextMock, openServerCB, managerMock);
        startGattServer.run();

        verify(cb, times(1)).onGattServerStarted(any());
        verify(serverMock).clearServices();
        verify(openServerCB, times(1)).onGattServerStatus(true);
        verifyNoMoreInteractions(cb);
        verifyNoMoreInteractions(openServerCB);
        assertSame(GattState.IDLE, fitbitGatt.getServer().getGattState());
    }


    @Test
    public void startingTheGattServerOpenGattIsNull() {
        doReturn(null).when(managerMock).openGattServer(eq(contextMock), any());
        FitbitGatt.OpenGattServerCallback openServerCB = mock(FitbitGatt.OpenGattServerCallback.class);
        FitbitGatt.FitbitGattCallback cb = mock(FitbitGatt.FitbitGattCallback.class);

        fitbitGatt.registerGattEventListener(cb);
        Runnable startGattServer = fitbitGatt.tryAndStartGattServer(contextMock, openServerCB, managerMock);
        startGattServer.run();

        verify(cb, never()).onGattServerStarted(any());
        verify(openServerCB, times(1)).onGattServerStatus(false);
        verifyNoMoreInteractions(cb);
        verifyNoMoreInteractions(openServerCB);
    }


    @Test
    public void startingTheGattServerWithAPreviousConnectionExisting() {
        BluetoothGattServer serverMock = mock(BluetoothGattServer.class);
        doReturn(serverMock).when(managerMock).openGattServer(eq(contextMock), any());
        FitbitGatt.OpenGattServerCallback openServerCB = mock(FitbitGatt.OpenGattServerCallback.class);
        FitbitGatt.FitbitGattCallback cb = mock(FitbitGatt.FitbitGattCallback.class);
        GattServerConnection prevConnection = mock(GattServerConnection.class);
        GattServerConnection newConnection = mock(GattServerConnection.class);
        when(newConnection.getGattState()).thenReturn(GattState.IDLE);
        doAnswer(invocation -> {
            TransactionResult tr = new TransactionResult.Builder()
                .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS).build();
            GattTransactionCallback gattServerCallback = invocation.getArgument(1);
            gattServerCallback.onTransactionComplete(tr);
            return tr;
        }).when(newConnection).runTx(any(ClearServerServicesTransaction.class), any());

        fitbitGatt.setGattServerConnection(prevConnection);
        fitbitGatt.setAppContext(contextMock);
        fitbitGatt.registerGattEventListener(cb);

        Runnable startGattServer = fitbitGatt.tryAndStartGattServer(contextMock, openServerCB, managerMock);
        startGattServer.run();

        verify(prevConnection).close();
        verify(cb, times(1)).onGattServerStarted(any());
        verify(openServerCB, times(1)).onGattServerStatus(true);
        verifyNoMoreInteractions(cb);
        verifyNoMoreInteractions(openServerCB);
        assertNotSame(prevConnection, fitbitGatt.getServer());
        assertSame(GattState.IDLE, fitbitGatt.getServer().getGattState());
    }

    @Test
    public void onBluetoothOnAndNotStartedWeDoNothing() {
        FitbitGatt.FitbitGattCallback cb = mock(FitbitGatt.FitbitGattCallback.class);
        fitbitGatt.registerGattEventListener(cb);
        fitbitGatt.setStarted(false);
        fitbitGatt.bluetoothOn();
        verifyNoMoreInteractions(cb);
    }

    @Test
    public void onBluetoothOnAndIfJustBitgattStartedWeCallOnlyOnBlueoothOn() {
        FitbitGatt.FitbitGattCallback cb = mock(FitbitGatt.FitbitGattCallback.class);
        fitbitGatt.registerGattEventListener(cb);
        fitbitGatt.setStarted(true);
        fitbitGatt.setAppContext(contextMock);
        fitbitGatt.bluetoothOn();
        verify(cb).onBluetoothOn();
        verifyNoMoreInteractions(cb);
    }


    @Test
    public void onBluetoothOnIfGattServerWasStartedWeStartTheServerBack() {
        FitbitGatt.FitbitGattCallback cb = mock(FitbitGatt.FitbitGattCallback.class);
        fitbitGatt.registerGattEventListener(cb);
        fitbitGatt.setAppContext(contextMock);
        fitbitGatt.setStarted(true);
        fitbitGatt.setGattServerStarted(true);

        fitbitGatt.bluetoothOn();

        verify(cb, never()).onGattServerStartError(any());
        verify(fitbitGattAsyncOperationHandlerMock).post(any());
        verify(cb).onBluetoothOn();
        verifyNoMoreInteractions(cb);
    }

    @Test
    public void onBluetoothOnIfGattClientStartedResetGattConnectionsToIdle() {
        ConcurrentHashMap<FitbitBluetoothDevice, GattConnection> map = new ConcurrentHashMap<FitbitBluetoothDevice, GattConnection>();
        FitbitBluetoothDevice device = mock(FitbitBluetoothDevice.class);
        GattConnection connection = mock(GattConnection.class);
        map.put(device, connection);
        FitbitGatt.FitbitGattCallback cb = mock(FitbitGatt.FitbitGattCallback.class);
        fitbitGatt.registerGattEventListener(cb);
        fitbitGatt.setAppContext(contextMock);
        fitbitGatt.setStarted(true);
        fitbitGatt.setGattClientStarted(true);
        fitbitGatt.setConnectionMap(map);

        fitbitGatt.bluetoothOn();

        verify(cb, never()).onGattServerStartError(any());
        verify(connection).setState(GattState.DISCONNECTED);
        verify(cb).onBluetoothOn();
        verifyNoMoreInteractions(cb);
    }

    @Test
    public void addingANewConnection() {
        FitbitGatt.FitbitGattCallback cb = mock(FitbitGatt.FitbitGattCallback.class);
        GattConnection connection = mock(GattConnection.class);
        FitbitBluetoothDevice fbDevice = mock(FitbitBluetoothDevice.class);

        fitbitGatt.registerGattEventListener(cb);
        fitbitGatt.putConnectionIntoDevices(fbDevice, connection);

        verify(cb, times(1)).onBluetoothPeripheralDiscovered(connection);
        verifyNoMoreInteractions(cb);
    }

    @Test
    public void testConnectionInvalidation() {
        FitbitGatt.FitbitGattCallback cb = mock(FitbitGatt.FitbitGattCallback.class);
        GattConnection connection = mock(GattConnection.class);
        doReturn(false).when(connection).isConnected();
        FitbitBluetoothDevice fbDevice = mock(FitbitBluetoothDevice.class);
        ConcurrentHashMap<FitbitBluetoothDevice, GattConnection> map = new ConcurrentHashMap<FitbitBluetoothDevice, GattConnection>();
        map.put(fbDevice, connection);

        fitbitGatt.registerGattEventListener(cb);
        fitbitGatt.setConnectionMap(map);
        fitbitGatt.setAppContext(contextMock);
        fitbitGatt.doDecrementAndInvalidateClosedConnections();

        verify(cb, times(1)).onBluetoothPeripheralDisconnected(connection);
        verifyNoMoreInteractions(cb);
    }


    @Test
    public void testConnectionDoDisconnectConnectedDevices() {
        FitbitGatt.FitbitGattCallback cb = mock(FitbitGatt.FitbitGattCallback.class);
        GattConnection connection = mock(GattConnection.class);
        doReturn(true).when(connection).isConnected();
        FitbitBluetoothDevice fbDevice = mock(FitbitBluetoothDevice.class);
        ConcurrentHashMap<FitbitBluetoothDevice, GattConnection> map = new ConcurrentHashMap<FitbitBluetoothDevice, GattConnection>();
        map.put(fbDevice, connection);

        fitbitGatt.registerGattEventListener(cb);
        fitbitGatt.setConnectionMap(map);
        fitbitGatt.setAppContext(contextMock);
        fitbitGatt.doDecrementAndInvalidateClosedConnections();

        verifyNoMoreInteractions(cb);
    }

    @Test
    public void testAddingNewConnectedDevice() {
        ConcurrentHashMap<FitbitBluetoothDevice, GattConnection> map = new ConcurrentHashMap<FitbitBluetoothDevice, GattConnection>();
        FitbitBluetoothDevice fbDevice = mock(FitbitBluetoothDevice.class);

        FitbitGatt.FitbitGattCallback cb = mock(FitbitGatt.FitbitGattCallback.class);
        fitbitGatt.registerGattEventListener(cb);
        fitbitGatt.setAppContext(contextMock);
        fitbitGatt.setStarted(true);
        fitbitGatt.setGattClientStarted(true);
        fitbitGatt.setConnectionMap(map);

        fitbitGatt.addConnectedDeviceToConnectionMap(contextMock, fbDevice);

        verify(cb, times(1)).onBluetoothPeripheralDiscovered(any());
        verifyNoMoreInteractions(cb);
    }

    @Test
    public void failToAddExistingConnectedDevice() {
        ConcurrentHashMap<FitbitBluetoothDevice, GattConnection> map = new ConcurrentHashMap<FitbitBluetoothDevice, GattConnection>();
        FitbitBluetoothDevice fbDevice = mock(FitbitBluetoothDevice.class);
        GattConnection connection = mock(GattConnection.class);
        map.put(fbDevice, connection);

        FitbitGatt.FitbitGattCallback cb = mock(FitbitGatt.FitbitGattCallback.class);
        fitbitGatt.registerGattEventListener(cb);
        fitbitGatt.setAppContext(contextMock);
        fitbitGatt.setStarted(true);
        fitbitGatt.setGattClientStarted(true);
        fitbitGatt.setConnectionMap(map);

        fitbitGatt.addConnectedDeviceToConnectionMap(contextMock, fbDevice);

        verifyZeroInteractions(cb);
    }

    @Test
    public void failToAddConnectedDeviceIfAdapterIsNull() {
        ConcurrentHashMap<FitbitBluetoothDevice, GattConnection> map = new ConcurrentHashMap<FitbitBluetoothDevice, GattConnection>();
        FitbitBluetoothDevice fbDevice = mock(FitbitBluetoothDevice.class);
        doReturn(null).when(utilsMock).getBluetoothAdapter(contextMock);

        FitbitGatt.FitbitGattCallback cb = mock(FitbitGatt.FitbitGattCallback.class);
        fitbitGatt.registerGattEventListener(cb);
        fitbitGatt.setAppContext(contextMock);
        fitbitGatt.setStarted(true);
        fitbitGatt.setGattClientStarted(true);
        fitbitGatt.setConnectionMap(map);

        fitbitGatt.addConnectedDeviceToConnectionMap(contextMock, fbDevice);

        verifyZeroInteractions(cb);
    }

    @Test
    public void testStartPeriodicScanAlwaysOnDisabled() {
        doReturn(false).when(alwaysConnectedScannerMock).isAlwaysConnectedScannerEnabled();

        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.startPeriodicScan(contextMock);

        verify(scannerMock).startPeriodicScan(contextMock);
        verifyNoMoreInteractions(scannerMock);
    }

    @Test
    public void testStartPeriodicScanAlwaysOnEnabled() {
        doReturn(true).when(alwaysConnectedScannerMock).isAlwaysConnectedScannerEnabled();

        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.startPeriodicScan(contextMock);

        verifyZeroInteractions(scannerMock);
    }


    @Test
    public void testCancelScanAlwaysOnDisabled() {
        doReturn(false).when(alwaysConnectedScannerMock).isAlwaysConnectedScannerEnabled();

        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.cancelScan(contextMock);

        verify(scannerMock).cancelScan(contextMock);
        verifyNoMoreInteractions(scannerMock);
    }

    @Test
    public void testCancelScanAlwaysOnEnabled() {
        doReturn(true).when(alwaysConnectedScannerMock).isAlwaysConnectedScannerEnabled();

        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.startPeriodicScan(contextMock);

        verifyZeroInteractions(scannerMock);
    }


    @Test
    public void testCancelPeriodicalScanScanAlwaysOnDisabled() {
        doReturn(false).when(alwaysConnectedScannerMock).isAlwaysConnectedScannerEnabled();

        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.cancelPeriodicalScan(contextMock);

        verify(scannerMock).cancelPeriodicalScan(contextMock);
        verifyNoMoreInteractions(scannerMock);
    }

    @Test
    public void testCancelPeriodicalScanScanAlwaysOnEnabled() {
        doReturn(true).when(alwaysConnectedScannerMock).isAlwaysConnectedScannerEnabled();

        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.cancelPeriodicalScan(contextMock);

        verifyZeroInteractions(scannerMock);
    }

    @Test
    public void startBackgroundScanAlwaysOnDisabled() {
        doReturn(false).when(alwaysConnectedScannerMock).isAlwaysConnectedScannerEnabled();
        Intent intent = mock(Intent.class);
        List<String> macs = new ArrayList<>();
        fitbitGatt.setStarted(true);
        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.startBackgroundScan(contextMock, intent, macs);

        verify(scannerMock).startBackgroundScan(macs, intent, contextMock);
        verifyNoMoreInteractions(scannerMock);
    }

    @Test
    public void startBackgroundScanAlwaysOnEnabled() {
        doReturn(true).when(alwaysConnectedScannerMock).isAlwaysConnectedScannerEnabled();
        Intent intent = mock(Intent.class);
        List<String> macs = new ArrayList<>();
        fitbitGatt.setStarted(true);
        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.startBackgroundScan(contextMock, intent, macs);

        verifyZeroInteractions(scannerMock);
    }

    @Test
    public void startSystemManagedPendingIntentScanAlwaysOnEnabled() {
        doReturn(true).when(alwaysConnectedScannerMock).isAlwaysConnectedScannerEnabled();

        List<ScanFilter> filters = new ArrayList<>();
        fitbitGatt.setStarted(true);
        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.startSystemManagedPendingIntentScan(contextMock, filters);

        verifyZeroInteractions(scannerMock);
    }

    @Test
    public void startSystemManagedPendingIntentScanAlwaysOnDisabled() {
        doReturn(false).when(alwaysConnectedScannerMock).isAlwaysConnectedScannerEnabled();

        List<ScanFilter> filters = new ArrayList<>();
        fitbitGatt.setStarted(true);
        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.startSystemManagedPendingIntentScan(contextMock, filters);

        verify(scannerMock).startPendingIntentBasedBackgroundScan(filters, contextMock);
        verifyNoMoreInteractions(scannerMock);
    }

    @Test
    public void stopSystemManagedPendingIntentScanAlwaysOnEnabled() {
        doReturn(true).when(alwaysConnectedScannerMock).isAlwaysConnectedScannerEnabled();

        fitbitGatt.setStarted(true);
        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.stopSystemManagedPendingIntentScan();

        verifyZeroInteractions(scannerMock);
    }

    @Test
    public void stopSystemManagedPendingIntentScanAlwaysOnDisabled() {
        doReturn(false).when(alwaysConnectedScannerMock).isAlwaysConnectedScannerEnabled();

        fitbitGatt.setStarted(true);
        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.stopSystemManagedPendingIntentScan();

        verify(scannerMock).cancelPendingIntentBasedBackgroundScan();
        verifyNoMoreInteractions(scannerMock);
    }

    @Test
    public void stopBackgroundScanWithRegularIntentAlwaysOnDisabled() {
        doReturn(false).when(alwaysConnectedScannerMock).isAlwaysConnectedScannerEnabled();
        Intent intent = mock(Intent.class);
        fitbitGatt.setStarted(true);
        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.stopBackgroundScanWithRegularIntent(contextMock, intent);

        verify(scannerMock).stopBackgroundScan(any());
        verifyNoMoreInteractions(scannerMock);
    }

    @Test
    public void testCancelHighPriorityScanAlwaysOnEnabled() {
        doReturn(true).when(alwaysConnectedScannerMock).isAlwaysConnectedScannerEnabled();

        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.cancelHighPriorityScan(contextMock);

        verifyZeroInteractions(scannerMock);
    }


    @Test
    public void testSetDeviceNameScanFilters() {
        ArrayList<String> filters = new ArrayList<String>();
        filters.add("TEST");
        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.setDeviceNameScanFilters(filters);

        verify(scannerMock).setDeviceNameFilters(filters);
        verifyNoMoreInteractions(scannerMock);
    }

    @Test
    public void addDeviceNameScanFilter() {
        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.addDeviceNameScanFilter("TEST");

        verify(scannerMock).addDeviceNameFilter("TEST");
        verifyNoMoreInteractions(scannerMock);
    }

    @Test
    public void setScanServiceUuidFilters() {
        List<ParcelUuid> filters = new ArrayList<>();

        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.setScanServiceUuidFilters(filters);

        verify(scannerMock).setServiceUuidFilters(filters);
        verifyNoMoreInteractions(scannerMock);
    }

    @Test
    public void addScanServiceUUIDWithMaskFilter() {
        ParcelUuid service = mock(ParcelUuid.class);
        ParcelUuid mask = mock(ParcelUuid.class);
        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.addScanServiceUUIDWithMaskFilter(service, mask);

        verify(scannerMock).addServiceUUIDWithMask(service, mask);
        verifyNoMoreInteractions(scannerMock);
    }

    @Test
    public void addFilterUsingServiceData() {
        ParcelUuid service = mock(ParcelUuid.class);
        byte[] serviceData = new byte[]{};
        byte[] serviceDataMask = new byte[]{};
        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.addFilterUsingServiceData(service, serviceData, serviceDataMask);

        verify(scannerMock).addFilterUsingServiceData(service, serviceData, serviceDataMask);
        verifyNoMoreInteractions(scannerMock);
    }

    @Test
    public void addScanRssiFilter() {
        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.addScanRssiFilter(5);

        verify(scannerMock).addRssiFilter(5);
        verifyNoMoreInteractions(scannerMock);
    }

    @Test
    public void addDeviceAddressFilter() {
        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.addDeviceAddressFilter("IN:VA:LI:DM");

        verify(scannerMock).addDeviceAddressFilter("IN:VA:LI:DM");
        verifyNoMoreInteractions(scannerMock);
    }

    @Test
    public void resetScanFilters() {
        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.resetScanFilters();

        verify(scannerMock).resetFilters();
        verifyNoMoreInteractions(scannerMock);
    }

    @Test
    public void isScanning() {
        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.isScanning();

        verify(scannerMock).isScanning();
        verifyNoMoreInteractions(scannerMock);
    }

    @Test
    public void isPendingIntentScan() {
        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.isPendingIntentScanning();

        verify(scannerMock).isPendingIntentScanning();
        verifyNoMoreInteractions(scannerMock);
    }

    @Test
    public void isPeriodicalScanEnabled() {
        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.isPeriodicalScanEnabled();

        verify(scannerMock).isPeriodicalScanEnabled();
        verifyNoMoreInteractions(scannerMock);
    }
}
