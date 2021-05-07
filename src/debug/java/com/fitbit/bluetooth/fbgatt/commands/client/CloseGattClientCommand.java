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
import com.fitbit.bluetooth.fbgatt.tx.CloseGattTransaction;

/**
 * Stetho command for closing the Gatt client.
 */
public class CloseGattClientCommand extends AbstractGattClientPropertiesListenerCommand {
    public CloseGattClientCommand(FitbitBluetoothDevice.DevicePropertiesChangedCallback callback, PluginLoggerInterface logger) {
        super("cgc", "close-gatt-client", "<mac>\n\nDescription: Will close the gatt client and release the android client_if handle", logger, callback);
    }

    @Override
    public CommandRequest<GattClientTransaction> buildTransaction(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        GattConnection conn = gattClientTransactionConfigInterface.getConnection();
        return new CommandRequest<>(new CloseGattTransaction(conn, GattState.CLOSE_GATT_CLIENT_SUCCESS), CommandRequest.RequestState.SUCCESS);
    }

    @Override
    public String getSuccessMsg(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        return "Successfully Closed Gatt Client Interface for mac : " + gattClientTransactionConfigInterface.getMac();
    }

    @Override
    public String getFailureMsg(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        return "Failed Closing Gatt Client Interface for mac : " + gattClientTransactionConfigInterface.getMac();
    }
}
