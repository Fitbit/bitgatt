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
import com.fitbit.bluetooth.fbgatt.GattClientTransaction;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.commands.CommandRequest;
import com.fitbit.bluetooth.fbgatt.commands.PluginCommandConfig;
import com.fitbit.bluetooth.fbgatt.logger.PluginLoggerInterface;
import com.fitbit.bluetooth.fbgatt.tx.ReadGattDescriptorTransaction;
import com.fitbit.bluetooth.fbgatt.util.Bytes;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static com.fitbit.bluetooth.fbgatt.tools.GattServerConnectionConsts.RESULT_DESCRIPTOR_VALUE;

/**
 * Stetho command for reading a peripheral's Gatt server descriptor for a service-characteristic.
 */
public class ReadGattClientDescriptorCommand extends AbstractGattClientPropertiesListenerCommand {
    private static final int MAC_ARG_INDEX = 0;
    private static final int SERVICE_UUID_ARG_INDEX = 1;
    private static final int CHARACTERISTIC_UUID_ARG_INDEX = 2;
    private static final int DESCRIPTOR_UUID_ARG_INDEX = 3;

    public ReadGattClientDescriptorCommand(FitbitBluetoothDevice.DevicePropertiesChangedCallback listener, PluginLoggerInterface logger) {
        super("rgcd", "read-gatt-client-descriptor", "<mac> <service uuid> <characteristic uuid> <descriptor uuid>\n\nDescription: Will read a value from a descriptor hosted on the peripheral's gatt server for a given service and characteristic", logger, listener);
    }

    @Override
    public CommandRequest<GattClientTransaction> buildTransaction(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        GattConnection connection = gattClientTransactionConfigInterface.getConnection();
        String serviceUuid = gattClientTransactionConfigInterface.getServiceUuid();
        String characteristicUuid = gattClientTransactionConfigInterface.getCharacteristicUuid();
        String descriptorUuid = gattClientTransactionConfigInterface.getDescriptorUuid();

        if (serviceUuid == null) {
            return new CommandRequest<>(new IllegalArgumentException("No service uuid provided"));
        }

        if (characteristicUuid == null) {
            return new CommandRequest<>(new IllegalArgumentException("No characteristic uuid provided"));
        }

        if (descriptorUuid == null) {
            return new CommandRequest<>(new IllegalArgumentException("No descriptor uuid provided"));
        }

        BluetoothGatt btGatt = connection.getGatt();
        if (btGatt == null) {
            return new CommandRequest<>(new IllegalArgumentException("BluetoothGatt was null"));
        }

        BluetoothGattService service = btGatt.getService(UUID.fromString(serviceUuid));
        if (service == null) {
            return new CommandRequest<>(new IllegalArgumentException("Server gatt service not found"));
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUuid));
        if (characteristic == null) {
            return new CommandRequest<>(new IllegalArgumentException("Server gatt characteristic not found"));
        }

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(descriptorUuid));
        if (descriptor == null) {
            return new CommandRequest<>(new IllegalArgumentException("Remote gatt descriptor not found"));
        }

        return new CommandRequest<>(new ReadGattDescriptorTransaction(connection, GattState.READ_DESCRIPTOR_SUCCESS, descriptor), CommandRequest.RequestState.SUCCESS);
    }

    @Override
    protected void onResult(PluginCommandConfig config, TransactionResult result) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(RESULT_DESCRIPTOR_VALUE, Bytes.byteArrayToHexString(result.getData()));
        responseHandler.onResponse(config, result, result.toString(), map);
    }

    @Override
    public String getSuccessMsg(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        return "Successfully read descriptor " + gattClientTransactionConfigInterface.getDescriptorUuid();
    }

    @Override
    public String getFailureMsg(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        return "Failed reading descriptor " + gattClientTransactionConfigInterface.getDescriptorUuid();
    }

    @Override
    public int getMacArgIndex() {
        return MAC_ARG_INDEX;
    }

    @Override
    public int getServiceArgIndex() {
        return SERVICE_UUID_ARG_INDEX;
    }

    @Override
    public int getCharacteristicArgIndex() {
        return CHARACTERISTIC_UUID_ARG_INDEX;
    }

    @Override
    public int getDescriptorArgIndex() {
        return DESCRIPTOR_UUID_ARG_INDEX;
    }
}
