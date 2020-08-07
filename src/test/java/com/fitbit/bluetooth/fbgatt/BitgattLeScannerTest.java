/*
 *  Copyright 2020 Fitbit, Inc. All rights reserved.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.util.GattUtils;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import org.junit.Before;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class BitgattLeScannerTest {

    private Context contextMock = mock(Context.class);
    private BluetoothAdapter adapterMock = mock(BluetoothAdapter.class);
    private BluetoothLeScanner scannerMock = mock(BluetoothLeScanner.class);
    private GattUtils gattUtilsMock = mock(GattUtils.class);
    private BitgattLeScanner sut;

    @Before
    public void before() {
        doReturn(adapterMock).when(gattUtilsMock).getBluetoothAdapter(contextMock);
        doReturn(scannerMock).when(adapterMock).getBluetoothLeScanner();
        doReturn(true).when(adapterMock).isEnabled();
        sut = new BitgattLeScanner(contextMock, gattUtilsMock);
    }

    @Test
    public void testStartScanner() {
        ScanCallback cb = mock(ScanCallback.class);
        sut.startScan(cb);
        verify(scannerMock).startScan(cb);
    }

    @Test
    public void testStartScannerWithSettingsAndFilters() {
        ScanCallback cb = mock(ScanCallback.class);
        ScanSettings settings = mock(ScanSettings.class);
        List<ScanFilter> filters = new ArrayList<>();
        sut.startScan(filters, settings, cb);
        verify(scannerMock, never()).startScan(cb);
        verify(scannerMock).startScan(filters, settings, cb);
    }

    @Test
    public void testStartScannerWithSettingsAndFiltersPI() throws NoSuchFieldException, IllegalAccessException {
        new TestUtils().setStaticField(Build.VERSION.class.getField("SDK_INT"), Build.VERSION_CODES.O);
        ScanSettings settings = mock(ScanSettings.class);
        List<ScanFilter> filters = new ArrayList<>();
        PendingIntent intent = mock(PendingIntent.class);
        sut.startScan(filters, settings, intent);
        verify(scannerMock).startScan(filters, settings, intent);
    }

    @Test
    public void testStartScannerWithSettingsAndFiltersPIPreOreo() throws NoSuchFieldException, IllegalAccessException {
        new TestUtils().setStaticField(Build.VERSION.class.getField("SDK_INT"), Build.VERSION_CODES.N);
        ScanSettings settings = mock(ScanSettings.class);
        List<ScanFilter> filters = new ArrayList<>();
        PendingIntent intent = mock(PendingIntent.class);
        sut.startScan(filters, settings, intent);
        verify(scannerMock, never()).startScan(filters, settings, intent);
    }

    @Test
    public void stopScanBtEnabledWithCB() {
        doReturn(true).when(adapterMock).isEnabled();
        ScanCallback cb = mock(ScanCallback.class);
        sut.stopScan(cb);
        verify(scannerMock).stopScan(cb);
    }

    @Test
    public void stopScanBtEnabledWithPI() throws NoSuchFieldException, IllegalAccessException {
        new TestUtils().setStaticField(Build.VERSION.class.getField("SDK_INT"), Build.VERSION_CODES.O);
        doReturn(true).when(adapterMock).isEnabled();
        PendingIntent pi = mock(PendingIntent.class);
        sut.stopScan(pi);
        verify(scannerMock).stopScan(pi);
    }

    @Test
    public void stopScanBtEnabledWithPreOreo() throws NoSuchFieldException, IllegalAccessException {
        new TestUtils().setStaticField(Build.VERSION.class.getField("SDK_INT"), Build.VERSION_CODES.N);
        doReturn(true).when(adapterMock).isEnabled();
        PendingIntent pi = mock(PendingIntent.class);
        sut.stopScan(pi);
        verify(scannerMock, never()).stopScan(pi);
    }

    @Test
    public void testFlushPendingScanResults() throws NoSuchFieldException, IllegalAccessException {
        doReturn(true).when(adapterMock).isEnabled();
        ScanCallback cb = mock(ScanCallback.class);
        sut.flushPendingScanResults(cb);
        verify(scannerMock).flushPendingScanResults(cb);
    }

    @Test
    public void testNoScannerNoExceptions() throws NoSuchFieldException, IllegalAccessException {
        new TestUtils().setStaticField(Build.VERSION.class.getField("SDK_INT"), Build.VERSION_CODES.O);
        doReturn(adapterMock).when(gattUtilsMock).getBluetoothAdapter(contextMock);
        doReturn(null).when(adapterMock).getBluetoothLeScanner();

        BitgattLeScanner sut = new BitgattLeScanner(contextMock, gattUtilsMock);

        ScanCallback cb = mock(ScanCallback.class);
        ScanSettings settings = mock(ScanSettings.class);
        List<ScanFilter> filters = new ArrayList<>();
        PendingIntent intent = mock(PendingIntent.class);

        sut.startScan(cb);
        sut.startScan(filters, settings, cb);
        sut.startScan(filters, settings, intent);
        sut.stopScan(cb);
        sut.stopScan(intent);
        sut.flushPendingScanResults(cb);

        verify(adapterMock, times(6)).getBluetoothLeScanner();
        verifyNoMoreInteractions(scannerMock);
    }

    @Test
    public void testNoAdapterNoExceptions() throws NoSuchFieldException, IllegalAccessException {
        new TestUtils().setStaticField(Build.VERSION.class.getField("SDK_INT"), Build.VERSION_CODES.O);
        doReturn(null).when(gattUtilsMock).getBluetoothAdapter(contextMock);

        BitgattLeScanner sut = new BitgattLeScanner(contextMock, gattUtilsMock);

        ScanCallback cb = mock(ScanCallback.class);
        ScanSettings settings = mock(ScanSettings.class);
        List<ScanFilter> filters = new ArrayList<>();
        PendingIntent intent = mock(PendingIntent.class);
        sut.startScan(cb);
        sut.startScan(filters, settings, cb);
        sut.startScan(filters, settings, intent);
        sut.stopScan(cb);
        sut.stopScan(intent);
        sut.flushPendingScanResults(cb);
        assertFalse(sut.isBluetoothEnabled());
    }

    @Test
    public void testNoContext() throws NoSuchFieldException, IllegalAccessException {
        new TestUtils().setStaticField(Build.VERSION.class.getField("SDK_INT"), Build.VERSION_CODES.O);

        BitgattLeScanner sut = new BitgattLeScanner(null);

        ScanCallback cb = mock(ScanCallback.class);
        ScanSettings settings = mock(ScanSettings.class);
        List<ScanFilter> filters = new ArrayList<>();
        PendingIntent intent = mock(PendingIntent.class);
        sut.startScan(cb);
        sut.startScan(filters, settings, cb);
        sut.startScan(filters, settings, intent);
        sut.stopScan(cb);
        sut.stopScan(intent);
        sut.flushPendingScanResults(cb);
        assertFalse(sut.isBluetoothEnabled());
    }
}
