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
import com.fitbit.bluetooth.fbgatt.tx.RequestGattConnectionIntervalTransaction;
import androidx.annotation.Nullable;

/**
 * Stetho command for requesting a new connection interval mapping.
 */
public class RequestGattClientConnectionIntervalCommand extends AbstractGattClientPropertiesListenerCommand {
    private final static int MAC_ARG_INDEX = 0;
    private final static int CONNECTION_INTERVAL_ARG_INDEX = 1;
    private final static String CONNECTION_INTERVAL_LOW = "low";
    private final static String CONNECTION_INTERVAL_MEDIUM = "medium";
    private final static String CONNECTION_INTERVAL_HIGH = "high";

    public RequestGattClientConnectionIntervalCommand(PluginLoggerInterface logger, FitbitBluetoothDevice.DevicePropertiesChangedCallback devicePropertiesListener) {
        super("rgcci", "request-gatt-client-connection-interval", "<mac> <low|medium|high>\n\nDescription: Will request a new connection interval mapping to one of the values hard-coded into Android from the mobile device", logger, devicePropertiesListener);
    }

    @Override
    public CommandRequest<GattClientTransaction> buildTransaction(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        GattConnection connection = gattClientTransactionConfigInterface.getConnection();
        String connectionInterval = gattClientTransactionConfigInterface.getConnectionInterval();

        if (isInvalidConnectionInterval(connectionInterval)) {
            return new CommandRequest<>(new IllegalArgumentException("No valid connection interval provided, must be low|medium|high"));
        }

        RequestGattConnectionIntervalTransaction.Speed realCI = RequestGattConnectionIntervalTransaction.Speed.MID;
        switch (connectionInterval) {
            case CONNECTION_INTERVAL_LOW:
                realCI = RequestGattConnectionIntervalTransaction.Speed.LOW;
                break;

            case CONNECTION_INTERVAL_MEDIUM:
                realCI = RequestGattConnectionIntervalTransaction.Speed.MID;
                break;

            case CONNECTION_INTERVAL_HIGH:
                realCI = RequestGattConnectionIntervalTransaction.Speed.HIGH;
                break;
        }

        return new CommandRequest<>(new RequestGattConnectionIntervalTransaction(connection, GattState.REQUEST_CONNECTION_INTERVAL_SUCCESS, realCI), CommandRequest.RequestState.SUCCESS);
    }

    private boolean isInvalidConnectionInterval(@Nullable String connectionInterval) {
        return (connectionInterval == null) || !(connectionInterval.equals(CONNECTION_INTERVAL_LOW) || connectionInterval.equals(CONNECTION_INTERVAL_MEDIUM) || connectionInterval.equals(CONNECTION_INTERVAL_HIGH));
    }

    @Override
    public String getSuccessMsg(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        return "Successfully changed connection speed on " + gattClientTransactionConfigInterface.getMac();
    }

    @Override
    public String getFailureMsg(GattClientTransactionConfigInterface gattClientTransactionConfigInterface) {
        return "Failed changing connection speed on " + gattClientTransactionConfigInterface.getMac();
    }

    @Override
    public int getMacArgIndex() {
        return MAC_ARG_INDEX;
    }

    @Override
    public int getConnectionIntervalArgIndex() {
        return CONNECTION_INTERVAL_ARG_INDEX;
    }
}
