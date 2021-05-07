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
import com.fitbit.bluetooth.fbgatt.btcopies.BluetoothGattServiceCopy;
import com.fitbit.bluetooth.fbgatt.util.GattUtils;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A class for testing the gatt utils
 *
 * Created by iowens on 6/5/18.
 */
@RunWith(JUnit4.class)
public class GattUtilsTest {
    @Test
    public void testCopyCharacteristic(){
        byte[] data = new byte[]{0x01, 0x02, 0x03};
        BluetoothGattCharacteristic characteristic = mock(BluetoothGattCharacteristic.class);
        // this should be the same as the real thing, the data inside could change as it's pointing
        // to something else.
        when(characteristic.getValue()).thenReturn(data);
        when(characteristic.getUuid()).thenReturn(UUID.fromString("E69169D1-11A7-4545-B7FC-02A3ADFC3EAC"));
        when(characteristic.getPermissions()).thenReturn(BluetoothGattCharacteristic.PERMISSION_READ);
        when(characteristic.getProperties()).thenReturn(BluetoothGattCharacteristic.PROPERTY_NOTIFY);
        BluetoothGattCharacteristicCopy result = new GattUtils().copyCharacteristic(characteristic);
        if(result == null) {
            fail(String.format(Locale.ENGLISH, "The result was null for characteristic: %s, with data: %s", characteristic.getUuid(), Arrays.toString(characteristic.getValue())));
            return;
        }
        data[2] = 0x04;
        Assert.assertNotEquals(Arrays.hashCode(data), Arrays.hashCode(result.getValue()));
    }

    @Test
    public void testCopyCharacteristicWithNullValue(){
        byte[] data = null;
        BluetoothGattCharacteristic characteristic = mock(BluetoothGattCharacteristic.class);
        // this should be the same as the real thing, the data inside could change as it's pointing
        // to something else.
        when(characteristic.getValue()).thenReturn(data);
        when(characteristic.getUuid()).thenReturn(UUID.fromString("E69169D1-11A7-4545-B7FC-02A3ADFC3EAC"));
        when(characteristic.getPermissions()).thenReturn(BluetoothGattCharacteristic.PERMISSION_READ);
        when(characteristic.getProperties()).thenReturn(BluetoothGattCharacteristic.PROPERTY_NOTIFY);
        BluetoothGattCharacteristicCopy result = new GattUtils().copyCharacteristic(characteristic);
        if(result == null) {
            fail(String.format(Locale.ENGLISH, "The result was null for characteristic: %s, with data: %s", characteristic.getUuid(), Arrays.toString(characteristic.getValue())));
            return;
        }
        //Assert.assertNotEquals(Arrays.hashCode(data), Arrays.hashCode(result.getValue()));
        Assert.assertNull(result.getValue());
    }

    @Test
    public void testCopyDescriptor(){
        byte[] data = new byte[]{0x01, 0x02, 0x03};
        BluetoothGattDescriptor descriptor = mock(BluetoothGattDescriptor.class);
        // this should be the same as the real thing, the data inside could change as it's pointing
        // to something else.
        when(descriptor.getValue()).thenReturn(data);
        when(descriptor.getUuid()).thenReturn(UUID.fromString("E69169D1-11A7-4545-B7FC-02A3ADFC3EAC"));
        when(descriptor.getPermissions()).thenReturn(BluetoothGattDescriptor.PERMISSION_READ);
        BluetoothGattDescriptorCopy result = new GattUtils().copyDescriptor(descriptor);
        if(result == null) {
            fail(String.format(Locale.ENGLISH, "The result was null for descriptor: %s, with data: %s", descriptor.getUuid(), Arrays.toString(descriptor.getValue())));
            return;
        }
        data[2] = 0x04;
        Assert.assertNotEquals(Arrays.hashCode(data), Arrays.hashCode(result.getValue()));
    }

    @Test
    public void testCopyDescriptorWithNullValue(){
        byte[] data = null;
        BluetoothGattDescriptor descriptor = mock(BluetoothGattDescriptor.class);
        // this should be the same as the real thing, the data inside could change as it's pointing
        // to something else.
        when(descriptor.getValue()).thenReturn(data);
        when(descriptor.getUuid()).thenReturn(UUID.fromString("E69169D1-11A7-4545-B7FC-02A3ADFC3EAC"));
        when(descriptor.getPermissions()).thenReturn(BluetoothGattDescriptor.PERMISSION_READ);
        BluetoothGattDescriptorCopy result = new GattUtils().copyDescriptor(descriptor);
        if(result == null) {
            fail(String.format(Locale.ENGLISH, "The result was null for descriptor: %s, with data: %s", descriptor.getUuid(), Arrays.toString(descriptor.getValue())));
            return;
        }
        Assert.assertNull(result.getValue());
    }

    @Test
    public void testCopyCharacteristicWithDescriptorChild(){
        byte[] data = null;
        BluetoothGattCharacteristic characteristic = mock(BluetoothGattCharacteristic.class);
        // this should be the same as the real thing, the data inside could change as it's pointing
        // to something else.
        when(characteristic.getValue()).thenReturn(data);
        when(characteristic.getUuid()).thenReturn(UUID.fromString("E69169D1-11A7-4545-B7FC-02A3ADFC3EAC"));
        when(characteristic.getPermissions()).thenReturn(BluetoothGattCharacteristic.PERMISSION_READ);
        when(characteristic.getProperties()).thenReturn(BluetoothGattCharacteristic.PROPERTY_NOTIFY);
        BluetoothGattDescriptor descriptor = mock(BluetoothGattDescriptor.class);
        // this should be the same as the real thing, the data inside could change as it's pointing
        // to something else.
        when(descriptor.getValue()).thenReturn(data);
        when(descriptor.getUuid()).thenReturn(UUID.fromString("E69169D1-11A7-4545-B7FC-02A3ADFC3EAC"));
        when(descriptor.getPermissions()).thenReturn(BluetoothGattDescriptor.PERMISSION_READ);
        characteristic.addDescriptor(descriptor);
        BluetoothGattCharacteristicCopy result = new GattUtils().copyCharacteristic(characteristic);
        if(result == null) {
            fail(String.format(Locale.ENGLISH, "The result was null for characteristic: %s, with data: %s", characteristic.getUuid(), Arrays.toString(characteristic.getValue())));
            return;
        }
        Assert.assertNotEquals(descriptor, characteristic.getDescriptor(descriptor.getUuid()));
    }

    @Test
    public void testCopyServiceWithCharacteristicWithDescriptorChild(){
        byte[] data = null;
        BluetoothGattService service = mock(BluetoothGattService.class);
        when(service.getUuid()).thenReturn(UUID.fromString("70FE657D-BBB0-4D17-B6A6-8C0545C4001F"));
        when(service.getType()).thenReturn(BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic characteristic = mock(BluetoothGattCharacteristic.class);
        // this should be the same as the real thing, the data inside could change as it's pointing
        // to something else.
        when(characteristic.getValue()).thenReturn(data);
        when(characteristic.getUuid()).thenReturn(UUID.fromString("E69169D1-11A7-4545-B7FC-02A3ADFC3EAC"));
        when(characteristic.getPermissions()).thenReturn(BluetoothGattCharacteristic.PERMISSION_READ);
        when(characteristic.getProperties()).thenReturn(BluetoothGattCharacteristic.PROPERTY_NOTIFY);
        BluetoothGattDescriptor descriptor = mock(BluetoothGattDescriptor.class);
        // this should be the same as the real thing, the data inside could change as it's pointing
        // to something else.
        when(descriptor.getValue()).thenReturn(data);
        when(descriptor.getUuid()).thenReturn(UUID.fromString("E69169D1-11A7-4545-B7FC-02A3ADFC3EAC"));
        when(descriptor.getPermissions()).thenReturn(BluetoothGattDescriptor.PERMISSION_READ);
        characteristic.addDescriptor(descriptor);
        service.addCharacteristic(characteristic);
        BluetoothGattServiceCopy result = new GattUtils().copyService(service);
        if(result == null) {
            fail(String.format(Locale.ENGLISH, "The result was null for service: %s, with characteristic data: %s", characteristic.getUuid(), Arrays.toString(characteristic.getValue())));
            return;
        }
        Assert.assertNotEquals(descriptor, characteristic.getDescriptor(descriptor.getUuid()));
        Assert.assertNotEquals(characteristic, service.getCharacteristic(characteristic.getUuid()));
    }

    @Test
    public void testCopyServiceWithCharacteristicWithDescriptorChildWithData() {
        byte[] data = new byte[]{0x01, 0x02, 0x03};
        BluetoothGattService service = mock(BluetoothGattService.class);
        when(service.getUuid()).thenReturn(UUID.fromString("70FE657D-BBB0-4D17-B6A6-8C0545C4001F"));
        when(service.getType()).thenReturn(BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic characteristic = mock(BluetoothGattCharacteristic.class);
        // this should be the same as the real thing, the data inside could change as it's pointing
        // to something else.
        when(characteristic.getValue()).thenReturn(data);
        when(characteristic.getUuid()).thenReturn(UUID.fromString("E69169D1-11A7-4545-B7FC-02A3ADFC3EAC"));
        when(characteristic.getPermissions()).thenReturn(BluetoothGattCharacteristic.PERMISSION_READ);
        when(characteristic.getProperties()).thenReturn(BluetoothGattCharacteristic.PROPERTY_NOTIFY);
        BluetoothGattDescriptor descriptor = mock(BluetoothGattDescriptor.class);
        // this should be the same as the real thing, the data inside could change as it's pointing
        // to something else.
        when(descriptor.getValue()).thenReturn(data);
        when(descriptor.getUuid()).thenReturn(UUID.fromString("F752AF23-EA2C-45BA-892C-5964B0D244A2"));
        when(descriptor.getPermissions()).thenReturn(BluetoothGattDescriptor.PERMISSION_READ);
        characteristic.addDescriptor(descriptor);
        service.addCharacteristic(characteristic);
        BluetoothGattServiceCopy result = new GattUtils().copyService(service);
        if(result == null) {
            fail(String.format(Locale.ENGLISH, "The result was null for service: %s, with characteristic data: %s", characteristic.getUuid(), Arrays.toString(characteristic.getValue())));
            return;
        }
        Assert.assertNotEquals(descriptor, characteristic.getDescriptor(descriptor.getUuid()));
        Assert.assertNotEquals(characteristic, service.getCharacteristic(characteristic.getUuid()));
        Assert.assertArrayEquals(descriptor.getValue(), data);
    }

}
