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
import com.fitbit.bluetooth.fbgatt.tx.GattServerDisconnectTransaction;

/**
 * Stetho command for disconnecting the Gatt server from the peripheral.
 */
public class GattServerDisconnectCommand extends AbstractGattServerCommand {
    private final FitbitGatt fitbitGatt;

    public GattServerDisconnectCommand(FitbitGatt fitbitGatt, FitbitBluetoothDevice.DevicePropertiesChangedCallback devicePropertiesListener, PluginLoggerInterface logger) {
        super("gsd", "gatt-server-disconnect", "<mac>\n\nDescription: Will unregister the android application's gatt server instance from the peripheral with the given mac address.  Note, this does not mean that the peripheral is disconnected from the mobile device", logger, devicePropertiesListener);
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
            return new CommandRequest<>(new IllegalArgumentException("Bluetooth connection for mac" + mac + "not found."));
        }

        return new CommandRequest<>(new GattServerDisconnectTransaction(fitbitGatt.getServer(), GattState.DISCONNECTED, connection.getDevice()), CommandRequest.RequestState.SUCCESS);
    }

    @Override
    public String getSuccessMsg(GattServerTransactionConfigInterface gattServerTransactionConfigInterface) {
        return "Successfully Disconnected from " + gattServerTransactionConfigInterface.getMac();
    }

    @Override
    public String getFailureMsg(GattServerTransactionConfigInterface gattServerTransactionConfigInterface) {
        return "Failed Disconnecting from " + gattServerTransactionConfigInterface.getMac();
    }
}
