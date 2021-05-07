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
import com.fitbit.bluetooth.fbgatt.tx.WriteGattServerCharacteristicDescriptorValueTransaction;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import java.util.UUID;
import androidx.annotation.NonNull;

/**
 * Stetho command for writing a local Gatt server characteristic descriptor.
 */
public class WriteLocalGattServerCharacteristicDescriptorCommand extends AbstractGattServerCommand {
    public static final int SERVICE_UUID_ARG_INDEX = 0;
    public static final int CHARACTERISTIC_UUID_ARG_INDEX = 1;
    public static final int DESCRIPTOR_UUID_ARG_INDEX = 2;
    public static final int DATA_ARG_INDEX = 3;

    public WriteLocalGattServerCharacteristicDescriptorCommand(PluginLoggerInterface logger) {
        super("wlgscd", "write-local-gatt-server-characteristic-descriptor", "<service uuid> <characteristic uuid> <descriptor uuid> <data>\n\nDescription: Will write to a local gatt server descriptor on a characteristic on a service on the gatt server of the mobile device", logger);
    }

    @NonNull
    @Override
    public CommandRequest<GattServerTransaction> buildTransaction(GattServerTransactionConfigInterface config) {
        String localServiceUuid = config.getServiceUuid();
        String localCharacteristicUuid = config.getCharacteristicUuid();
        String localDescriptorUuid = config.getDescriptorUuid();
        byte[] data = config.getData();

        if (localServiceUuid == null) {
            return new CommandRequest<>(new IllegalArgumentException("No service uuid provided"));
        }

        if (localCharacteristicUuid == null) {
            return new CommandRequest<>(new IllegalArgumentException("No characteristic uuid provided"));
        }

        if (localDescriptorUuid == null) {
            return new CommandRequest<>(new IllegalArgumentException("No descriptor uuid provided"));
        }

        if (data == null) {
            return new CommandRequest<>(new IllegalArgumentException("No data provided"));
        }

        GattServerConnection serverConnection = config.getServerConnection();
        BluetoothGattService localService = serverConnection.getServer().getService(UUID.fromString(localServiceUuid));
        if (localService == null) {
            return new CommandRequest<>(new IllegalStateException("No service for the uuid " + localServiceUuid + " found"));
        }

        BluetoothGattCharacteristic localCharacteristic = localService.getCharacteristic(UUID.fromString(localCharacteristicUuid));
        if (localCharacteristic == null) {
            return new CommandRequest<>(new IllegalStateException("No characteristic for the uuid " + localCharacteristicUuid + " found"));
        }

        BluetoothGattDescriptor localDescriptor = localCharacteristic.getDescriptor(UUID.fromString(localDescriptorUuid));
        if (localDescriptor == null) {
            return new CommandRequest<>(new IllegalStateException("No descriptor for the uuid " + localDescriptorUuid + " found"));
        }
        localDescriptor.setValue(data);

        return new CommandRequest<>(new WriteGattServerCharacteristicDescriptorValueTransaction(serverConnection, GattState.WRITE_DESCRIPTOR_SUCCESS, localService, localCharacteristic, localDescriptor, data), CommandRequest.RequestState.SUCCESS);
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

    @Override
    public int getDataArgIndex() {
        return DATA_ARG_INDEX;
    }

    @NonNull
    @Override
    public String getSuccessMsg(GattServerTransactionConfigInterface config) {
        return "Successfully wrote to " + config.getDescriptorUuid() + " on " + config.getCharacteristicUuid() + " on " + config.getServiceUuid();
    }

    @NonNull
    @Override
    public String getFailureMsg(GattServerTransactionConfigInterface config) {
        return "Failed writing to " + config.getDescriptorUuid() + " on " + config.getCharacteristicUuid() + " on " + config.getServiceUuid();
    }
}
