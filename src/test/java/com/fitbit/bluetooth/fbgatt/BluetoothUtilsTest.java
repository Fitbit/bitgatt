/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.util.GattUtils;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Test the name retrieval code
 *
 * Created by iowens on 2/28/18.
 */

@RunWith(JUnit4.class)
public class BluetoothUtilsTest {

    private BluetoothDevice mockDevice;
    private BluetoothDevice nonNullMockDevice;
    private BluetoothGatt mockGatt;
    private BluetoothGatt nonNullMockGatt;
    private BluetoothDevice throwableNullMockDevice;
    private BluetoothGatt throwableNullMockGatt;
    private GattUtils utils;

    private String expected = "Unknown Name";

    @Before
    public void setup() {
        if (BuildConfig.DEBUG) {
            expected = "Ionic";
        }
        utils = new GattUtils();
        mockDevice = mock(BluetoothDevice.class);
        when(mockDevice.getName()).thenReturn(null);
        mockGatt = mock(BluetoothGatt.class);
        when(mockGatt.getDevice()).thenReturn(null);
        nonNullMockDevice = mock(BluetoothDevice.class);
        when(nonNullMockDevice.getName()).thenReturn("Ionic");
        nonNullMockGatt = mock(BluetoothGatt.class);
        when(nonNullMockGatt.getDevice()).thenReturn(nonNullMockDevice);
        throwableNullMockGatt = mock(BluetoothGatt.class);
        throwableNullMockDevice = mock(BluetoothDevice.class);
        when(throwableNullMockDevice.getName()).thenThrow(new NullPointerException("You suck bluedroid"));
        when(throwableNullMockGatt.getDevice()).thenReturn(throwableNullMockDevice);

    }

    @Test
    public void testProvidingGattWithNullDevice() throws Exception {
        String name = utils.debugSafeGetBtDeviceName(mockGatt);
        assertEquals("Unknown Name", name);
    }

    @Test
    public void testProvidingBluetoothDeviceWithNullName() throws Exception {
        String name = utils.debugSafeGetBtDeviceName(mockDevice);
        assertEquals("Unknown Name", name);
    }

    @Test
    public void testProvidingGattWithNonNullName() throws Exception {
        String name = utils.debugSafeGetBtDeviceName(nonNullMockGatt);
        assertEquals(expected, name);
    }

    @Test
    @Ignore
    public void testProvidingBluetoothDeviceWithNonNullName() throws Exception {
        String name = utils.debugSafeGetBtDeviceName(nonNullMockDevice);
        assertEquals(expected, name);
    }

    @Test
    public void testThrowableOnGetNameInsideGatt() throws Exception {
        String name = utils.debugSafeGetBtDeviceName(throwableNullMockGatt);
        assertEquals("Unknown Name", name);
    }

    @Test
    public void testThrowableOnGetNameInsideBluetoothDevice() throws Exception {
        String name = utils.debugSafeGetBtDeviceName(throwableNullMockDevice);
        assertEquals("Unknown Name", name);
    }
}
