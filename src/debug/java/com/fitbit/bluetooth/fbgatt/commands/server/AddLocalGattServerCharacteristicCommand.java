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
import com.fitbit.bluetooth.fbgatt.tx.AddGattServerServiceCharacteristicTransaction;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import java.util.UUID;
import androidx.annotation.NonNull;

/**
 * Stetho command for adding a local Gatt server characteristic.
 */
public class AddLocalGattServerCharacteristicCommand extends AbstractGattServerCommand {
    public static final int SERVICE_UUID_ARG_INDEX = 0;
    public static final int CHARACTERISTIC_UUID_ARG_INDEX = 1;
    public static final int PROPERTIES_ARG_INDEX = 2;
    public static final int PERMISSIONS_ARG_INDEX = 3;

    public AddLocalGattServerCharacteristicCommand(PluginLoggerInterface logger) {
        super("add-local-gatt-server-characteristic", "algsc", "<service uuid> <characteristic uuid> <properties int>\\n\" +\n" +
            "            \"Properties Values: PROPERTY_BROADCAST=1, PROPERTY_EXTENDED_PROPS=2, \" +\n" +
            "            \"PROPERTY_INDICATE=32, PROPERTY_NOTIFY=16, PROPERTY_READ=2, \" +\n" +
            "            \"PROPERTY_SIGNED_WRITE=64, PROPERTY_WRITE=8, PROPERTY_WRITE_NO_RESPONSE=4\\n\" +\n" +
            "            \" <permissions int>\\nPermission Values: PERMISSION_READ=1, \" +\n" +
            "            \"PERMISSION_READ_ENCRYPTED=2, PERMISSION_READ_ENCRYPTED_MITM=4, \" +\n" +
            "            \"PERMISSION_WRITE=16, PERMISSION_WRITE_ENCRYPTED=32, \" +\n" +
            "            \"PERMISSION_WRITE_ENCRYPTED_MITM=64, PERMISSION_WRITE_SIGNED=128, \" +\n" +
            "            \"PERMISSION_WRITE_SIGNED_MITM=256\\n\\nDescription: Will add a local gatt server characteristic to a gatt service on the mobile device", logger);
    }

    @NonNull
    @Override
    public CommandRequest<GattServerTransaction> buildTransaction(GattServerTransactionConfigInterface config) {
        String localServiceUuid = config.getServiceUuid();
        String localCharacteristicUuid = config.getCharacteristicUuid();
        int permissions = config.getPermissions();
        int properties = config.getProperties();

        if (localServiceUuid == null) {
            return new CommandRequest<>(new IllegalArgumentException("No local server service uuid provided"));
        }

        if (localCharacteristicUuid == null) {
            return new CommandRequest<>(new IllegalArgumentException("No characteristic uuid provided"));
        }

        if (permissions == Integer.MIN_VALUE) {
            return new CommandRequest<>(new IllegalArgumentException("No characteristic permissions provided"));
        }

        if (properties == Integer.MIN_VALUE) {
            return new CommandRequest<>(new IllegalArgumentException("No characteristic properties provided"));
        }

        GattServerConnection serverConnection = config.getServerConnection();
        BluetoothGattService localService = serverConnection.getServer().getService(UUID.fromString(localServiceUuid));
        if (localService == null) {
            return new CommandRequest<>(new IllegalStateException("No local service for the uuid" + localServiceUuid + "found"));
        }

        BluetoothGattCharacteristic localCharacteristic =
            new BluetoothGattCharacteristic(UUID.fromString(localCharacteristicUuid), properties, permissions);
        if (!localService.addCharacteristic(localCharacteristic)) {
            return new CommandRequest<>(new IllegalStateException("Couldn't add characteristic to service"));
        }

        return new CommandRequest<>(new AddGattServerServiceCharacteristicTransaction(serverConnection, GattState.ADD_SERVICE_CHARACTERISTIC_SUCCESS, localService, localCharacteristic), CommandRequest.RequestState.SUCCESS);
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
    public int getPermissionsArgIndex() {
        return PERMISSIONS_ARG_INDEX;
    }

    @Override
    public int getPropertiesArgIndex() {
        return PROPERTIES_ARG_INDEX;
    }

    @NonNull
    @Override
    public String getSuccessMsg(GattServerTransactionConfigInterface config) {
        return "Successfully added " + config.getCharacteristicUuid() + " to " + config.getServiceUuid();
    }

    @NonNull
    @Override
    public String getFailureMsg(GattServerTransactionConfigInterface config) {
        return "Failed adding " + config.getCharacteristicUuid() + " to " + config.getServiceUuid();
    }
}
