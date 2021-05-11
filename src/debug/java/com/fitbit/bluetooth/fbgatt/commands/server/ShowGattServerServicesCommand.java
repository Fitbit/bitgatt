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

import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.fitbit.bluetooth.fbgatt.tools.GattServerConnectionConsts.PASS_STATUS;
import static com.fitbit.bluetooth.fbgatt.tools.GattServerConnectionConsts.RESULT_SERVICE_TYPE_KEY;
import static com.fitbit.bluetooth.fbgatt.tools.GattServerConnectionConsts.RESULT_SERVICE_UUID_KEY;

/**
 * Stetho command for listing the services hosted Gatt server.
 */
public class ShowGattServerServicesCommand extends AbstractGattCommand {
    public ShowGattServerServicesCommand(PluginLoggerInterface logger) {
        super("sgss", "show-gatt-server-services", "Description: Will list off hosted gatt server services on the mobile device", logger);
    }

    @Override
    public void run(PluginCommandConfig config) {
        if (!config.isJsonFormat()) {
            onMessage(config, String.format("| %1$32s | %2$32s |\n", "Service UUID", "Type"));
        }

        GattServerConnection connection = config.getServerConnection();
        BluetoothGattServer gattServer = connection.getServer();
        JSONArray jsonArray = new JSONArray();
        for (BluetoothGattService service : gattServer.getServices()) {
            String serviceUuid = service.getUuid().toString();
            String type;
            switch (service.getType()) {
                case BluetoothGattService.SERVICE_TYPE_PRIMARY:
                    type = "primary";
                    break;
                case BluetoothGattService.SERVICE_TYPE_SECONDARY:
                    type = "secondary";
                    break;
                default:
                    type = "unknown";
            }

            if (!config.isJsonFormat()) {
                onMessage(config, String.format("| %1$32s | %2$32s |\n", serviceUuid, type));
            } else {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put(RESULT_SERVICE_UUID_KEY, serviceUuid);
                map.put(RESULT_SERVICE_TYPE_KEY, type);
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
}
