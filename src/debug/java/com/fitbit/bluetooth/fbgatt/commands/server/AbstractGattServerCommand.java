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
import com.fitbit.bluetooth.fbgatt.GattServerConnection;
import com.fitbit.bluetooth.fbgatt.GattServerTransaction;
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.commands.AbstractGattCommand;
import com.fitbit.bluetooth.fbgatt.commands.CommandRequest;
import com.fitbit.bluetooth.fbgatt.commands.DefaultGattTransactionExecutor;
import com.fitbit.bluetooth.fbgatt.commands.GattCommandExecutorInterface;
import com.fitbit.bluetooth.fbgatt.commands.PluginCommandConfig;
import com.fitbit.bluetooth.fbgatt.logger.PluginLoggerInterface;
import androidx.annotation.Nullable;

/**
 * Base class for common Gatt server commands implementation.
 */
abstract class AbstractGattServerCommand extends AbstractGattCommand implements GattServerCommandInterface {
    @Nullable
    protected final FitbitBluetoothDevice.DevicePropertiesChangedCallback devicePropertiesListener;
    private final GattServerTransactionConfigParser configParser = new GattServerTransactionConfigParser();

    protected AbstractGattServerCommand(String shortName, String fullName, String description, PluginLoggerInterface logger) {
        this(shortName, fullName, description, logger, null);
    }

    protected AbstractGattServerCommand(String shortName, String fullName, String description, PluginLoggerInterface logger, @Nullable FitbitBluetoothDevice.DevicePropertiesChangedCallback devicePropertiesListener) {
        super(shortName, fullName, description, logger);
        this.devicePropertiesListener = devicePropertiesListener;
    }

    @Override
    public final void run(PluginCommandConfig config) {
        GattServerTransactionConfigInterface transactionConfig = configParser.getTransactionConfig(config, this);
        CommandRequest<GattServerTransaction> commandRequest = buildTransaction(transactionConfig);
        if (commandRequest.getState() != CommandRequest.RequestState.SUCCESS) {
            onError(config, commandRequest.getException());
            return;
        }

        GattServerTransaction tx = commandRequest.getTransaction();
        String message = commandRequest.getMessage();
        if (tx == null && message != null) {
            onSuccess(config, message);
            return;
        } else if (tx == null) {
            //  Just for edge cases - should never get to state in which CommandRequest.getState() ==
            //  RequestState.SUCCESS && CommandRequest.getTransaction() == null &&
            //  CommandRequest.getMessage() == null.
            onError(config, new IllegalStateException("Could not create GattServerTransaction; aborting command"));
            return;
        }

        GattServerConnection serverConnection = config.getServerConnection();
        DefaultGattTransactionExecutor executor = new DefaultGattTransactionExecutor(serverConnection, new GattCommandExecutorInterface.GattCommandExecutorCallback() {
            @Override
            public void onResult(TransactionResult result) {
                AbstractGattServerCommand.this.onResult(config, result);
            }

            @Override
            public void onError(Exception e) {
                AbstractGattServerCommand.this.onError(config, e);
            }
        }, devicePropertiesListener);
        executor.runGattServerTransaction(tx, getSuccessMsg(transactionConfig), getFailureMsg(transactionConfig), config.isJsonFormat());
    }
}
