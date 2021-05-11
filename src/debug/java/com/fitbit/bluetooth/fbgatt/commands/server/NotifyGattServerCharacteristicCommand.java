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

import com.fitbit.bluetooth.fbgatt.FitbitBluetoothDevice;
import com.fitbit.bluetooth.fbgatt.FitbitGatt;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattServerConnection;
import com.fitbit.bluetooth.fbgatt.GattServerTransaction;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.commands.CommandRequest;
import com.fitbit.bluetooth.fbgatt.logger.PluginLoggerInterface;
import com.fitbit.bluetooth.fbgatt.tx.NotifyGattServerCharacteristicTransaction;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import java.util.UUID;
import androidx.annotation.NonNull;

/**
 * Stetho command for notifying a service characteristic.
 */
public class NotifyGattServerCharacteristicCommand extends AbstractGattServerCommand {
    public static final int MAC_ARG_INDEX = 0;
    public static final int SERVICE_UUID_ARG_INDEX = 1;
    public static final int CHARACTERISTIC_UUID_ARG_INDEX = 2;

    private final FitbitGatt fitbitGatt;

    public NotifyGattServerCharacteristicCommand(FitbitGatt fitbitGatt, PluginLoggerInterface logger, FitbitBluetoothDevice.DevicePropertiesChangedCallback devicePropertiesListener) {
        super("ngsc", "notify-gatt-server-characteristic", "<mac> <service uuid> <characteristic uuid>\n\nDescription: Will notify on the given server service characteristic that something has changed, this will tell the peripheral that the service has had something done to it if the peripheral has subscribed to notifications on the characteristic that is being notified.", logger, devicePropertiesListener);
        this.fitbitGatt = fitbitGatt;
    }

    @NonNull
    @Override
    public CommandRequest<GattServerTransaction> buildTransaction(GattServerTransactionConfigInterface config) {
        String mac = config.getMac();
        String localServiceUuid = config.getServiceUuid();
        String localCharacteristicUuid = config.getCharacteristicUuid();

        if (mac == null) {
            return new CommandRequest<>(new IllegalArgumentException("No bluetooth mac provided"));
        }

        if (localServiceUuid == null) {
            return new CommandRequest<>(new IllegalArgumentException("No local server service uuid provided"));
        }

        if (localCharacteristicUuid == null) {
            return new CommandRequest<>(new IllegalArgumentException("No characteristic uuid provided"));
        }

        GattServerConnection serverConnection = config.getServerConnection();
        BluetoothGattService localService = serverConnection.getServer().getService(UUID.fromString(localServiceUuid));
        if (localService == null) {
            return new CommandRequest<>(new IllegalArgumentException("Server gatt service not found"));
        }

        BluetoothGattCharacteristic localCharacteristic = localService.getCharacteristic(UUID.fromString(localCharacteristicUuid));
        if (localCharacteristic == null) {
            return new CommandRequest<>(new IllegalArgumentException("Server gatt characteristic not found"));
        }

        GattConnection connection = fitbitGatt.getConnectionForBluetoothAddress(mac);
        if (connection == null) {
            return new CommandRequest<>(new IllegalArgumentException("Bluetooth connection for mac" + mac + "not found."));
        }

        return new CommandRequest<>(new NotifyGattServerCharacteristicTransaction(fitbitGatt.getServer(), connection.getDevice(), GattState.NOTIFY_CHARACTERISTIC_SUCCESS, localCharacteristic, false), CommandRequest.RequestState.SUCCESS);
    }

    @NonNull
    @Override
    public String getSuccessMsg(GattServerTransactionConfigInterface config) {
        return "Successfully notified gatt server characteristic " + config.getCharacteristicUuid();
    }

    @NonNull
    @Override
    public String getFailureMsg(GattServerTransactionConfigInterface config) {
        return "Failed notifying gatt server characteristic " + config.getCharacteristicUuid();
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
