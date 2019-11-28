/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tools;

import com.fitbit.bluetooth.fbgatt.FitbitGatt;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GattPluginTests {

    private Context mockContext;
    private static final UUID uuid = UUID.randomUUID();

    @Before
    public void beforeTest(){
        mockContext = mock(Context.class);
    }

    @After
    public void after() {
        FitbitGatt.getInstance().shutdown();
        FitbitGatt.setInstance(null);
    }

    @Test
    public void testGetBluetoothGattDescriptorPermissions(){
        int comboPermissions = BluetoothGattDescriptor.PERMISSION_READ |
                BluetoothGattDescriptor.PERMISSION_WRITE |
                BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED |
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED;
        BluetoothGattDescriptor descriptor = mock(BluetoothGattDescriptor.class);
        when(descriptor.getPermissions()).thenReturn(comboPermissions);
        when(descriptor.getUuid()).thenReturn(uuid);
        String output = new GattPlugin(mockContext).getStringRepresentationOfPermissionsForDescriptor(descriptor);
        Assert.assertEquals("read, read-encrypted, write, write-encrypted", output);
    }

    @Test
    public void testGetBluetoothGattCharacteristicProperties(){
        int comboProperties = BluetoothGattCharacteristic.PROPERTY_READ |
                BluetoothGattCharacteristic.PROPERTY_WRITE |
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE |
                BluetoothGattCharacteristic.PROPERTY_NOTIFY;
        BluetoothGattCharacteristic characteristic = mock(BluetoothGattCharacteristic.class);
        when(characteristic.getProperties()).thenReturn(comboProperties);
        when(characteristic.getUuid()).thenReturn(uuid);
        String output = new GattPlugin(mockContext).getStringRepresentationOfPropertiesForCharacteristic(characteristic);
        Assert.assertEquals("read, write, notify, write-no-response", output);
    }

    @Test
    public void testGetBluetoothGattCharacteristicPermissions(){
        int comboPermissions = BluetoothGattCharacteristic.PERMISSION_READ |
                BluetoothGattCharacteristic.PERMISSION_WRITE |
                BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED |
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED;
        BluetoothGattCharacteristic characteristic = mock(BluetoothGattCharacteristic.class);
        when(characteristic.getPermissions()).thenReturn(comboPermissions);
        when(characteristic.getUuid()).thenReturn(uuid);
        String output = new GattPlugin(mockContext).getStringRepresentationOfPermissionsForCharacteristic(characteristic);
        Assert.assertEquals("read, read-encrypted, write, read-encrypted", output);
    }
}