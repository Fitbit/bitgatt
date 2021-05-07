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

import com.fitbit.bluetooth.fbgatt.ConnectionEventListener;
import com.fitbit.bluetooth.fbgatt.FitbitBluetoothDevice;
import com.fitbit.bluetooth.fbgatt.FitbitGatt;
import com.fitbit.bluetooth.fbgatt.GattClientTransaction;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.commands.CommandRequest;
import com.fitbit.bluetooth.fbgatt.logger.PluginLoggerInterface;
import android.bluetooth.BluetoothGattCharacteristic;
import java.util.UUID;
import androidx.annotation.NonNull;

/**
 * Base implementation for commands related to characteristic subscription management.
 */
abstract class AbstractGattCharacteristicSubscriptionCommand extends AbstractGattClientPropertiesListenerCommand {
    private final FitbitGatt fitbitGatt;
    protected final ConnectionEventListener connectionEventListener;

    AbstractGattCharacteristicSubscriptionCommand(String shortName, String fullName, String description, FitbitGatt fitbitGatt, PluginLoggerInterface logger, FitbitBluetoothDevice.DevicePropertiesChangedCallback devicePropertiesChangedCallback, ConnectionEventListener connectionEventListener) {
        super(shortName, fullName, description, logger, devicePropertiesChangedCallback);
        this.fitbitGatt = fitbitGatt;
        this.connectionEventListener = connectionEventListener;
    }

    @Override
    public final CommandRequest<GattClientTransaction> buildTransaction(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        String mac = gattClientTransactionConfigInterface.getMac();
        String serviceUuid = gattClientTransactionConfigInterface.getServiceUuid();
        String characteristicUuid = gattClientTransactionConfigInterface.getCharacteristicUuid();

        if (mac == null) {
            return new CommandRequest<>(new IllegalArgumentException("No bluetooth mac provided"));
        }

        if (serviceUuid == null) {
            return new CommandRequest<>(new IllegalArgumentException("No service uuid provided"));
        }

        if (characteristicUuid == null) {
            return new CommandRequest<>(new IllegalArgumentException("No characteristic uuid provided"));
        }

        GattConnection connection = fitbitGatt.getConnectionForBluetoothAddress(mac);
        if (connection == null) {
            return new CommandRequest<>(new IllegalArgumentException("No valid connection for provided mac"));
        }

        BluetoothGattCharacteristic btCharacteristic = connection.getRemoteGattServiceCharacteristic(UUID.fromString(serviceUuid), UUID.fromString(characteristicUuid));
        if (btCharacteristic == null) {
            return new CommandRequest<>(new IllegalStateException("No characteristic for the uuid" + characteristicUuid + "found"));
        }

        return buildTransaction(connection, btCharacteristic);
    }

    @Override
    public abstract int getMacArgIndex();

    @Override
    public abstract int getServiceArgIndex();

    @Override
    public abstract int getCharacteristicArgIndex();

    protected abstract CommandRequest<GattClientTransaction> buildTransaction(@NonNull GattConnection connection, @NonNull BluetoothGattCharacteristic btCharacteristic);
}
