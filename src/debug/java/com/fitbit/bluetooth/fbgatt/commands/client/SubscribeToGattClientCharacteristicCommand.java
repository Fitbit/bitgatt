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
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.commands.CommandRequest;
import com.fitbit.bluetooth.fbgatt.logger.PluginLoggerInterface;
import com.fitbit.bluetooth.fbgatt.tx.SubscribeToCharacteristicNotificationsTransaction;
import android.bluetooth.BluetoothGattCharacteristic;
import androidx.annotation.NonNull;

/**
 * Stetho command for subscribing to a particular Gatt client characteristic.
 */
public class SubscribeToGattClientCharacteristicCommand extends AbstractGattCharacteristicSubscriptionCommand {
    private static final int MAC_ARG_INDEX = 0;
    private static final int SERVICE_UUID_ARG_INDEX = 1;
    private static final int CHARACTERISTIC_UUID_ARG_INDEX = 2;

    public SubscribeToGattClientCharacteristicCommand(FitbitGatt fitbitGatt, PluginLoggerInterface logger, FitbitBluetoothDevice.DevicePropertiesChangedCallback devicePropertiesChangedCallback, ConnectionEventListener connectionEventListener) {
        super("stgcc", "subscribe-to-gatt-client-characteristic", "<mac> <service uuid> <characteristic uuid>\n\nDescription: Will subscribe to a particular gatt client characteristic, please remember that you must write to the notification descriptor on the given characteristic to truly have notifications enabled, this command will just route the notifications to this android process", fitbitGatt, logger, devicePropertiesChangedCallback, connectionEventListener);
    }

    @Override
    protected CommandRequest<GattClientTransaction> buildTransaction(@NonNull GattConnection connection, @NonNull BluetoothGattCharacteristic btCharacteristic) {
        return new CommandRequest<>(new SubscribeToCharacteristicNotificationsTransaction(connection, GattState.ENABLE_CHARACTERISTIC_NOTIFICATION_SUCCESS, btCharacteristic), CommandRequest.RequestState.SUCCESS);
    }

    @Override
    protected void preExecute(@NonNull GattConnection connection) {
        super.preExecute(connection);
        connection.registerConnectionEventListener(connectionEventListener);
    }

    @Override
    public String getSuccessMsg(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        return "Successfully subscribed to " + gattClientTransactionConfigInterface.getCharacteristicUuid() + " on " + gattClientTransactionConfigInterface.getMac();
    }

    @Override
    public String getFailureMsg(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        return "Failed subscribing to " + gattClientTransactionConfigInterface.getCharacteristicUuid() + " on " + gattClientTransactionConfigInterface.getMac();
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
