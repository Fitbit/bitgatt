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
import com.fitbit.bluetooth.fbgatt.tx.GattConnectTransaction;

/**
 * Stetho command for connecting to a peripheral.
 */
public class GattClientConnectCommand extends AbstractGattClientPropertiesListenerCommand {
    private static final int MAC_ARG_INDEX = 0;

    public GattClientConnectCommand(PluginLoggerInterface logger, FitbitBluetoothDevice.DevicePropertiesChangedCallback devicePropertiesChangedCallback) {
        super("gcc", "gatt-client-connect", "<mac>\n\nDescription: Will connect to the peripheral with the provided mac address", logger, devicePropertiesChangedCallback);
    }

    @Override
    public CommandRequest<GattClientTransaction> buildTransaction(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        GattConnection connection = gattClientTransactionConfigInterface.getConnection();
        return new CommandRequest<>(new GattConnectTransaction(connection, GattState.CONNECTED), CommandRequest.RequestState.SUCCESS);
    }

    @Override
    public String getSuccessMsg(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        return "Successfully Connected to " + gattClientTransactionConfigInterface.getMac();
    }

    @Override
    public String getFailureMsg(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        return "Failed Connecting to " + gattClientTransactionConfigInterface.getMac();
    }

    @Override
    public int getMacArgIndex() {
        return MAC_ARG_INDEX;
    }
}
