/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import androidx.test.core.app.ApplicationProvider;
import com.fitbit.bluetooth.fbgatt.exception.BluetoothNotEnabledException;
import com.fitbit.bluetooth.fbgatt.exception.MissingGattServerErrorException;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import com.fitbit.bluetooth.fbgatt.util.BluetoothManagerFacade;
import com.fitbit.bluetooth.fbgatt.util.BluetoothUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.robolectric.Shadows.shadowOf;

/**
 * Responsible for testing the {@link FitbitGatt} apif
 *
 * Created by ilepadatescu on 09/20/2019
 */
@RunWith(RobolectricTestRunner.class)
@Config(
    minSdk = 21
)
public class FitbitGattTest {
    private BluetoothRadioStatusListener bluetoothRadioStatusListenerMock = mock(BluetoothRadioStatusListener.class);


    private AlwaysConnectedScanner alwaysConnectedScannerMock = mock(AlwaysConnectedScanner.class);



    private PeripheralScanner scannerMock = mock(PeripheralScanner.class);
    private BitGattDependencyProvider dependencyProviderMock = mock(BitGattDependencyProvider.class);

    private FitbitGatt fitbitGatt =  FitbitGatt.getInstance();
    private  Context context;

    @Before
    public void before() {
        context = ApplicationProvider.getApplicationContext();
        // doReturn(scanIntentMock).when(dependencyProviderMock).getNewScanPendingIntent(any(), any());
        doReturn(bluetoothRadioStatusListenerMock).when(dependencyProviderMock).getNewBluetoothRadioStatusListener(any(), eq(false));
    }

    @After
    public void after() {
        FitbitGatt.getInstance().shutdown();
        FitbitGatt.setInstance(null);
    }

    @Test
    public void testSetScanSettings() {
        ScanSettings mock = new ScanSettings.Builder().build();
        fitbitGatt.setPeripheralScanner(scannerMock);

        fitbitGatt.setScanSettings(mock);

        verify(scannerMock).setScanSettings(mock);
    }

    @Test
    public void testGattClientStartWithBluetoothOff() {

        FitbitGatt.FitbitGattCallback cb = mock(FitbitGatt.FitbitGattCallback.class);
        fitbitGatt.registerGattEventListener(cb);
        fitbitGatt.startGattClient(context);
        verify(cb).onGattClientStartError(any(BluetoothNotEnabledException.class));


        assertTrue(fitbitGatt.isInitialized());
        verifyNoMoreInteractions(cb);
    }

    @Test
    public void startGattServerWithMissingGattServer() {

        FitbitGatt.FitbitGattCallback cb = mock(FitbitGatt.FitbitGattCallback.class);
        BluetoothUtils bluetoothUtilsMock = mock(BluetoothUtils.class);
        doReturn(true).when(bluetoothUtilsMock).isBluetoothEnabled(context);
        doReturn(bluetoothUtilsMock).when(dependencyProviderMock).getBluetoothUtils();
        fitbitGatt.setDependencyProvider(dependencyProviderMock);
        fitbitGatt.registerGattEventListener(cb);
        fitbitGatt.startGattServer(context);

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
        fitbitGatt.setAppContext(context);
        fitbitGatt.setGattServerConnection(serverConnection);
        verify(cb, times(1)).onGattServerStarted(serverConnection);

        FitbitGatt.OpenGattServerCallback opencb = fitbitGatt.getOpenGattServerCallback(services);
        opencb.onGattServerStatus(true);


        verify(serverConnection).runTx(any(CompositeServerTransaction.class), any());
        verifyNoMoreInteractions(cb);
    }

    @Test
    @Ignore
    public void startingTheGattServerOpenGattIsNull() {

        FitbitGatt.OpenGattServerCallback openServerCB = mock(FitbitGatt.OpenGattServerCallback.class);
        FitbitGatt.FitbitGattCallback cb = mock(FitbitGatt.FitbitGattCallback.class);
        BluetoothManagerFacade managerFacade = spy(new BluetoothManagerFacade(context));
        GattServerCallback gattServerCallback = new GattServerCallback();
        doReturn(null).when(managerFacade).openGattServer(context, gattServerCallback);
        fitbitGatt.registerGattEventListener(cb);
        Runnable startGattServer = fitbitGatt.tryAndStartGattServer(context, openServerCB, managerFacade, gattServerCallback);
        startGattServer.run();

        verify(cb, never()).onGattServerStarted(any());
        verify(openServerCB, times(1)).onGattServerStatus(false);
        verifyNoMoreInteractions(cb);
        verifyNoMoreInteractions(openServerCB);
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
        fitbitGatt.setAppContext(context);
        fitbitGatt.bluetoothOn();
        verify(cb).onBluetoothOn();
        verifyNoMoreInteractions(cb);
    }


    @Test
    public void onBluetoothOnIfGattServerWasStartedWeStartTheServerBack() {
        FitbitGatt.FitbitGattCallback cb = mock(FitbitGatt.FitbitGattCallback.class);
        fitbitGatt.registerGattEventListener(cb);
        fitbitGatt.setAppContext(context);
        fitbitGatt.setStarted(true);
        fitbitGatt.setGattServerStarted(true);

        fitbitGatt.bluetoothOn();

        verify(cb, never()).onGattServerStartError(any());
        // verify(fitbitGattAsyncOperationHandlerMock).post(any());
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
        fitbitGatt.setAppContext(context);
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
    public void testAddingNewConnectedDevice() {
        ConcurrentHashMap<FitbitBluetoothDevice, GattConnection> map = new ConcurrentHashMap<FitbitBluetoothDevice, GattConnection>();
        FitbitBluetoothDevice fbDevice = mock(FitbitBluetoothDevice.class);

        FitbitGatt.FitbitGattCallback cb = mock(FitbitGatt.FitbitGattCallback.class);
        fitbitGatt.registerGattEventListener(cb);
        fitbitGatt.setAppContext(context);
        fitbitGatt.setStarted(true);
        fitbitGatt.setGattClientStarted(true);
        fitbitGatt.setConnectionMap(map);

        fitbitGatt.addConnectedDeviceToConnectionMap(context, fbDevice);

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
        fitbitGatt.setAppContext(context);
        fitbitGatt.setStarted(true);
        fitbitGatt.setGattClientStarted(true);
        fitbitGatt.setConnectionMap(map);

        fitbitGatt.addConnectedDeviceToConnectionMap(context, fbDevice);

        verifyNoInteractions(cb);
    }

    @Test
    public void testStartPeriodicScanAlwaysOnDisabled() {
        doReturn(false).when(alwaysConnectedScannerMock).isAlwaysConnectedScannerEnabled();

        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.startPeriodicScan(context);

        verify(scannerMock).startPeriodicScan(context);
        verifyNoMoreInteractions(scannerMock);
    }

    @Test
    public void testStartPeriodicScanAlwaysOnEnabled() {
        doReturn(true).when(alwaysConnectedScannerMock).isAlwaysConnectedScannerEnabled();

        fitbitGatt.setAlwaysConnectedScanner(alwaysConnectedScannerMock);
        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.startPeriodicScan(context);

        verifyNoInteractions(scannerMock);
    }


    @Test
    public void testCancelScanAlwaysOnDisabled() {
        doReturn(false).when(alwaysConnectedScannerMock).isAlwaysConnectedScannerEnabled();

        fitbitGatt.setAlwaysConnectedScanner(alwaysConnectedScannerMock);
        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.cancelScan(context);

        verify(scannerMock).cancelScan(context);
        verifyNoMoreInteractions(scannerMock);
    }

    @Test
    public void testCancelScanAlwaysOnEnabled() {
        doReturn(true).when(alwaysConnectedScannerMock).isAlwaysConnectedScannerEnabled();

        fitbitGatt.setAlwaysConnectedScanner(alwaysConnectedScannerMock);
        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.startPeriodicScan(context);

        verifyNoInteractions(scannerMock);
    }


    @Test
    public void testCancelPeriodicalScanScanAlwaysOnDisabled() {
        doReturn(false).when(alwaysConnectedScannerMock).isAlwaysConnectedScannerEnabled();

        fitbitGatt.setAlwaysConnectedScanner(alwaysConnectedScannerMock);
        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.cancelPeriodicalScan(context);

        verify(scannerMock).cancelPeriodicalScan(context);
        verifyNoMoreInteractions(scannerMock);
    }

    @Test
    public void testCancelPeriodicalScanScanAlwaysOnEnabled() {
        doReturn(true).when(alwaysConnectedScannerMock).isAlwaysConnectedScannerEnabled();

        fitbitGatt.setAlwaysConnectedScanner(alwaysConnectedScannerMock);
        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.cancelPeriodicalScan(context);

        verifyNoInteractions(scannerMock);
    }

    @Test
    public void startBackgroundScanAlwaysOnDisabled() {
        doReturn(false).when(alwaysConnectedScannerMock).isAlwaysConnectedScannerEnabled();
        Intent intent = mock(Intent.class);
        List<String> macs = new ArrayList<>();
        fitbitGatt.setStarted(true);
        fitbitGatt.setAlwaysConnectedScanner(alwaysConnectedScannerMock);
        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.startBackgroundScan(context, intent, macs);

        verify(scannerMock).startBackgroundScan(macs, intent, context);
        verifyNoMoreInteractions(scannerMock);
    }

    @Test
    public void startBackgroundScanAlwaysOnEnabled() {
        doReturn(true).when(alwaysConnectedScannerMock).isAlwaysConnectedScannerEnabled();
        Intent intent = mock(Intent.class);
        List<String> macs = new ArrayList<>();
        fitbitGatt.setStarted(true);
        fitbitGatt.setAlwaysConnectedScanner(alwaysConnectedScannerMock);
        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.startBackgroundScan(context, intent, macs);

        verifyNoInteractions(scannerMock);
    }

    @Test
    public void startSystemManagedPendingIntentScanAlwaysOnEnabled() {
        doReturn(true).when(alwaysConnectedScannerMock).isAlwaysConnectedScannerEnabled();

        List<ScanFilter> filters = new ArrayList<>();
        fitbitGatt.setStarted(true);
        fitbitGatt.setAlwaysConnectedScanner(alwaysConnectedScannerMock);
        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.startSystemManagedPendingIntentScan(context, filters);

        verifyNoInteractions(scannerMock);
    }

    @Test
    public void startSystemManagedPendingIntentScanAlwaysOnDisabled() {
        doReturn(false).when(alwaysConnectedScannerMock).isAlwaysConnectedScannerEnabled();

        List<ScanFilter> filters = new ArrayList<>();
        fitbitGatt.setStarted(true);
        fitbitGatt.setAlwaysConnectedScanner(alwaysConnectedScannerMock);
        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.startSystemManagedPendingIntentScan(context, filters);

        verify(scannerMock).startPendingIntentBasedBackgroundScan(filters, context);
        verifyNoMoreInteractions(scannerMock);
    }

    @Test
    public void stopSystemManagedPendingIntentScanAlwaysOnEnabled() {
        doReturn(true).when(alwaysConnectedScannerMock).isAlwaysConnectedScannerEnabled();

        fitbitGatt.setStarted(true);
        fitbitGatt.setAlwaysConnectedScanner(alwaysConnectedScannerMock);
        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.stopSystemManagedPendingIntentScan();

        verifyNoInteractions(scannerMock);
    }

    @Test
    public void stopSystemManagedPendingIntentScanAlwaysOnDisabled() {
        doReturn(false).when(alwaysConnectedScannerMock).isAlwaysConnectedScannerEnabled();

        fitbitGatt.setStarted(true);
        fitbitGatt.setAlwaysConnectedScanner(alwaysConnectedScannerMock);
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
        fitbitGatt.setAlwaysConnectedScanner(alwaysConnectedScannerMock);
        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.stopBackgroundScanWithRegularIntent(context, intent);

        verify(scannerMock).stopBackgroundScan(any());
        verifyNoMoreInteractions(scannerMock);
    }

    @Test
    public void testSetDeviceNameScanFilters() {
        ArrayList<String> filters = new ArrayList<String>();
        filters.add("TEST");
        fitbitGatt.setAlwaysConnectedScanner(alwaysConnectedScannerMock);
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
        ParcelUuid service = ParcelUuid.fromString("25b9586a-d8cf-11eb-b8bc-0242ac130003");
        ParcelUuid mask = ParcelUuid.fromString("25b9586a-d8cf-11eb-b8bc-0242ac130003");
        fitbitGatt.setPeripheralScanner(scannerMock);
        fitbitGatt.addScanServiceUUIDWithMaskFilter(service, mask);

        verify(scannerMock).addServiceUUIDWithMask(service, mask);
        verifyNoMoreInteractions(scannerMock);
    }

    @Test
    public void addFilterUsingServiceData() {
        ParcelUuid service = ParcelUuid.fromString("25b9586a-d8cf-11eb-b8bc-0242ac130003");
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
