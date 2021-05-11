/*
 *
 *  Copyright 2021 Fitbit, Inc. All rights reserved.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package com.fitbit.bluetooth.fbgatt.commands.client;

import com.fitbit.bluetooth.fbgatt.FitbitGatt;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.commands.AbstractGattCommand;
import com.fitbit.bluetooth.fbgatt.commands.PluginCommandConfig;
import com.fitbit.bluetooth.fbgatt.logger.PluginLoggerInterface;
import com.fitbit.bluetooth.fbgatt.tools.ConsumerInterface;
import com.fitbit.bluetooth.fbgatt.tools.GattServerConnectionConsts;
import com.fitbit.bluetooth.fbgatt.util.GattUtils;
import com.fitbit.bluetooth.fbgatt.utils.GattConnectionUtils;
import com.fitbit.bluetooth.fbgatt.utils.JsonBuilder;

import android.bluetooth.BluetoothDevice;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.fitbit.bluetooth.fbgatt.tools.GattServerConnectionConsts.FAIL_STATUS;
import static com.fitbit.bluetooth.fbgatt.tools.GattServerConnectionConsts.PASS_STATUS;

/**
 * Common behaviour of the Find Nearby Devices commands.
 */
abstract class AbstractFindNearbyDevicesCommand extends AbstractGattCommand {
    protected final FitbitGatt fitbitGatt;
    private final JsonBuilder jsonBuilder;

    protected AbstractFindNearbyDevicesCommand(String shortName, String fullName, String description, FitbitGatt fitbitGatt, PluginLoggerInterface logger) {
        super(shortName, fullName, description, logger);
        this.fitbitGatt = fitbitGatt;
        this.jsonBuilder = new JsonBuilder(logger);
    }

    @Override
    public final void run(PluginCommandConfig config) {
        StringBuilder builder = new StringBuilder();
        JSONArray jsonArray = new JSONArray();
        String status = PASS_STATUS;
        String error = "";

        try {
            GattConnectionUtils connectionUtils = config.getConnectionUtils();
            for (String mac : connectionUtils.getConnectionMacs()) {
                GattConnection gattConnection = connectionUtils.getConnectionForMac(mac);
                BluetoothDevice device = fitbitGatt.getBluetoothDevice(mac);
                if (device == null) {
                    continue;
                }

                GattUtils gattUtils = new GattUtils();
                GattServerConnectionConsts.DeviceType type = connectionUtils.getDeviceType(device);

                String deviceName = gattUtils.debugSafeGetBtDeviceName(device);
                String deviceAddress = device.getAddress();
                String origin = gattConnection.getDevice().getOrigin().name();
                String rssi = String.valueOf(gattConnection.getDevice().getRssi());
                Map<String, Object> map = new LinkedHashMap<String, Object>() {{
                    put("name", deviceName);
                    put("address", deviceAddress);
                    put("btname", type.getValue());
                    put("origin", origin);
                    put("rssi", rssi);
                }};

                JSONObject jsonObject = jsonBuilder.buildJsonResult(map);
                jsonArray.put(jsonObject);
                if (!config.isJsonFormat()) {
                    onMessage(config, String.format("| %1$32s | %2$32s | %3$32s | %4$32s | %5$32s\n", deviceName, deviceAddress, type, origin, rssi));
                }
            }
        } catch (Exception e) {
            status = FAIL_STATUS;
            error = Arrays.toString(e.getStackTrace());
        }

        ConsumerInterface consumer = config.getConsumer();
        if (config.isJsonFormat()) {
            JSONObject result = jsonBuilder.makeJsonResult(getShortName(), status, error, jsonArray);
            consumer.consumeJson(result);
        } else {
            consumer.consumeResult(builder.toString());
        }

        onPostProcessing(config);
    }

    /**
     * Method to be called after building the list of devices.
     */
    abstract void onPostProcessing(PluginCommandConfig config);
}
