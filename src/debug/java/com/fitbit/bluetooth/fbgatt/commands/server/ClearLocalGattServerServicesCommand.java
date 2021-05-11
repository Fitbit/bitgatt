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

import com.fitbit.bluetooth.fbgatt.GattServerTransaction;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.commands.CommandRequest;
import com.fitbit.bluetooth.fbgatt.logger.PluginLoggerInterface;
import com.fitbit.bluetooth.fbgatt.tx.ClearServerServicesTransaction;
import androidx.annotation.NonNull;

/**
 * Stetho command for removing all hosted services from the local Gatt server.
 */
public class ClearLocalGattServerServicesCommand extends AbstractGattServerCommand {
    public ClearLocalGattServerServicesCommand(PluginLoggerInterface logger) {
        super("clgss", "clear-local-gatt-server-services", "Description: Will remove all hosted service from the local gatt server on the mobile device", logger);
    }

    @NonNull
    @Override
    public CommandRequest<GattServerTransaction> buildTransaction(GattServerTransactionConfigInterface config) {
        return new CommandRequest<>(new ClearServerServicesTransaction(config.getServerConnection(), GattState.CLEAR_GATT_SERVER_SERVICES_SUCCESS), CommandRequest.RequestState.SUCCESS);
    }

    @NonNull
    @Override
    public String getSuccessMsg(GattServerTransactionConfigInterface config) {
        return "Successfully Cleared Gatt Server Service";
    }

    @NonNull
    @Override
    public String getFailureMsg(GattServerTransactionConfigInterface config) {
        return "Failed Clearing Gatt Server Service";
    }
}
