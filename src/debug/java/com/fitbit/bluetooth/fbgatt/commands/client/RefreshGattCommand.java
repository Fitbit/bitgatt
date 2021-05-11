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
import com.fitbit.bluetooth.fbgatt.FitbitGatt;
import com.fitbit.bluetooth.fbgatt.GattClientTransaction;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.commands.CommandRequest;
import com.fitbit.bluetooth.fbgatt.logger.PluginLoggerInterface;
import com.fitbit.bluetooth.fbgatt.tx.GattClientRefreshGattTransaction;

/**
 * Stetho command for refreshing the Gatt.
 */
public class RefreshGattCommand extends AbstractGattClientPropertiesListenerCommand {
    private static final int MAC_ARG_INDEX = 0;

    private final FitbitGatt fitbitGatt;

    public RefreshGattCommand(FitbitGatt fitbitGatt, PluginLoggerInterface logger, FitbitBluetoothDevice.DevicePropertiesChangedCallback devicePropertiesChangedCallback) {
        super("rgt", "refresh-gatt", "<mac>\n\nDescription: Refresh the gatt on the phone", logger, devicePropertiesChangedCallback);
        this.fitbitGatt = fitbitGatt;
    }

    @Override
    public CommandRequest<GattClientTransaction> buildTransaction(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        GattConnection connection = gattClientTransactionConfigInterface.getConnection();
        return new CommandRequest<>(new GattClientRefreshGattTransaction(connection, GattState.REFRESH_GATT_SUCCESS), CommandRequest.RequestState.SUCCESS);
    }

    @Override
    public String getSuccessMsg(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        return "Successfully refreshed on " + gattClientTransactionConfigInterface.getMac();
    }

    @Override
    public String getFailureMsg(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        return "Failed refreshing on " + gattClientTransactionConfigInterface.getMac();
    }

    @Override
    public int getMacArgIndex() {
        return MAC_ARG_INDEX;
    }
}
