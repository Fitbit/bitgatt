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

import com.fitbit.bluetooth.fbgatt.GattClientTransaction;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.commands.CommandRequest;
import com.fitbit.bluetooth.fbgatt.logger.PluginLoggerInterface;
import com.fitbit.bluetooth.fbgatt.tx.ReadGattClientPhyTransaction;

/**
 * Stetho command for reading Gatt client phy.
 */
public class ReadGattClientPhyCommand extends AbstractGattClientCommand {
    private static final int MAC_ARG_INDEX = 0;

    public ReadGattClientPhyCommand(PluginLoggerInterface logger) {
        super("rgcp", "read-gatt-client-phy", "<mac>\n\nDescription: Will read the gatt client phy", logger);
    }

    @Override
    public CommandRequest<GattClientTransaction> buildTransaction(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        GattConnection connection = gattClientTransactionConfigInterface.getConnection();
        return new CommandRequest<>(new ReadGattClientPhyTransaction(connection, GattState.READ_CURRENT_PHY_SUCCESS), CommandRequest.RequestState.SUCCESS);
    }

    @Override
    public String getSuccessMsg(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        return "Successfully read phy for mac " + gattClientTransactionConfigInterface.getMac();
    }

    @Override
    public String getFailureMsg(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        return "Failed to read phy for mac " + gattClientTransactionConfigInterface.getMac();
    }

    @Override
    public int getMacArgIndex() {
        return MAC_ARG_INDEX;
    }
}
