/*
 *  Copyright 2020 Fitbit, Inc. All rights reserved.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.util.BluetoothUtils;
import com.fitbit.bluetooth.fbgatt.util.ScanFailedReason;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SuppressWarnings("ResultOfMethodCallIgnored")
@RunWith(JUnit4.class)
public class HandleIntentBasedScanResultTest {

    private final BluetoothUtils mockBluetoothUtils = mock(BluetoothUtils.class);
    private final FitbitGatt fitbitGattMock = mock(FitbitGatt.class);

    private HandleIntentBasedScanResult sut = new HandleIntentBasedScanResult(mockBluetoothUtils, fitbitGattMock);

    private final Context contextMock = mock(Context.class);
    private final Intent intentMock = mock(Intent.class);

    @Test
    public void testBTOff() {
        doReturn(false).when(mockBluetoothUtils).isBluetoothEnabled(contextMock);

        sut.onReceive(contextMock, intentMock);

        verifyNoMoreInteractions(fitbitGattMock);
        verify(mockBluetoothUtils).isBluetoothEnabled(contextMock);
        verifyNoMoreInteractions(intentMock);
    }

    @Test
    public void testNoAdapter() {
        doReturn(false).when(mockBluetoothUtils).isBluetoothEnabled(contextMock);

        sut.onReceive(contextMock, intentMock);

        verifyNoMoreInteractions(fitbitGattMock);
        verifyNoMoreInteractions(intentMock);
    }

    @Test
    public void testScanResultError() {
        doReturn(true).when(mockBluetoothUtils).isBluetoothEnabled(contextMock);
        doReturn(ScanFailedReason.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED.getCode()).when(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode());

        sut.onReceive(contextMock, intentMock);

        verify(fitbitGattMock).isInitialized();
        verify(mockBluetoothUtils).isBluetoothEnabled(contextMock);
        verify(intentMock).getAction();
        verify(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, ScanSettings.CALLBACK_TYPE_FIRST_MATCH);
        verify(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode());
        verifyNoMoreInteractions(intentMock);
        verifyNoMoreInteractions(fitbitGattMock);
    }


    @Test
    public void testEmptyScanResults() {
        GattClientCallback gattClientCallbackMock = mock(GattClientCallback.class);

        doReturn(true).when(mockBluetoothUtils).isBluetoothEnabled(contextMock);
        doReturn(ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode()).when(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode());
        doReturn(new ArrayList<ScanResult>()).when(intentMock).getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);
        doReturn(true).when(fitbitGattMock).isInitialized();
        doReturn(gattClientCallbackMock).when(fitbitGattMock).getClientCallback();

        sut.onReceive(contextMock, intentMock);

        verify(fitbitGattMock).isInitialized();
        verify(mockBluetoothUtils).isBluetoothEnabled(contextMock);
        verify(intentMock).getAction();
        verify(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, ScanSettings.CALLBACK_TYPE_FIRST_MATCH);
        verify(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode());
        verify(intentMock).getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);
        verifyNoMoreInteractions(intentMock);
        verifyNoMoreInteractions(fitbitGattMock);
    }

    @Test
    public void testNullResultSet() {
        doReturn(true).when(mockBluetoothUtils).isBluetoothEnabled(contextMock);
        doReturn(ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode()).when(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode());
        doReturn(null).when(intentMock).getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);

        sut.onReceive(contextMock, intentMock);

        verify(fitbitGattMock).isInitialized();
        verify(mockBluetoothUtils).isBluetoothEnabled(contextMock);
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

        doReturn(true).when(mockBluetoothUtils).isBluetoothEnabled(contextMock);
        doReturn(ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode()).when(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode());
        doReturn(results).when(intentMock).getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);
        doReturn(null).when(fitbitGattMock).getClientCallback();

        sut.onReceive(contextMock, intentMock);

        verify(fitbitGattMock).isInitialized();
        verify(fitbitGattMock).getClientCallback();
        verify(mockBluetoothUtils).isBluetoothEnabled(contextMock);
        verify(intentMock).getAction();
        verify(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, ScanSettings.CALLBACK_TYPE_FIRST_MATCH);
        verify(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode());
        verify(intentMock).getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);
        verify(fitbitGattMock).registerGattEventListener(any());
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


        doReturn(true).when(mockBluetoothUtils).isBluetoothEnabled(contextMock);
        doReturn(ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode()).when(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode());
        doReturn(results).when(intentMock).getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);
        doReturn(gattClientCallbackMock).when(fitbitGattMock).getClientCallback();
        doReturn(handler).when(gattClientCallbackMock).getClientCallbackHandler();
        doReturn(true).when(fitbitGattMock).isInitialized();

        sut.onReceive(contextMock, intentMock);

        verify(fitbitGattMock, times(2)).isInitialized();
        verify(fitbitGattMock).getClientCallback();
        verify(mockBluetoothUtils).isBluetoothEnabled(contextMock);
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
        doReturn(true).when(mockBluetoothUtils).isBluetoothEnabled(contextMock);
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

        verify(fitbitGattMock, times(2)).isInitialized();
        verify(fitbitGattMock).isPendingIntentScanning();
        verify(fitbitGattMock).addBackgroundScannedDeviceConnection(any(FitbitBluetoothDevice.class));
        verify(fitbitGattMock).getClientCallback();
        verify(mockBluetoothUtils).isBluetoothEnabled(contextMock);
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
        doReturn(true).when(mockBluetoothUtils).isBluetoothEnabled(contextMock);
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

        verify(fitbitGattMock, times(2)).isInitialized();
        verify(fitbitGattMock).isPendingIntentScanning();
        verify(fitbitGattMock).addBackgroundScannedDeviceConnection(any(FitbitBluetoothDevice.class));
        verify(fitbitGattMock).getClientCallback();
        verify(fitbitGattMock).getPeripheralScanner();
        verify(scannerMock).setIsPendingIntentScanning(true);
        verify(mockBluetoothUtils).isBluetoothEnabled(contextMock);
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
        doReturn(true).when(mockBluetoothUtils).isBluetoothEnabled(contextMock);
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

        verify(fitbitGattMock, times(2)).isInitialized();
        verify(fitbitGattMock).isPendingIntentScanning();
        verify(fitbitGattMock).addBackgroundScannedDeviceConnection(any(FitbitBluetoothDevice.class));
        verify(fitbitGattMock).getClientCallback();
        verify(fitbitGattMock).getPeripheralScanner();
        verify(mockBluetoothUtils).isBluetoothEnabled(contextMock);
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
        doAnswer(new Answer<Boolean>() {
            private int count = 0;

            public Boolean answer(InvocationOnMock invocation) {
                return count++ > 1;
            }
        }).when(fitbitGattMock).isInitialized();
        doReturn(false).when(fitbitGattMock).isPendingIntentScanning();
        doReturn(true).when(mockBluetoothUtils).isBluetoothEnabled(contextMock);
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

        verify(fitbitGattMock, times(3)).isInitialized();
        verify(fitbitGattMock).isPendingIntentScanning();
        verify(fitbitGattMock).addBackgroundScannedDeviceConnection(any(FitbitBluetoothDevice.class));
        verify(fitbitGattMock, times(2)).getClientCallback();
        verify(fitbitGattMock).registerGattEventListener(any());
        verify(fitbitGattMock).getPeripheralScanner();
        verify(fitbitGattMock).unregisterGattEventListener(gattCallbackRef.get());
        verify(mockBluetoothUtils).isBluetoothEnabled(contextMock);
        verify(intentMock).getAction();
        verify(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, ScanSettings.CALLBACK_TYPE_FIRST_MATCH);
        verify(intentMock).getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode());
        verify(intentMock).getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);
        verify(handlerMock).post(any());
        verifyNoMoreInteractions(intentMock);
        verifyNoMoreInteractions(fitbitGattMock);
    }
}
