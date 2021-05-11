/*
 *
 *  Copyright 2021 Fitbit, Inc. All rights reserved.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package com.fitbit.bluetooth.fbgatt.utils;

import com.fitbit.bluetooth.fbgatt.util.Bytes;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.fitbit.bluetooth.fbgatt.tools.GattServerConnectionConsts.RESULT_CHARACTERISTIC_UUID_KEY;
import static com.fitbit.bluetooth.fbgatt.tools.GattServerConnectionConsts.RESULT_DESCRIPTOR_UUID_KEY;
import static com.fitbit.bluetooth.fbgatt.tools.GattServerConnectionConsts.RESULT_PERMISSIONS_KEY;
import static com.fitbit.bluetooth.fbgatt.tools.GattServerConnectionConsts.RESULT_PROPERTIES_KEY;
import static com.fitbit.bluetooth.fbgatt.tools.GattServerConnectionConsts.RESULT_SERVICE_UUID_KEY;
import static com.fitbit.bluetooth.fbgatt.tools.GattServerConnectionConsts.RESULT_VALUE_KEY;

/**
 * Utils class for parsing bt Gatt descriptors.
 */
public class BtGattDescriptorUtils {
    public String getStringPermissions(BluetoothGattDescriptor descriptor) {
        StringBuilder permissionBuilder = new StringBuilder();
        ArrayList<Integer> permissions = new ArrayList<>(8);
        int descriptorPermissions = descriptor.getPermissions();
        if ((descriptorPermissions & BluetoothGattDescriptor.PERMISSION_READ) == BluetoothGattDescriptor.PERMISSION_READ) {
            permissions.add(BluetoothGattDescriptor.PERMISSION_READ);
        }
        if ((descriptorPermissions & BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED) == BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED) {
            permissions.add(BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED);
        }
        if ((descriptorPermissions & BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM) == BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM) {
            permissions.add(BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM);
        }
        if ((descriptorPermissions & BluetoothGattDescriptor.PERMISSION_WRITE) == BluetoothGattDescriptor.PERMISSION_WRITE) {
            permissions.add(BluetoothGattDescriptor.PERMISSION_WRITE);
        }
        if ((descriptorPermissions & BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED) == BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED) {
            permissions.add(BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED);
        }
        if ((descriptorPermissions & BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM) == BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM) {
            permissions.add(BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM);
        }
        if ((descriptorPermissions & BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED) == BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED) {
            permissions.add(BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED);
        }
        if ((descriptorPermissions & BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED_MITM) == BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED_MITM) {
            permissions.add(BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED_MITM);
        }
        for (int i = 0; i < permissions.size(); i++) {
            int permission = permissions.get(i);
            switch (permission) {
                case BluetoothGattDescriptor.PERMISSION_READ:
                    permissionBuilder.append("read");
                    break;
                case BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED:
                    permissionBuilder.append("read-encrypted");
                    break;
                case BluetoothGattDescriptor.PERMISSION_WRITE:
                    permissionBuilder.append("write");
                    break;
                case BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM:
                    permissionBuilder.append("read-encrypted-mitm");
                    break;
                case BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED:
                    permissionBuilder.append("write-encrypted");
                    break;
                case BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM:
                    permissionBuilder.append("write-encrypted-mitm");
                    break;
                case BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED:
                    permissionBuilder.append("write-signed");
                    break;
                case BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED_MITM:
                    permissionBuilder.append("write-signed-mitm");
                    break;
                default:
                    permissionBuilder.append("unknown");
            }
            if (i < permissions.size() - 1) {
                permissionBuilder.append(", ");
            }
        }
        return permissionBuilder.toString();
    }

    public Map<String, Object> getMapForDescriptor(BluetoothGattService service, BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor descriptor, String descriptorPermission) {
        return new HashMap<String, Object>() {{
            put(RESULT_SERVICE_UUID_KEY, service.getUuid().toString());
            put(RESULT_CHARACTERISTIC_UUID_KEY, characteristic.getUuid().toString());
            put(RESULT_DESCRIPTOR_UUID_KEY, descriptor.getUuid().toString());
            put(RESULT_PROPERTIES_KEY, "N/A");
            put(RESULT_PERMISSIONS_KEY, descriptorPermission);
            put(RESULT_VALUE_KEY, Bytes.byteArrayToHexString(descriptor.getValue()));
        }};
    }
}
