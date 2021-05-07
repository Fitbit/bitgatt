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
 * Utils class for parsing permissions for bt Gatt characteristics.
 */
public class BtGattCharacteristicUtils {
    public String getStringFromPermissions(BluetoothGattCharacteristic characteristic) {
        StringBuilder permissionBuilder = new StringBuilder();
        ArrayList<Integer> permissions = new ArrayList<>(8);
        int characteristicPermissions = characteristic.getPermissions();
        if ((characteristicPermissions & BluetoothGattCharacteristic.PERMISSION_READ) == BluetoothGattCharacteristic.PERMISSION_READ) {
            permissions.add(BluetoothGattCharacteristic.PERMISSION_READ);
        }
        if ((characteristicPermissions & BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED) == BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED) {
            permissions.add(BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
        }
        if ((characteristicPermissions & BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM) == BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM) {
            permissions.add(BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM);
        }
        if ((characteristicPermissions & BluetoothGattCharacteristic.PERMISSION_WRITE) == BluetoothGattCharacteristic.PERMISSION_WRITE) {
            permissions.add(BluetoothGattCharacteristic.PERMISSION_WRITE);
        }
        if ((characteristicPermissions & BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED) == BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED) {
            permissions.add(BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
        }
        if ((characteristicPermissions & BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM) == BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM) {
            permissions.add(BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM);
        }
        if ((characteristicPermissions & BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED) == BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED) {
            permissions.add(BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED);
        }
        if ((characteristicPermissions & BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM) == BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM) {
            permissions.add(BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM);
        }
        for (int i = 0; i < permissions.size(); i++) {
            int permission = permissions.get(i);
            switch (permission) {
                case BluetoothGattCharacteristic.PERMISSION_READ:
                    permissionBuilder.append("read");
                    break;
                case BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED:
                    permissionBuilder.append("read-encrypted");
                    break;
                case BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM:
                    permissionBuilder.append("read-encrypted-mitm");
                    break;
                case BluetoothGattCharacteristic.PERMISSION_WRITE:
                    permissionBuilder.append("write");
                    break;
                case BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED:
                    permissionBuilder.append("write-encrypted");
                    break;
                case BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM:
                    permissionBuilder.append("write-encrypted-mitm");
                    break;
                case BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED:
                    permissionBuilder.append("write-signed");
                    break;
                case BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM:
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

    public String getStringFromProperties(BluetoothGattCharacteristic characteristic) {
        StringBuilder propertyBuilder = new StringBuilder();
        ArrayList<Integer> properties = new ArrayList<>(8);
        int characteristicProperties = characteristic.getProperties();
        if ((characteristicProperties & BluetoothGattCharacteristic.PROPERTY_READ) == BluetoothGattCharacteristic.PROPERTY_READ) {
            properties.add(BluetoothGattCharacteristic.PROPERTY_READ);
        }
        if ((characteristicProperties & BluetoothGattCharacteristic.PROPERTY_WRITE) == BluetoothGattCharacteristic.PROPERTY_WRITE) {
            properties.add(BluetoothGattCharacteristic.PROPERTY_WRITE);
        }
        if ((characteristicProperties & BluetoothGattCharacteristic.PROPERTY_BROADCAST) == BluetoothGattCharacteristic.PROPERTY_BROADCAST) {
            properties.add(BluetoothGattCharacteristic.PROPERTY_BROADCAST);
        }
        if ((characteristicProperties & BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS) == BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS) {
            properties.add(BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS);
        }
        if ((characteristicProperties & BluetoothGattCharacteristic.PROPERTY_INDICATE) == BluetoothGattCharacteristic.PROPERTY_INDICATE) {
            properties.add(BluetoothGattCharacteristic.PROPERTY_INDICATE);
        }
        if ((characteristicProperties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == BluetoothGattCharacteristic.PROPERTY_NOTIFY) {
            properties.add(BluetoothGattCharacteristic.PROPERTY_NOTIFY);
        }
        if ((characteristicProperties & BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) == BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) {
            properties.add(BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE);
        }
        if ((characteristicProperties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) {
            properties.add(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE);
        }
        for (int i = 0; i < properties.size(); i++) {
            int property = properties.get(i);
            switch (property) {
                case BluetoothGattCharacteristic.PROPERTY_READ:
                    propertyBuilder.append("read");
                    break;
                case BluetoothGattCharacteristic.PROPERTY_WRITE:
                    propertyBuilder.append("write");
                    break;
                case BluetoothGattCharacteristic.PROPERTY_BROADCAST:
                    propertyBuilder.append("broadcast");
                    break;
                case BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS:
                    propertyBuilder.append("extended-props");
                    break;
                case BluetoothGattCharacteristic.PROPERTY_NOTIFY:
                    propertyBuilder.append("notify");
                    break;
                case BluetoothGattCharacteristic.PROPERTY_INDICATE:
                    propertyBuilder.append("indicate");
                    break;
                case BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE:
                    propertyBuilder.append("write-signed");
                    break;
                case BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE:
                    propertyBuilder.append("write-no-response");
                    break;
                default:
                    propertyBuilder.append("unknown");
            }
            if (i < properties.size() - 1) {
                propertyBuilder.append(", ");
            }
        }
        return propertyBuilder.toString();
    }

    public Map<String, Object> getMapForCharacteristic(BluetoothGattService service, BluetoothGattCharacteristic characteristic, String permissions, String properties) {
        return new HashMap<String, Object>() {{
            put(RESULT_SERVICE_UUID_KEY, service.getUuid().toString());
            put(RESULT_CHARACTERISTIC_UUID_KEY, characteristic.getUuid().toString());
            put(RESULT_DESCRIPTOR_UUID_KEY, "N/A");
            put(RESULT_PERMISSIONS_KEY, permissions);
            put(RESULT_PROPERTIES_KEY, properties);
            put(RESULT_VALUE_KEY, Bytes.byteArrayToHexString(characteristic.getValue()));
        }};
    }
}
