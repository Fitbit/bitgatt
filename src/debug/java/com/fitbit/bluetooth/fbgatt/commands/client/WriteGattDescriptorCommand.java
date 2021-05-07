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
import com.fitbit.bluetooth.fbgatt.commands.CommandRequest;
import com.fitbit.bluetooth.fbgatt.logger.PluginLoggerInterface;
import com.fitbit.bluetooth.fbgatt.tx.WriteGattDescriptorTransaction;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import java.util.UUID;

/**
 * Stetho command for writing to a Gatt descriptor on the peripheral.
 */
public class WriteGattDescriptorCommand extends AbstractGattClientPropertiesListenerCommand {
    private static final int MAC_ARG_INDEX = 0;
    private static final int SERVICE_UUID_ARG_INDEX = 1;
    private static final int CHARACTERISTIC_UUID_ARG_INDEX = 2;
    private static final int DESCRIPTOR_ARG_INDEX = 3;
    private static final int DATA_ARG_INDEX = 4;

    public WriteGattDescriptorCommand(PluginLoggerInterface logger, FitbitBluetoothDevice.DevicePropertiesChangedCallback devicePropertiesChangedCallback) {
        super("wgd", "write-gatt-descriptor", "<mac>  <service uuid> <characteristic uuid> <descriptor uuid> <data>\n\nDescription: Will write to a remote gatt descriptor hosted on the peripheral's gatt server with the given service and characteristic", logger, devicePropertiesChangedCallback);
    }

    @Override
    public CommandRequest<GattClientTransaction> buildTransaction(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        GattConnection connection = gattClientTransactionConfigInterface.getConnection();
        String serviceUuid = gattClientTransactionConfigInterface.getServiceUuid();
        String characteristicUuid = gattClientTransactionConfigInterface.getCharacteristicUuid();
        byte[] data = gattClientTransactionConfigInterface.getData();
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

        if (data == null) {
            return new CommandRequest<>(new IllegalArgumentException("No data provided"));
        }

        BluetoothGattCharacteristic btCharacteristic = connection.getRemoteGattServiceCharacteristic(UUID.fromString(serviceUuid), UUID.fromString(characteristicUuid));
        if (btCharacteristic == null) {
            return new CommandRequest<>(new IllegalStateException("No characteristic for the uuid" + characteristicUuid + "found"));
        }

        BluetoothGattDescriptor btDescriptor = btCharacteristic.getDescriptor(UUID.fromString(descriptorUuid));
        if (btDescriptor == null) {
            return new CommandRequest<>(new IllegalStateException("No descriptor for the uuid" + descriptorUuid + "found"));
        }

        btDescriptor.setValue(data);
        return new CommandRequest<>(new WriteGattDescriptorTransaction(connection, GattState.WRITE_DESCRIPTOR_SUCCESS, btDescriptor), CommandRequest.RequestState.SUCCESS);
    }

    @Override
    public String getSuccessMsg(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        return null;
    }

    @Override
    public String getFailureMsg(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        return null;
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
        return DESCRIPTOR_ARG_INDEX;
    }

    @Override
    public int getDataArgIndex() {
        return DATA_ARG_INDEX;
    }
}
