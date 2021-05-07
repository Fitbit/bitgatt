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
import com.fitbit.bluetooth.fbgatt.tx.GattClientDiscoverServicesTransaction;

/**
 * Stetho command for discovering services on the peripheral.
 */
public class GattClientDiscoverServicesCommand extends AbstractGattClientPropertiesListenerCommand {
    private static final int MAC_ARG_INDEX = 0;

    public GattClientDiscoverServicesCommand(PluginLoggerInterface logger, FitbitBluetoothDevice.DevicePropertiesChangedCallback callback) {
        super("gcds", "gatt-client-discover-services", "<mac>\n\nDescription: Will discover services on connected peripheral with the given mac address", logger, callback);
    }

    @Override
    public CommandRequest<GattClientTransaction> buildTransaction(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        GattConnection connection = gattClientTransactionConfigInterface.getConnection();
        return new CommandRequest<>(new GattClientDiscoverServicesTransaction(connection, GattState.DISCOVERY_SUCCESS), CommandRequest.RequestState.SUCCESS);
    }

    @Override
    public String getSuccessMsg(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        return "Successfully Discovered Gatt Client Services";
    }

    @Override
    public String getFailureMsg(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        return "Failed Discovering Gatt Client Services";
    }

    @Override
    public int getMacArgIndex() {
        return MAC_ARG_INDEX;
    }
}
