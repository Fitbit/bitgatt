/*
 *  Copyright 2020 Fitbit, Inc. All rights reserved.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.util.GattUtils;
import com.fitbit.bluetooth.fbgatt.util.ScanFailedReason;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import org.junit.Test;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class HandleIntentBasedScanResultTest {

    private GattUtils gattUtilsMock = mock(GattUtils.class);
    private FitbitGatt fitbitGattMock = mock(FitbitGatt.class);
    private BluetoothAdapter adapterMock = mock(BluetoothAdapter.class);

    private HandleIntentBasedScanResult sut = new HandleIntentBasedScanResult(gattUtilsMock, fitbitGattMock);

    private Context contextMock = mock(Context.class);
    private Intent intentMock = mock(Intent.class);

    @Test
    public void testBTOff() {
        doReturn(adapterMock).when(gattUtilsMock).getBluetoothAdapter(contextMock);
        doReturn(false).when(adapterMock).isEnabled();

        sut.onReceive(contextMock, intentMock);

        verifyNoMoreInteractions(fitbitGattMock);
        verify(adapterMock).isEnabled();
        verifyNoMoreInteractions(intentMock);
    }

    @Test
    public void testNoAdapter() {
        doReturn(null).when(gattUtilsMock).getBluetoothAdapter(contextMock);
        doReturn(false).when(adapterMock).isEnabled();

        sut.onReceive(contextMock, intentMock);

        verifyNoMoreInteractions(fitbitGattMock);
        verifyNoMoreInteractions(intentMock);
    }

    @Test
    public void testScanResultError() {
        doReturn(adapterMock).when(gattUtilsMock).getBluetoothAdapter(contextMock);
        doReturn(true).when(adapterMock).isEnabled();
        doReturn(ScanFailedReason.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED.getCode()).when(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode());

        sut.onReceive(contextMock, intentMock);

        verify(fitbitGattMock).isInitialized();
        verify(adapterMock).isEnabled();
        verify(intentMock).getAction();
        verify(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, ScanSettings.CALLBACK_TYPE_FIRST_MATCH);
        verify(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode());
        verifyNoMoreInteractions(intentMock);
        verifyNoMoreInteractions(fitbitGattMock);
    }


    @Test
    public void testEmptyScanResults() {
        doReturn(adapterMock).when(gattUtilsMock).getBluetoothAdapter(contextMock);
        doReturn(true).when(adapterMock).isEnabled();
        doReturn(ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode()).when(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode());
        doReturn(new ArrayList<ScanResult>()).when(intentMock).getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);

        sut.onReceive(contextMock, intentMock);

        verify(fitbitGattMock).isInitialized();
        verify(adapterMock).isEnabled();
        verify(intentMock).getAction();
        verify(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, ScanSettings.CALLBACK_TYPE_FIRST_MATCH);
        verify(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode());
        verify(intentMock).getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);
        verifyNoMoreInteractions(intentMock);
        verifyNoMoreInteractions(fitbitGattMock);
    }

    @Test
    public void testNullResultSet() {
        doReturn(adapterMock).when(gattUtilsMock).getBluetoothAdapter(contextMock);
        doReturn(true).when(adapterMock).isEnabled();
        doReturn(ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode()).when(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode());
        doReturn(null).when(intentMock).getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);

        sut.onReceive(contextMock, intentMock);

        verify(fitbitGattMock).isInitialized();
        verify(adapterMock).isEnabled();
        verify(intentMock).getAction();
        verify(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, ScanSettings.CALLBACK_TYPE_FIRST_MATCH);
        verify(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode());
        verify(intentMock).getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);
        verifyNoMoreInteractions(intentMock);
        verifyNoMoreInteractions(fitbitGattMock);
    }


    @Test
    public void testNoClientCB() {
        ArrayList<ScanResult> results = new ArrayList<>();
        results.add(mock(ScanResult.class));

        doReturn(adapterMock).when(gattUtilsMock).getBluetoothAdapter(contextMock);
        doReturn(true).when(adapterMock).isEnabled();
        doReturn(ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode()).when(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode());
        doReturn(results).when(intentMock).getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);
        doReturn(null).when(fitbitGattMock).getClientCallback();

        sut.onReceive(contextMock, intentMock);

        verify(fitbitGattMock).isInitialized();
        verify(fitbitGattMock).getClientCallback();
        verify(adapterMock).isEnabled();
        verify(intentMock).getAction();
        verify(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, ScanSettings.CALLBACK_TYPE_FIRST_MATCH);
        verify(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode());
        verify(intentMock).getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);
        verifyNoMoreInteractions(intentMock);
        verifyNoMoreInteractions(fitbitGattMock);
    }


    @Test
    public void testCreateRunnable() {
        ArrayList<ScanResult> results = new ArrayList<>();
        ScanResult scanResultMock = mock(ScanResult.class);
        results.add(scanResultMock);
        Handler handler = mock(Handler.class);
        GattClientCallback gattClientCallbackMock = mock(GattClientCallback.class);


        doReturn(adapterMock).when(gattUtilsMock).getBluetoothAdapter(contextMock);
        doReturn(true).when(adapterMock).isEnabled();
        doReturn(ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode()).when(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode());
        doReturn(results).when(intentMock).getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);
        doReturn(gattClientCallbackMock).when(fitbitGattMock).getClientCallback();
        doReturn(handler).when(gattClientCallbackMock).getClientCallbackHandler();

        sut.onReceive(contextMock, intentMock);

        verify(fitbitGattMock).isInitialized();
        verify(fitbitGattMock).getClientCallback();
        verify(adapterMock).isEnabled();
        verify(intentMock).getAction();
        verify(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, ScanSettings.CALLBACK_TYPE_FIRST_MATCH);
        verify(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode());
        verify(intentMock).getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);
        verify(handler).post(any());
        verifyNoMoreInteractions(intentMock);
        verifyNoMoreInteractions(fitbitGattMock);
    }

    @Test
    public void testAddDeviceFromPI() {
        ArrayList<ScanResult> results = new ArrayList<>();
        ScanResult scanResultMock = mock(ScanResult.class);
        results.add(scanResultMock);
        Handler handler = mock(Handler.class);
        GattClientCallback gattClientCallbackMock = mock(GattClientCallback.class);


        BluetoothDevice device = mock(BluetoothDevice.class);
        doReturn("BI:TG:AT:IO").when(device).getAddress();
        doReturn(mock(ScanRecord.class)).when(scanResultMock).getScanRecord();
        doReturn(-60).when(scanResultMock).getRssi();
        doReturn(device).when(scanResultMock).getDevice();
        doReturn(true).when(fitbitGattMock).isInitialized();
        doReturn(true).when(fitbitGattMock).isPendingIntentScanning();
        doReturn(adapterMock).when(gattUtilsMock).getBluetoothAdapter(contextMock);
        doReturn(true).when(adapterMock).isEnabled();
        doReturn(ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode()).when(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode());
        doReturn(results).when(intentMock).getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);
        doReturn(gattClientCallbackMock).when(fitbitGattMock).getClientCallback();
        doReturn(handler).when(gattClientCallbackMock).getClientCallbackHandler();

        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return true;
        }).when(handler).post(any());

        sut.onReceive(contextMock, intentMock);

        verify(fitbitGattMock,times(2)).isInitialized();
        verify(fitbitGattMock).isPendingIntentScanning();
        verify(fitbitGattMock).addBackgroundScannedDeviceConnection(any(FitbitBluetoothDevice.class));
        verify(fitbitGattMock).getClientCallback();
        verify(adapterMock).isEnabled();
        verify(intentMock).getAction();
        verify(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, ScanSettings.CALLBACK_TYPE_FIRST_MATCH);
        verify(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode());
        verify(intentMock).getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);
        verify(handler).post(any());
        verifyNoMoreInteractions(intentMock);
        verifyNoMoreInteractions(fitbitGattMock);
    }


    @Test
    public void testAddDeviceFromWakeUpNOPI() {
        ArrayList<ScanResult> results = new ArrayList<>();
        ScanResult scanResultMock = mock(ScanResult.class);
        results.add(scanResultMock);
        Handler handlerMock = mock(Handler.class);
        GattClientCallback gattClientCallbackMock = mock(GattClientCallback.class);
        PeripheralScanner scannerMock = mock(PeripheralScanner.class);


        BluetoothDevice device = mock(BluetoothDevice.class);
        doReturn("BI:TG:AT:IO").when(device).getAddress();
        doReturn(mock(ScanRecord.class)).when(scanResultMock).getScanRecord();
        doReturn(-60).when(scanResultMock).getRssi();
        doReturn(device).when(scanResultMock).getDevice();
        doReturn(true).when(fitbitGattMock).isInitialized();
        doReturn(false).when(fitbitGattMock).isPendingIntentScanning();
        doReturn(adapterMock).when(gattUtilsMock).getBluetoothAdapter(contextMock);
        doReturn(true).when(adapterMock).isEnabled();
        doReturn(ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode()).when(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode());
        doReturn(results).when(intentMock).getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);
        doReturn(gattClientCallbackMock).when(fitbitGattMock).getClientCallback();
        doReturn(handlerMock).when(gattClientCallbackMock).getClientCallbackHandler();
        doReturn(scannerMock).when(fitbitGattMock).getPeripheralScanner();

        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return true;
        }).when(handlerMock).post(any());

        sut.onReceive(contextMock, intentMock);

        verify(fitbitGattMock,times(2)).isInitialized();
        verify(fitbitGattMock).isPendingIntentScanning();
        verify(fitbitGattMock).addBackgroundScannedDeviceConnection(any(FitbitBluetoothDevice.class));
        verify(fitbitGattMock).getClientCallback();
        verify(fitbitGattMock).getPeripheralScanner();
        verify(scannerMock).setIsPendingIntentScanning(true);
        verify(adapterMock).isEnabled();
        verify(intentMock).getAction();
        verify(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, ScanSettings.CALLBACK_TYPE_FIRST_MATCH);
        verify(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode());
        verify(intentMock).getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);
        verify(handlerMock).post(any());
        verifyNoMoreInteractions(intentMock);
        verifyNoMoreInteractions(fitbitGattMock);
    }


    @Test
    public void testAddDeviceFromWakeUpNOScanner() {
        ArrayList<ScanResult> results = new ArrayList<>();
        ScanResult scanResultMock = mock(ScanResult.class);
        results.add(scanResultMock);
        Handler handlerMock = mock(Handler.class);
        GattClientCallback gattClientCallbackMock = mock(GattClientCallback.class);


        BluetoothDevice device = mock(BluetoothDevice.class);
        doReturn("BI:TG:AT:IO").when(device).getAddress();
        doReturn(mock(ScanRecord.class)).when(scanResultMock).getScanRecord();
        doReturn(-60).when(scanResultMock).getRssi();
        doReturn(device).when(scanResultMock).getDevice();
        doReturn(true).when(fitbitGattMock).isInitialized();
        doReturn(true).when(fitbitGattMock).isInitialized();
        doReturn(false).when(fitbitGattMock).isPendingIntentScanning();
        doReturn(adapterMock).when(gattUtilsMock).getBluetoothAdapter(contextMock);
        doReturn(true).when(adapterMock).isEnabled();
        doReturn(ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode()).when(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode());
        doReturn(results).when(intentMock).getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);
        doReturn(gattClientCallbackMock).when(fitbitGattMock).getClientCallback();
        doReturn(handlerMock).when(gattClientCallbackMock).getClientCallbackHandler();
        doReturn(null).when(fitbitGattMock).getPeripheralScanner();

        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return true;
        }).when(handlerMock).post(any());

        sut.onReceive(contextMock, intentMock);

        verify(fitbitGattMock,times(2)).isInitialized();
        verify(fitbitGattMock).isPendingIntentScanning();
        verify(fitbitGattMock).addBackgroundScannedDeviceConnection(any(FitbitBluetoothDevice.class));
        verify(fitbitGattMock).getClientCallback();
        verify(fitbitGattMock).getPeripheralScanner();
        verify(adapterMock).isEnabled();
        verify(intentMock).getAction();
        verify(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, ScanSettings.CALLBACK_TYPE_FIRST_MATCH);
        verify(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode());
        verify(intentMock).getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);
        verify(handlerMock).post(any());
        verifyNoMoreInteractions(intentMock);
        verifyNoMoreInteractions(fitbitGattMock);
    }

    @Test
    public void testAddDeviceFromWakeUpFitbitGattNotInitialized() {
        ArrayList<ScanResult> results = new ArrayList<>();
        ScanResult scanResultMock = mock(ScanResult.class);
        results.add(scanResultMock);
        Handler handlerMock = mock(Handler.class);
        GattClientCallback gattClientCallbackMock = mock(GattClientCallback.class);


        BluetoothDevice device = mock(BluetoothDevice.class);
        doReturn("BI:TG:AT:IO").when(device).getAddress();
        doReturn(mock(ScanRecord.class)).when(scanResultMock).getScanRecord();
        doReturn(-60).when(scanResultMock).getRssi();
        doReturn(device).when(scanResultMock).getDevice();
        doReturn(false).when(fitbitGattMock).isInitialized();
        doReturn(false).when(fitbitGattMock).isPendingIntentScanning();
        doReturn(adapterMock).when(gattUtilsMock).getBluetoothAdapter(contextMock);
        doReturn(true).when(adapterMock).isEnabled();
        doReturn(ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode()).when(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode());
        doReturn(results).when(intentMock).getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);
        doReturn(gattClientCallbackMock).when(fitbitGattMock).getClientCallback();
        doReturn(handlerMock).when(gattClientCallbackMock).getClientCallbackHandler();
        doReturn(null).when(fitbitGattMock).getPeripheralScanner();

        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return true;
        }).when(handlerMock).post(any());

        AtomicReference<FitbitGatt.FitbitGattCallback> gattCallbackRef = new AtomicReference<>();
        doAnswer(invocation -> {
            gattCallbackRef.set(invocation.getArgument(0));
            gattCallbackRef.get().onGattClientStarted();
            return true;
        }).when(fitbitGattMock).registerGattEventListener(any());

        sut.onReceive(contextMock, intentMock);

        verify(fitbitGattMock,times(2)).isInitialized();
        verify(fitbitGattMock).isPendingIntentScanning();
        verify(fitbitGattMock).addBackgroundScannedDeviceConnection(any(FitbitBluetoothDevice.class));
        verify(fitbitGattMock).getClientCallback();
        verify(fitbitGattMock).registerGattEventListener(any());
        verify(fitbitGattMock).getPeripheralScanner();
        verify(fitbitGattMock).unregisterGattEventListener(gattCallbackRef.get());
        verify(adapterMock).isEnabled();
        verify(intentMock).getAction();
        verify(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, ScanSettings.CALLBACK_TYPE_FIRST_MATCH);
        verify(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode());
        verify(intentMock).getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);
        verify(handlerMock).post(any());
        verifyNoMoreInteractions(intentMock);
        verifyNoMoreInteractions(fitbitGattMock);
    }
}
