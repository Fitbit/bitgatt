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
import com.fitbit.bluetooth.fbgatt.tx.WriteGattServerCharacteristicValueTransaction;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import java.util.UUID;
import androidx.annotation.NonNull;

/**
 * Stetho command for writing a local Gatt server characteristic.
 */
public class WriteLocalGattServerCharacteristicCommand extends AbstractGattServerCommand {
    public static final int SERVICE_UUID_ARG_INDEX = 0;
    public static final int CHARACTERISTIC_UUID_ARG_INDEX = 1;
    public static final int DATA_ARG_INDEX = 2;

    public WriteLocalGattServerCharacteristicCommand(PluginLoggerInterface logger) {
        super("wlgsc", "write-local-gatt-server-characteristic", "<service uuid> <characteristic uuid> <data>\n\nDescription: Will write to a local gatt server service characteristic on a service on the mobile device", logger);
    }

    @NonNull
    @Override
    public CommandRequest<GattServerTransaction> buildTransaction(GattServerTransactionConfigInterface config) {
        String serviceString = config.getServiceUuid();
        String characteristicString = config.getCharacteristicUuid();
        byte[] data = config.getData();

        if (serviceString == null) {
            return new CommandRequest<>(new IllegalArgumentException("No service uuid provided"));
        }
        if (characteristicString == null) {
            return new CommandRequest<>(new IllegalArgumentException("No characteristic uuid provided"));
        }
        if (data == null) {
            return new CommandRequest<>(new IllegalArgumentException("No data provided"));
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
        localCharacteristic.setValue(data);

        return new CommandRequest<>(new WriteGattServerCharacteristicValueTransaction(serverConnection, GattState.WRITE_CHARACTERISTIC_SUCCESS, localService, localCharacteristic, data), CommandRequest.RequestState.SUCCESS);
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
    public int getDataArgIndex() {
        return DATA_ARG_INDEX;
    }

    @NonNull
    @Override
    public String getSuccessMsg(GattServerTransactionConfigInterface config) {
        return "Successfully wrote to " + config.getCharacteristicUuid() + " on " + config.getServiceUuid();
    }

    @NonNull
    @Override
    public String getFailureMsg(GattServerTransactionConfigInterface config) {
        return "Failed writing to " + config.getCharacteristicUuid() + " on " + config.getServiceUuid();
    }
}
