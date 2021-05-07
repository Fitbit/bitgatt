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
import com.fitbit.bluetooth.fbgatt.GattServerTransaction;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.commands.CommandRequest;
import com.fitbit.bluetooth.fbgatt.logger.PluginLoggerInterface;
import com.fitbit.bluetooth.fbgatt.tx.ReadGattServerCharacteristicValueTransaction;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import java.util.UUID;
import androidx.annotation.NonNull;

/**
 * Stetho command for reading the local Gatt server characteristic.
 */
public class ReadLocalGattServerCharacteristicCommand extends AbstractGattServerCommand {
    public static final int SERVICE_UUID_ARG_INDEX = 0;
    public static final int CHARACTERISTIC_UUID_ARG_INDEX = 1;

    public ReadLocalGattServerCharacteristicCommand(PluginLoggerInterface logger) {
        super("rlgsc", "read-local-gatt-server-characteristic", "<service uuid> <characteristic uuid>\n\nDescription: Will read out the data value of a local gatt server service characteristic on the mobile device", logger);
    }

    @NonNull
    @Override
    public CommandRequest<GattServerTransaction> buildTransaction(GattServerTransactionConfigInterface config) {
        String serviceString = config.getServiceUuid();
        String characteristicString = config.getCharacteristicUuid();

        if (serviceString == null) {
            return new CommandRequest<>(new IllegalArgumentException("No service uuid provided"));
        }

        if (characteristicString == null) {
            return new CommandRequest<>(new IllegalArgumentException("No characteristic uuid provided"));
        }

        GattServerConnection serverConnection = config.getServerConnection();
        BluetoothGattService localService = serverConnection.getServer().getService(UUID.fromString(serviceString));
        if (localService == null) {
            return new CommandRequest<>(new IllegalStateException("No service for the uuid " + serviceString + " found"));
        }

        BluetoothGattCharacteristic localCharacteristic = localService.getCharacteristic(UUID.fromString(characteristicString));
        if (localCharacteristic == null) {
            return new CommandRequest<>(new IllegalStateException("No characteristic for the uuid " + characteristicString + " found"));
        }

        return new CommandRequest<>(new ReadGattServerCharacteristicValueTransaction(serverConnection, GattState.ADD_SERVICE_CHARACTERISTIC_SUCCESS, localService, localCharacteristic), CommandRequest.RequestState.SUCCESS);
    }

    @Override
    public int getServiceArgIndex() {
        return SERVICE_UUID_ARG_INDEX;
    }

    @Override
    public int getCharacteristicArgIndex() {
        return CHARACTERISTIC_UUID_ARG_INDEX;
    }

    @NonNull
    @Override
    public String getSuccessMsg(GattServerTransactionConfigInterface config) {
        return "Successfully read " + config.getCharacteristicUuid() + " on " + config.getServiceUuid();
    }

    @NonNull
    @Override
    public String getFailureMsg(GattServerTransactionConfigInterface config) {
        return "Failed reading " + config.getCharacteristicUuid() + " from " + config.getServiceUuid();
    }
}
