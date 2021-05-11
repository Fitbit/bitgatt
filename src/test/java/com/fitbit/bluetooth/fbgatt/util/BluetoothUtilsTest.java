/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class BluetoothUtilsTest {

    private Context mockContext = mock(Context.class);
    private BluetoothManager mockBluetoothManager = mock(BluetoothManager.class);
    private BluetoothAdapter mockBluetoothAdapter = mock(BluetoothAdapter.class);
    private BluetoothManagerProvider mockBluetoothManagerProvider = mock(BluetoothManagerProvider.class);

    private BluetoothUtils utils = new BluetoothUtils(mockBluetoothManagerProvider);

    @Test
    public void getBluetoothAdapter_shouldReturnNullIfBluetoothNotSupported() {
        mockBluetoothNotAvailable();

        BluetoothAdapter adapter = utils.getBluetoothAdapter(mockContext);
        assertNull(adapter);
    }

    @Test
    public void getBluetoothAdapter_shouldReturnNullIfNoDefaultAdapter() {
        mockBluetoothAvailable();
        mockNoDefaultAdapter();

        BluetoothAdapter adapter = utils.getBluetoothAdapter(mockContext);
        assertNull(adapter);
    }

    @Test
    public void getBluetoothAdapter_shouldReturnAdapterIfBluetoothIsSupported() {
        mockBluetoothAvailable();
        mockAdapter();

        BluetoothAdapter adapter = utils.getBluetoothAdapter(mockContext);
        assertEquals(mockBluetoothAdapter, adapter);
    }

    @Test
    public void getBluetoothLeScanner_shouldReturnNullIfBluetoothNotSupported() {
        mockBluetoothNotAvailable();

        BluetoothLeScanner scanner = utils.getBluetoothLeScanner(mockContext);
        assertNull(scanner);
    }

    @Test
    public void getBluetoothLeScanner_shouldReturnNullIfNoDefaultAdapter() {
        mockBluetoothAvailable();
        mockNoDefaultAdapter();

        BluetoothLeScanner scanner = utils.getBluetoothLeScanner(mockContext);
        assertNull(scanner);
    }

    @Test
    public void getBluetoothLeScanner_shouldReturnNullIfBluetoothNotEnabled() {
        mockBluetoothAvailable();
        mockAdapter();
        mockBleDisabled();

        BluetoothLeScanner scanner = utils.getBluetoothLeScanner(mockContext);
        assertNull(scanner);
    }

    @Test
    public void getBluetoothLeScanner_shouldReturnAdapterIfBluetoothIsSupported() {
        mockBluetoothAvailable();
        mockAdapter();
        mockBleEnabled();
        BluetoothLeScanner mockBluetoothLeScanner = mock(BluetoothLeScanner.class);
        when(mockBluetoothAdapter.getBluetoothLeScanner()).thenReturn(mockBluetoothLeScanner);

        BluetoothLeScanner scanner = utils.getBluetoothLeScanner(mockContext);
        assertEquals(mockBluetoothLeScanner, scanner);
    }

    @Test
    public void isBluetoothEnabled_shouldReturnFalseIfBluetoothNotSupported() {
        mockBluetoothNotAvailable();

        boolean enabled = utils.isBluetoothEnabled(mockContext);
        assertFalse(enabled);
    }

    @Test
    public void isBluetoothEnabled_shouldReturnFalseIfNoDefaultAdapter() {
        mockBluetoothAvailable();
        mockNoDefaultAdapter();

        boolean enabled = utils.isBluetoothEnabled(mockContext);
        assertFalse(enabled);
    }

    @Test
    public void isBluetoothEnabled_shouldReturnTrueIfEnabled() {
        mockBluetoothAvailable();
        mockAdapter();
        mockBleEnabled();

        boolean enabled = utils.isBluetoothEnabled(mockContext);
        assertTrue(enabled);
    }

    private void mockBluetoothNotAvailable() {
        when(mockBluetoothManagerProvider.get(mockContext)).thenReturn(null);
    }

    private void mockBluetoothAvailable() {
        when(mockBluetoothManagerProvider.get(mockContext)).thenReturn(mockBluetoothManager);
    }

    private void mockNoDefaultAdapter() {
        when(mockBluetoothManager.getAdapter()).thenReturn(null);
    }

    private void mockAdapter() {
        when(mockBluetoothManager.getAdapter()).thenReturn(mockBluetoothAdapter);
    }

    private void mockBleEnabled() {
        when(mockBluetoothAdapter.isEnabled()).thenReturn(true);
    }

    private void mockBleDisabled() {
        when(mockBluetoothAdapter.isEnabled()).thenReturn(false);
    }
}