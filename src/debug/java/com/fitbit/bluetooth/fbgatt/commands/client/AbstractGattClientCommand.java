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
import com.fitbit.bluetooth.fbgatt.commands.AbstractGattCommand;
import com.fitbit.bluetooth.fbgatt.commands.CommandRequest;
import com.fitbit.bluetooth.fbgatt.commands.PluginCommandConfig;
import com.fitbit.bluetooth.fbgatt.logger.PluginLoggerInterface;
import com.fitbit.bluetooth.fbgatt.utils.GattConnectionUtils;
import java.util.concurrent.CountDownLatch;
import androidx.annotation.NonNull;

/**
 * Base class for common Gatt client commands implementation.
 */
abstract class AbstractGattClientCommand extends AbstractGattCommand implements GattClientCommandInterface {
    private final GattClientTransactionConfigParser configParser = new GattClientTransactionConfigParser();

    protected AbstractGattClientCommand(String shortName, String fullName, String description, PluginLoggerInterface logger) {
        super(shortName, fullName, description, logger);
    }

    @Override
    public final void run(PluginCommandConfig config) {
        GattConnectionUtils connectionUtils = config.getConnectionUtils();

        GattClientTransactionConfigInterface transactionConfig = configParser.getTransactionConfig(config, this);
        GattConnection connection = connectionUtils.getConnectionForMac(transactionConfig.getMac());
        if (connection == null) {
            onError(config, new IllegalStateException("No connected client for mac " + transactionConfig.getMac()));
            return;
        }

        transactionConfig.setConnection(connection);
        CommandRequest<GattClientTransaction> commandRequest = buildTransaction(transactionConfig);
        if (commandRequest.getState() != CommandRequest.RequestState.SUCCESS) {
            onError(config, commandRequest.getException());
            return;
        }

        GattClientTransaction tx = commandRequest.getTransaction();
        String message = commandRequest.getMessage();
        if (tx == null && message != null) {
            onSuccess(config, message);
            return;
        } else if (tx == null) {
            //  Just for edge cases - should never get to state in which CommandRequest.getState() ==
            //  RequestState.SUCCESS && CommandRequest.getTransaction() == null &&
            //  CommandRequest.getMessage() == null.
            onError(config, new IllegalStateException("Could not create GattClientTransaction; aborting command"));
            return;
        }

        preExecute(connection);

        CountDownLatch cdl = new CountDownLatch(1);
        connection.runTx(tx, result -> {
            onResult(config, result);

            cdl.countDown();
            postExecute(connection);
        });

        try {
            cdl.await();
        } catch (InterruptedException e) {
            onError(config, e);
        }
    }

    protected void preExecute(@NonNull GattConnection connection) {
        //  To be implemented if additional operations need to be executed before the
        //  command transaction is run.
    }

    protected void postExecute(@NonNull GattConnection connection) {
        //  To be implemented if additional operations need to be executed after the
        //  command transaction is run.
    }
}
