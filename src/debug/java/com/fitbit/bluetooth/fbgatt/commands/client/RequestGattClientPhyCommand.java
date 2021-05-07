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
import com.fitbit.bluetooth.fbgatt.tx.RequestGattClientPhyChangeTransaction;

/**
 * Stetho command for requesting different phy values.
 */
public class RequestGattClientPhyCommand extends AbstractGattClientCommand {
    private static final int MAC_ARG_INDEX = 0;
    private static final int TX_PHY_ARG_INDEX = 1;
    private static final int RX_PHY_ARG_INDEX = 2;
    private static final int PHY_OPTIONS_ARG_INDEX = 3;

    public RequestGattClientPhyCommand(PluginLoggerInterface logger) {
        super("rqgcp", "request-gatt-client-phy", "<mac> <txPhy> <rxPhy> <phyOptions>\n\nDescription: Will request a different PHY from the mobile, can be 1, 2, or 3, please see BluetoothDevice#PHY*", logger);
    }

    @Override
    public CommandRequest<GattClientTransaction> buildTransaction(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        GattConnection connection = gattClientTransactionConfigInterface.getConnection();
        int txPhy = gattClientTransactionConfigInterface.getTxPhy();
        int rxPhy = gattClientTransactionConfigInterface.getRxPhy();
        int phyOptions = gattClientTransactionConfigInterface.getPhyOptions();

        if (txPhy == Integer.MIN_VALUE) {
            return new CommandRequest<>(new IllegalArgumentException("No tx PHY"));
        }

        if (rxPhy == Integer.MIN_VALUE) {
            return new CommandRequest<>(new IllegalArgumentException("No rx PHY"));
        }

        if (phyOptions == Integer.MIN_VALUE) {
            return new CommandRequest<>(new IllegalArgumentException("No phy options"));
        }

        return new CommandRequest<>(new RequestGattClientPhyChangeTransaction(connection, GattState.REQUEST_PHY_CHANGE_SUCCESS, txPhy, rxPhy, phyOptions), CommandRequest.RequestState.SUCCESS);
    }

    @Override
    public String getSuccessMsg(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        return "Successfully changed physical layer to tx: " + gattClientTransactionConfigInterface.getTxPhy() + ", rx:  " + gattClientTransactionConfigInterface.getRxPhy();
    }

    @Override
    public String getFailureMsg(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        return "Failed in changing physical layer to tx: " + gattClientTransactionConfigInterface.getTxPhy() + ", rx: " + gattClientTransactionConfigInterface.getRxPhy();
    }

    @Override
    public int getMacArgIndex() {
        return MAC_ARG_INDEX;
    }

    @Override
    public int getTxPhyArgIndex() {
        return TX_PHY_ARG_INDEX;
    }

    @Override
    public int getRxPhyArgIndex() {
        return RX_PHY_ARG_INDEX;
    }

    @Override
    public int getPhyOptionsArgIndex() {
        return PHY_OPTIONS_ARG_INDEX;
    }
}
