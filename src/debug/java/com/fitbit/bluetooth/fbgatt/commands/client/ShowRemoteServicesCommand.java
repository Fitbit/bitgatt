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

import com.fitbit.bluetooth.fbgatt.FitbitBluetoothDevice;
import com.fitbit.bluetooth.fbgatt.FitbitGatt;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.commands.AbstractGattCommand;
import com.fitbit.bluetooth.fbgatt.commands.PluginCommandConfig;
import com.fitbit.bluetooth.fbgatt.logger.PluginLoggerInterface;
import com.fitbit.bluetooth.fbgatt.util.Bytes;
import com.fitbit.bluetooth.fbgatt.utils.BtGattCharacteristicUtils;
import com.fitbit.bluetooth.fbgatt.utils.BtGattDescriptorUtils;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

import static com.fitbit.bluetooth.fbgatt.tools.GattServerConnectionConsts.PASS_STATUS;

/**
 * Stetho command for listing available services, characteristics and descriptors.
 */
public class ShowRemoteServicesCommand extends AbstractGattCommand {
    private final FitbitGatt fitbitGatt;
    private final FitbitBluetoothDevice.DevicePropertiesChangedCallback devicePropertiesListener;
    private final BtGattCharacteristicUtils characteristicUtils = new BtGattCharacteristicUtils();
    private final BtGattDescriptorUtils descriptorUtils = new BtGattDescriptorUtils();

    public ShowRemoteServicesCommand(FitbitGatt fitbitGatt, PluginLoggerInterface logger, FitbitBluetoothDevice.DevicePropertiesChangedCallback devicePropertiesListener) {
        super("srs", "show-remote-services", "<mac>\n\nDescription: Will show remote services, characteristics, and descriptors available post discovery", logger);
        this.fitbitGatt = fitbitGatt;
        this.devicePropertiesListener = devicePropertiesListener;
    }

    @Override
    public void run(PluginCommandConfig config) {
        String mac = null;
        if (config.getArgs().hasNext()) {
            mac = config.getArgs().next();
        }

        if (mac == null) {
            onError(config, new IllegalArgumentException("No bluetooth mac provided"));
            return;
        }

        if (!config.isJsonFormat()) {
            onMessage(config, String.format("| %1$36s | %2$36s | %3$36s | %4$32s | %5$32s | %6$32s\n",
                "Service UUID",
                "Characteristic UUID",
                "Descriptor UUID",
                "Permissions",
                "Properties",
                "Value")
            );
        }

        GattConnection connection = fitbitGatt.getConnectionForBluetoothAddress(mac);
        if (connection == null) {
            onError(config, new IllegalArgumentException("No valid connection for provided mac"));
            return;
        }

        connection.getDevice().addDevicePropertiesChangedListener(devicePropertiesListener);
        processServices(connection, config);
    }

    private void processServices(GattConnection connection, PluginCommandConfig config) {
        BluetoothGatt btGatt = connection.getGatt();
        if (btGatt == null) {
            onError(config, new IllegalStateException("BluetoothGatt was null"));
            return;
        }

        JSONArray jsonArray = new JSONArray();
        for (BluetoothGattService service : btGatt.getServices()) {
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                String permissions = characteristicUtils.getStringFromPermissions(characteristic);
                String properties = characteristicUtils.getStringFromProperties(characteristic);

                if (!config.isJsonFormat()) {
                    onMessage(config, String.format("| %1$36s | %2$36s | %3$36s | %4$32s | %5$32s | %6$32s\n",
                        service.getUuid().toString(),
                        characteristic.getUuid().toString(),
                        "N/A",
                        permissions,
                        properties,
                        Bytes.byteArrayToHexString(characteristic.getValue()))
                    );
                } else {
                    Map<String, Object> mappedCharacteristic = characteristicUtils.getMapForCharacteristic(service, characteristic, permissions, properties);
                    JSONObject jsonCharacteristic = jsonBuilder.buildJsonResult(mappedCharacteristic);
                    jsonArray.put(jsonCharacteristic);
                }

                for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                    String descriptorPermission = descriptorUtils.getStringPermissions(descriptor);
                    if (!config.isJsonFormat()) {
                        onMessage(config, String.format("| %1$36s | %2$36s | %3$36s | %4$32s | %5$32s | %6$32s\n",
                            service.getUuid().toString(),
                            characteristic.getUuid().toString(),
                            descriptor.getUuid().toString(),
                            descriptorPermission,
                            "N/A",
                            Bytes.byteArrayToHexString(descriptor.getValue()))
                        );
                    } else {
                        Map<String, Object> mappedDescriptor = descriptorUtils.getMapForDescriptor(service, characteristic, descriptor, descriptorPermission);
                        JSONObject jsonDescriptor = jsonBuilder.buildJsonResult(mappedDescriptor);
                        jsonArray.put(jsonDescriptor);
                    }
                }
            }
        }

        if (config.isJsonFormat()) {
            JSONObject jsonResult = jsonBuilder.makeJsonResult(getFullName(), PASS_STATUS, "", jsonArray);
            config.getConsumer().consumeJson(jsonResult);
        }

        connection.getDevice().removeDevicePropertiesChangedListener(devicePropertiesListener);
    }
}
