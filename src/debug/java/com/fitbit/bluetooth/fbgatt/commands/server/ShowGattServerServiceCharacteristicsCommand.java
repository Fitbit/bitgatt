/*
 *
 *  Copyright 2021 Fitbit, Inc. All rights reserved.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package com.fitbit.bluetooth.fbgatt.commands.server;

import com.fitbit.bluetooth.fbgatt.GattServerConnection;
import com.fitbit.bluetooth.fbgatt.commands.AbstractGattCommand;
import com.fitbit.bluetooth.fbgatt.commands.PluginCommandConfig;
import com.fitbit.bluetooth.fbgatt.logger.PluginLoggerInterface;
import com.fitbit.bluetooth.fbgatt.tools.ConsumerInterface;
import com.fitbit.bluetooth.fbgatt.util.Bytes;
import com.fitbit.bluetooth.fbgatt.utils.JsonBuilder;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static com.fitbit.bluetooth.fbgatt.tools.GattServerConnectionConsts.PASS_STATUS;
import static com.fitbit.bluetooth.fbgatt.tools.GattServerConnectionConsts.RESULT_CHARACTERISTIC_UUID_KEY;
import static com.fitbit.bluetooth.fbgatt.tools.GattServerConnectionConsts.RESULT_PERMISSIONS_KEY;
import static com.fitbit.bluetooth.fbgatt.tools.GattServerConnectionConsts.RESULT_PROPERTIES_KEY;
import static com.fitbit.bluetooth.fbgatt.tools.GattServerConnectionConsts.RESULT_VALUE_KEY;

/**
 * Stetho command for listing the characteristics hosted by the local Gatt server service.
 */
public class ShowGattServerServiceCharacteristicsCommand extends AbstractGattCommand {
    private final JsonBuilder jsonBuilder;

    public ShowGattServerServiceCharacteristicsCommand(PluginLoggerInterface logger) {
        super("sgssc", "show-gatt-server-service-characteristics", "<service uuid>\n\nDescription: Will list off characteristics hosted by the provided local gatt server service", logger);
        this.jsonBuilder = new JsonBuilder(logger);
    }

    @Override
    public void run(PluginCommandConfig config) {
        if (!config.isJsonFormat()) {
            onMessage(config, String.format("| %1$32s | %2$32s | %3$32s | %4$32s |\n", "Characteristic UUID",
                "Permissions",
                "Properties",
                "Value"));
        }

        Iterator<String> args = config.getArgs();
        String serviceName = null;
        if (args.hasNext()) {
            serviceName = args.next();
        }

        if (serviceName == null) {
            onError(config, new IllegalArgumentException("No service uuid provided"));
            return;
        }

        JSONArray jsonArray = new JSONArray();

        GattServerConnection connection = config.getServerConnection();
        BluetoothGattServer gattServer = connection.getServer();
        BluetoothGattService service = gattServer.getService(UUID.fromString(serviceName));
        if (service == null) {
            onError(config, new IllegalArgumentException("BluetoothGattService was null"));
            return;
        }

        for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
            String permissions = getPermissions(characteristic);
            String properties = getProperties(characteristic);

            if (!config.isJsonFormat()) {
                onMessage(config, String.format("| %1$32s | %2$32s | %3$32s | %4$32s |\n", characteristic.getUuid().toString(), permissions, properties, Bytes.byteArrayToHexString(characteristic.getValue())));
            } else {
                Map<String, Object> map = new LinkedHashMap<String, Object>() {{
                    put(RESULT_CHARACTERISTIC_UUID_KEY, characteristic.getUuid().toString());
                    put(RESULT_PERMISSIONS_KEY, permissions);
                    put(RESULT_PROPERTIES_KEY, properties);
                    put(RESULT_VALUE_KEY, Bytes.byteArrayToHexString(characteristic.getValue()));
                }};
                JSONObject jsonObject = jsonBuilder.buildJsonResult(map);
                jsonArray.put(jsonObject);
            }
        }

        ConsumerInterface consumer = config.getConsumer();
        if (config.isJsonFormat()) {
            consumer.consumeJson(jsonBuilder.makeJsonResult(getShortName(), PASS_STATUS, "", jsonArray));
        } else {
            consumer.consumeResult(jsonArray.toString());
        }
    }

    private String getPermissions(BluetoothGattCharacteristic characteristic) {
        String permission;
        switch (characteristic.getPermissions()) {
            case BluetoothGattCharacteristic.PERMISSION_READ:
                permission = "read";
                break;
            case BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED:
                permission = "read-encrypted";
                break;
            case BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM:
                permission = "read-encrypted-mitm";
                break;
            case BluetoothGattCharacteristic.PERMISSION_WRITE:
                permission = "write";
                break;
            case BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED:
                permission = "write-encrypted";
                break;
            case BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM:
                permission = "write-encrypted-mitm";
                break;
            default:
                permission = "unknown";
        }

        return permission;
    }

    private String getProperties(BluetoothGattCharacteristic characteristic) {
        String properties;
        switch (characteristic.getProperties()) {
            case BluetoothGattCharacteristic.PROPERTY_BROADCAST:
                properties = "broadcast";
                break;
            case BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS:
                properties = "extended-props";
                break;
            case BluetoothGattCharacteristic.PROPERTY_INDICATE:
                properties = "indicate";
                break;
            case BluetoothGattCharacteristic.PROPERTY_NOTIFY:
                properties = "notify";
                break;
            case BluetoothGattCharacteristic.PROPERTY_READ:
                properties = "read";
                break;
            case BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE:
                properties = "signed-write";
                break;
            case BluetoothGattCharacteristic.PROPERTY_WRITE:
                properties = "write";
                break;
            case BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE:
                properties = "write-no-response";
                break;
            default:
                properties = "unknown";
        }

        return properties;
    }
}
