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
import com.fitbit.bluetooth.fbgatt.tx.ReadGattCharacteristicTransaction;
import com.fitbit.bluetooth.fbgatt.util.Bytes;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static com.fitbit.bluetooth.fbgatt.tools.GattServerConnectionConsts.RESULT_CHARACTERISTIC_VALUE;

/**
 * Stetho command for reading a peripheral's Gatt server characteristic.
 */
public class ReadGattClientCharacteristicCommand extends AbstractGattClientPropertiesListenerCommand {
    private static final int MAC_ARG_INDEX = 0;
    private static final int SERVICE_UUID_ARG_INDEX = 1;
    private static final int CHARACTERISTIC_UUID_ARG_INDEX = 2;

    public ReadGattClientCharacteristicCommand(FitbitBluetoothDevice.DevicePropertiesChangedCallback listener, PluginLoggerInterface logger) {
        super("rgcc", "read-gatt-client-characteristic", "<mac> <service uuid> <characteristic uuid>\n\nDescription: Will read a value from a characteristic hosted on the peripheral's gatt server for a given service", logger, listener);
    }

    @Override
    public CommandRequest<GattClientTransaction> buildTransaction(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        GattConnection connection = gattClientTransactionConfigInterface.getConnection();
        String serviceUuid = gattClientTransactionConfigInterface.getServiceUuid();
        String characteristicUuid = gattClientTransactionConfigInterface.getCharacteristicUuid();

        if (serviceUuid == null) {
            return new CommandRequest<>(new IllegalArgumentException("No service uuid provided"));
        }

        if (characteristicUuid == null) {
            return new CommandRequest<>(new IllegalArgumentException("No characteristic uuid provided"));
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

        return new CommandRequest<>(new ReadGattCharacteristicTransaction(connection, GattState.READ_CHARACTERISTIC_SUCCESS, characteristic), CommandRequest.RequestState.SUCCESS);
    }

    @Override
    protected void onResult(PluginCommandConfig config, TransactionResult result) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(RESULT_CHARACTERISTIC_VALUE, Bytes.byteArrayToHexString(result.getData()));
        responseHandler.onResponse(config, result, result.toString(), map);
    }

    @Override
    public String getSuccessMsg(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        return "Successfully read characteristic " + gattClientTransactionConfigInterface.getCharacteristicUuid();
    }

    @Override
    public String getFailureMsg(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        return "Failed reading characteristic " + gattClientTransactionConfigInterface.getCharacteristicUuid();
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
}
