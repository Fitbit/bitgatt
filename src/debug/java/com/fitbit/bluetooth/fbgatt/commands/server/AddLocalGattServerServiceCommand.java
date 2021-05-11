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
import com.fitbit.bluetooth.fbgatt.tx.AddGattServerServiceTransaction;
import com.fitbit.bluetooth.fbgatt.utils.CommonGattServerUtils;
import android.bluetooth.BluetoothGattService;
import java.util.UUID;
import androidx.annotation.NonNull;

/**
 * Stetho command for adding a local Gatt server service.
 */
public class AddLocalGattServerServiceCommand extends AbstractGattServerCommand {
    public static final int SERVICE_UUID_ARG_INDEX = 0;
    private final CommonGattServerUtils gattServerUtils;

    public AddLocalGattServerServiceCommand(PluginLoggerInterface logger, CommonGattServerUtils gattServerUtils) {
        super("algss", "add-local-gatt-server-service", "<uuid>\n\nDescription: Will add a local gatt server service to the mobile device", logger);
        this.gattServerUtils = gattServerUtils;
    }

    @NonNull
    @Override
    public CommandRequest<GattServerTransaction> buildTransaction(GattServerTransactionConfigInterface config) {
        String serviceUuid = config.getServiceUuid();
        if (serviceUuid == null) {
            return new CommandRequest<>(new IllegalArgumentException("No viable service UUID provided"));
        }

        GattServerConnection serverConnection = config.getServerConnection();

        boolean isDuplicate = gattServerUtils.isServiceDuplicate(serviceUuid);
        if (!isDuplicate) {
            BluetoothGattService btGattService = new BluetoothGattService(UUID.fromString(serviceUuid), BluetoothGattService.SERVICE_TYPE_PRIMARY);
            return new CommandRequest<>(new AddGattServerServiceTransaction(serverConnection, GattState.ADD_SERVICE_SUCCESS, btGattService), CommandRequest.RequestState.SUCCESS);
        } else {
            return new CommandRequest<>("Duplicate service by UUID", CommandRequest.RequestState.SUCCESS);
        }
    }

    @Override
    public int getServiceArgIndex() {
        return SERVICE_UUID_ARG_INDEX;
    }

    @NonNull
    @Override
    public String getSuccessMsg(GattServerTransactionConfigInterface config) {
        return "Successfully Added Gatt Server Service for " + config.getServiceUuid();
    }

    @NonNull
    @Override
    public String getFailureMsg(GattServerTransactionConfigInterface config) {
        return "Failed Adding Gatt Server Service for " + config.getServiceUuid();
    }
}
