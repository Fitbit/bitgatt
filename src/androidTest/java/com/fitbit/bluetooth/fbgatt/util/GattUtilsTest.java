/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.util;

import com.fitbit.bluetooth.fbgatt.btcopies.BluetoothGattServiceCopy;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import junit.framework.Assert;

import org.junit.Test;

import java.util.Arrays;
import java.util.UUID;

public class GattUtilsTest {

    @Test
    public void testCopyServiceFull() {
        BluetoothGattService service = new BluetoothGattService(UUID.randomUUID(), BluetoothGattService.SERVICE_TYPE_PRIMARY);
        service.addService(new BluetoothGattService(UUID.randomUUID(), BluetoothGattService.SERVICE_TYPE_SECONDARY));
        service.addService(new BluetoothGattService(UUID.randomUUID(), BluetoothGattService.SERVICE_TYPE_SECONDARY));
        service.addService(service);
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID.randomUUID(), BluetoothGattCharacteristic.PERMISSION_READ, BluetoothGattCharacteristic.PROPERTY_NOTIFY);
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(UUID.randomUUID(), BluetoothGattDescriptor.PERMISSION_WRITE);
        descriptor.setValue(new byte[]{0x01, 0x02, 0x04, 0x21});
        characteristic.addDescriptor(descriptor);
        service.addCharacteristic(characteristic);
        BluetoothGattServiceCopy copyOfservice = new GattUtils().copyService(service);
        Assert.assertNotNull(copyOfservice);
        Assert.assertEquals(service.getIncludedServices().size(), copyOfservice.getIncludedServices().size());
        Assert.assertEquals(service.getCharacteristics().size(), copyOfservice.getCharacteristics().size());
        Assert.assertTrue(Arrays.equals(descriptor.getValue(),
            copyOfservice.getCharacteristic(characteristic.getUuid()).getDescriptor(descriptor.getUuid()).getValue()));
    }

    @Test
    public void testCopyServiceInfiniteRecursion() {
        BluetoothGattService service = new BluetoothGattService(UUID.randomUUID(), BluetoothGattService.SERVICE_TYPE_PRIMARY);
        service.addService(new BluetoothGattService(UUID.randomUUID(), BluetoothGattService.SERVICE_TYPE_SECONDARY));
        service.addService(new BluetoothGattService(UUID.randomUUID(), BluetoothGattService.SERVICE_TYPE_SECONDARY));
        service.addService(service);
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID.randomUUID(), BluetoothGattCharacteristic.PERMISSION_READ, BluetoothGattCharacteristic.PROPERTY_NOTIFY);
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(UUID.randomUUID(), BluetoothGattDescriptor.PERMISSION_WRITE);
        descriptor.setValue(new byte[]{0x01, 0x02, 0x04, 0x21});
        characteristic.addDescriptor(descriptor);
        service.addCharacteristic(characteristic);
        BluetoothGattServiceCopy copyOfservice = new GattUtils().copyService(service);
        Assert.assertNotNull(copyOfservice);
        Assert.assertEquals(service.getIncludedServices().size(), copyOfservice.getIncludedServices().size());
        Assert.assertEquals(service.getCharacteristics().size(), copyOfservice.getCharacteristics().size());
        Assert.assertTrue(Arrays.equals(descriptor.getValue(),
            copyOfservice.getCharacteristic(characteristic.getUuid()).getDescriptor(descriptor.getUuid()).getValue()));
    }
}
