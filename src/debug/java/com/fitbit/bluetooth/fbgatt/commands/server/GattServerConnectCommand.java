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
import com.fitbit.bluetooth.fbgatt.GattServerTransaction;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.commands.CommandRequest;
import com.fitbit.bluetooth.fbgatt.logger.PluginLoggerInterface;
import com.fitbit.bluetooth.fbgatt.tx.GattServerConnectTransaction;

/**
 * Stetho command for connecting the Gatt server to a peripheral.
 */
public class GattServerConnectCommand extends AbstractGattServerCommand {
    private final FitbitGatt fitbitGatt;

    public GattServerConnectCommand(FitbitGatt fitbitGatt, PluginLoggerInterface logger) {
        super("gsc", "gatt-server-connect", "<mac>\n\nDescription: Will connect to the peripheral with the given mac address from the local gatt server", logger);
        this.fitbitGatt = fitbitGatt;
    }

    @Override
    public CommandRequest<GattServerTransaction> buildTransaction(GattServerTransactionConfigInterface gattServerTransactionConfigInterface) {
        String mac = gattServerTransactionConfigInterface.getMac();
        if (mac == null) {
            return new CommandRequest<>(new IllegalArgumentException("No bluetooth mac provided"));
        }

        GattConnection connection = fitbitGatt.getConnectionForBluetoothAddress(mac);
        if (connection == null) {
            return new CommandRequest<>(new IllegalStateException("GattConnection was null"));
        }

        FitbitBluetoothDevice device = connection.getDevice();
        if (device == null) {
            return new CommandRequest<>(new IllegalArgumentException("No device for mac address provided"));
        }

        return new CommandRequest<>(new GattServerConnectTransaction(gattServerTransactionConfigInterface.getServerConnection(), GattState.CONNECTED, device), CommandRequest.RequestState.SUCCESS);
    }

    @Override
    public String getSuccessMsg(GattServerTransactionConfigInterface gattServerTransactionConfigInterface) {
        return "Successfully Server Connected to " + gattServerTransactionConfigInterface.getMac();
    }

    @Override
    public String getFailureMsg(GattServerTransactionConfigInterface gattServerTransactionConfigInterface) {
        return "Failed Server Connecting to " + gattServerTransactionConfigInterface.getMac();
    }
}
