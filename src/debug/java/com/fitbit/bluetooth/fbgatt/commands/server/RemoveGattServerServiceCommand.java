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

import com.fitbit.bluetooth.fbgatt.GattServerConnection;
import com.fitbit.bluetooth.fbgatt.GattServerTransaction;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.commands.CommandRequest;
import com.fitbit.bluetooth.fbgatt.logger.PluginLoggerInterface;
import com.fitbit.bluetooth.fbgatt.tx.RemoveGattServerServicesTransaction;

import android.bluetooth.BluetoothGattService;

import java.util.UUID;

/**
 * Stetho command for removing a service from the local Gatt server.
 */
public class RemoveGattServerServiceCommand extends AbstractGattServerCommand {
    private static final int SERVICE_UUID_ARG_INDEX = 0;

    public RemoveGattServerServiceCommand(PluginLoggerInterface logger) {
        super("rgss", "remove-gatt-server-service", "<service uuid>\n\nDescription: Will remove a service from the local gatt server on the mobile device", logger);
    }

    @Override
    public CommandRequest<GattServerTransaction> buildTransaction(GattServerTransactionConfigInterface gattServerTransactionConfigInterface) {
        String serviceUuid = gattServerTransactionConfigInterface.getServiceUuid();
        if (serviceUuid == null) {
            return new CommandRequest<>(new IllegalArgumentException("No local server service uuid provided"));
        }

        BluetoothGattService gattService = new BluetoothGattService(UUID.fromString(serviceUuid), BluetoothGattService.SERVICE_TYPE_PRIMARY);
        GattServerConnection connection = gattServerTransactionConfigInterface.getServerConnection();
        return new CommandRequest<>(new RemoveGattServerServicesTransaction(connection, GattState.ADD_SERVICE_SUCCESS, gattService), CommandRequest.RequestState.SUCCESS);
    }

    @Override
    public String getSuccessMsg(GattServerTransactionConfigInterface gattServerTransactionConfigInterface) {
        return "Successfully Removed Gatt Server Service";
    }

    @Override
    public String getFailureMsg(GattServerTransactionConfigInterface gattServerTransactionConfigInterface) {
        return "Failed Removing Gatt Server Service";
    }

    @Override
    public int getServiceArgIndex() {
        return SERVICE_UUID_ARG_INDEX;
    }
}
