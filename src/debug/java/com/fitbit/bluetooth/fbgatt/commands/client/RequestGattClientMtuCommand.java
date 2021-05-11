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
import com.fitbit.bluetooth.fbgatt.tx.RequestMtuGattTransaction;

/**
 * Stetho command for requesting a different MTU size from the peripheral.
 */
public class RequestGattClientMtuCommand extends AbstractGattClientCommand {
    private final static int MAC_ARG_INDEX = 0;
    private final static int MTU_ARG_INDEX = 1;

    public RequestGattClientMtuCommand(PluginLoggerInterface logger) {
        super("rgcm", "request-gatt-client-mtu", "<mac> <mtu> ( must be between 23 and 512 )\n\nDescription: Will request a different MTU size from a peripheral with the given mac address", logger);
    }

    @Override
    public CommandRequest<GattClientTransaction> buildTransaction(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        GattConnection connection = gattClientTransactionConfigInterface.getConnection();
        int mtu = gattClientTransactionConfigInterface.getMtu();

        if (mtu == Integer.MIN_VALUE) {
            return new CommandRequest<>(new IllegalArgumentException("No mtu"));
        }

        return new CommandRequest<>(new RequestMtuGattTransaction(connection, GattState.REQUEST_MTU_SUCCESS, mtu), CommandRequest.RequestState.SUCCESS);
    }

    @Override
    public String getSuccessMsg(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        return "Successfully changed mtu to " + gattClientTransactionConfigInterface.getMtu() + " on " + gattClientTransactionConfigInterface.getMac();
    }

    @Override
    public String getFailureMsg(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        return "Failed to change mtu to " + gattClientTransactionConfigInterface.getMtu() + " on " + gattClientTransactionConfigInterface.getMac();
    }

    @Override
    public int getMacArgIndex() {
        return MAC_ARG_INDEX;
    }

    @Override
    public int getMtuArgIndex() {
        return MTU_ARG_INDEX;
    }
}
