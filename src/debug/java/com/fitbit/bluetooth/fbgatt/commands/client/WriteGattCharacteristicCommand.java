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
import com.fitbit.bluetooth.fbgatt.tx.WriteGattCharacteristicTransaction;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.UUID;

/**
 * Stetho command for writing to a Gatt characteristic on the peripheral.
 */
public class WriteGattCharacteristicCommand extends AbstractGattClientPropertiesListenerCommand {
    private static final int MAC_ARG_INDEX = 0;
    private static final int SERVICE_UUID_ARG_INDEX = 1;
    private static final int CHARACTERISTIC_UUID_ARG_INDEX = 2;
    private static final int DATA_ARG_INDEX = 3;

    public WriteGattCharacteristicCommand(PluginLoggerInterface logger, FitbitBluetoothDevice.DevicePropertiesChangedCallback devicePropertiesChangedCallback) {
        super("wgc", "write-gatt-characteristic", "<mac>  <service uuid> <characteristic uuid> <data>\n\nDescription: Will write to a remote gatt characteristic hosted on the peripheral with the given service", logger, devicePropertiesChangedCallback);
    }

    @Override
    public CommandRequest<GattClientTransaction> buildTransaction(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        GattConnection connection = gattClientTransactionConfigInterface.getConnection();
        String serviceUuid = gattClientTransactionConfigInterface.getServiceUuid();
        String characteristicUuid = gattClientTransactionConfigInterface.getCharacteristicUuid();
        byte[] data = gattClientTransactionConfigInterface.getData();

        if (serviceUuid == null) {
            return new CommandRequest<>(new IllegalArgumentException("No service uuid provided"));
        }

        if (characteristicUuid == null) {
            return new CommandRequest<>(new IllegalArgumentException("No characteristic uuid provided"));
        }

        if (data == null) {
            return new CommandRequest<>(new IllegalArgumentException("No data provided"));
        }

        BluetoothGattCharacteristic btCharacteristic = connection.getRemoteGattServiceCharacteristic(UUID.fromString(serviceUuid), UUID.fromString(characteristicUuid));
        if (btCharacteristic == null) {
            return new CommandRequest<>(new IllegalStateException("No characteristic for the uuid" + characteristicUuid + "found"));
        }

        btCharacteristic.setValue(data);
        return new CommandRequest<>(new WriteGattCharacteristicTransaction(connection, GattState.WRITE_CHARACTERISTIC_SUCCESS, btCharacteristic), CommandRequest.RequestState.SUCCESS);
    }

    @Override
    public String getSuccessMsg(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        return "Successfully wrote to " + gattClientTransactionConfigInterface.getCharacteristicUuid() + " on " + gattClientTransactionConfigInterface.getMac();
    }

    @Override
    public String getFailureMsg(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        return "Failed writing to " + gattClientTransactionConfigInterface.getCharacteristicUuid() + " on " + gattClientTransactionConfigInterface.getMac();
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
    public int getDataArgIndex() {
        return DATA_ARG_INDEX;
    }
}
