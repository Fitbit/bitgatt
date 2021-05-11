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
import com.fitbit.bluetooth.fbgatt.FitbitGatt;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattServerTransaction;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.commands.CommandRequest;
import com.fitbit.bluetooth.fbgatt.logger.PluginLoggerInterface;
import com.fitbit.bluetooth.fbgatt.tx.SendGattServerResponseTransaction;

/**
 * Stetho command for sending a Gatt server response to the peripheral.
 */
public class WriteGattServerResponseCommand extends AbstractGattServerCommand {
    private static final int MAC_ARG_INDEX = 0;
    private static final int REQUEST_ID_ARG_INDEX = 1;
    private static final int STATUS_ARG_INDEX = 2;
    private static final int OFFSET_ARG_INDEX = 3;
    private static final int DATA_ARG_INDEX = 4;

    private final FitbitGatt fitbitGatt;

    public WriteGattServerResponseCommand(FitbitGatt fitbitGatt, PluginLoggerInterface logger) {
        super("wgsr", "write-gatt-server-response", "<mac> <request id> <status-int> <offset-int> <data char[]>\n\nDescription: Will send the gatt server response to the peripheral for a read or write request", logger);
        this.fitbitGatt = fitbitGatt;
    }

    @Override
    public CommandRequest<GattServerTransaction> buildTransaction(GattServerTransactionConfigInterface gattServerTransactionConfigInterface) {
        String mac = gattServerTransactionConfigInterface.getMac();
        String requestId = gattServerTransactionConfigInterface.getRequestId();
        String status = gattServerTransactionConfigInterface.getStatus();
        String offset = gattServerTransactionConfigInterface.getOffset();
        byte[] data = gattServerTransactionConfigInterface.getData();

        if (mac == null) {
            return new CommandRequest<>(new IllegalArgumentException("No bluetooth mac provided"));
        }

        if (requestId == null) {
            return new CommandRequest<>(new IllegalArgumentException("No requestId provided"));
        }

        if (status == null) {
            return new CommandRequest<>(new IllegalArgumentException("No status provided"));
        }

        if (offset == null) {
            return new CommandRequest<>(new IllegalArgumentException("No offset provided"));
        }

        if (data == null) {
            return new CommandRequest<>(new IllegalArgumentException("No data provided"));
        }

        GattConnection connection = fitbitGatt.getConnectionForBluetoothAddress(mac);
        if (connection == null) {
            return new CommandRequest<>(new IllegalArgumentException("Bluetooth connection for mac " + mac + " not found."));
        }

        FitbitBluetoothDevice fitbitBluetoothDevice = connection.getDevice();
        return new CommandRequest<>(new SendGattServerResponseTransaction(gattServerTransactionConfigInterface.getServerConnection(), GattState.SEND_SERVER_RESPONSE_SUCCESS, fitbitBluetoothDevice, Integer.parseInt(requestId), Integer.parseInt(status), Integer.parseInt(offset), data), CommandRequest.RequestState.SUCCESS);
    }

    @Override
    public String getSuccessMsg(GattServerTransactionConfigInterface gattServerTransactionConfigInterface) {
        return "Successfully wrote response to " + gattServerTransactionConfigInterface.getMac();
    }

    @Override
    public String getFailureMsg(GattServerTransactionConfigInterface gattServerTransactionConfigInterface) {
        return "Failed writing response to " + gattServerTransactionConfigInterface.getMac();
    }

    @Override
    public int getMacArgIndex() {
        return MAC_ARG_INDEX;
    }

    @Override
    public int getDataArgIndex() {
        return DATA_ARG_INDEX;
    }

    @Override
    public int getRequestIdArgIndex() {
        return REQUEST_ID_ARG_INDEX;
    }

    @Override
    public int getStatusArgIndex() {
        return STATUS_ARG_INDEX;
    }

    @Override
    public int getOffsetArgIndex() {
        return OFFSET_ARG_INDEX;
    }
}
