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
import com.fitbit.bluetooth.fbgatt.tx.ReadGattServerCharacteristicDescriptorValueTransaction;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import java.util.UUID;
import androidx.annotation.NonNull;

/**
 * Stetho command for reading the Gatt server characteristic descriptor.
 */
public class ReadLocalGattServerCharacteristicDescriptorCommand extends AbstractGattServerCommand {
    public static final int SERVICE_UUID_ARG_INDEX = 0;
    public static final int CHARACTERISTIC_UUID_ARG_INDEX = 1;
    public static final int DESCRIPTOR_UUID_ARG_INDEX = 2;

    public ReadLocalGattServerCharacteristicDescriptorCommand(PluginLoggerInterface logger) {
        super("rlgscd", "read-local-gatt-server-characteristic-descriptor", "<service uuid> <characteristic uuid> <descriptor uuid>\n\nDescription: Will read off the value of a descriptor on a gatt server service characteristic descriptor on the mobile device", logger);
    }

    @NonNull
    @Override
    public CommandRequest<GattServerTransaction> buildTransaction(GattServerTransactionConfigInterface config) {
        String serviceString = config.getServiceUuid();
        String characteristicString = config.getCharacteristicUuid();
        String descriptorString = config.getDescriptorUuid();

        if (serviceString == null) {
            return new CommandRequest<>(new IllegalArgumentException("No service uuid provided"));
        }

        if (characteristicString == null) {
            return new CommandRequest<>(new IllegalArgumentException("No characteristic uuid provided"));
        }

        if (descriptorString == null) {
            return new CommandRequest<>(new IllegalArgumentException("No descriptor uuid provided"));
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
        BluetoothGattDescriptor localDescriptor = localCharacteristic.getDescriptor(UUID.fromString(descriptorString));
        if (localDescriptor == null) {
            return new CommandRequest<>(new IllegalStateException("No descriptor for the uuid " + descriptorString + " found"));
        }

        return new CommandRequest<>(new ReadGattServerCharacteristicDescriptorValueTransaction(serverConnection, GattState.READ_DESCRIPTOR_SUCCESS, localService, localCharacteristic, localDescriptor), CommandRequest.RequestState.SUCCESS);
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
