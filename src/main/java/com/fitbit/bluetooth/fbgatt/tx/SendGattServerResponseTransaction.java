/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx;

import com.fitbit.bluetooth.fbgatt.FitbitBluetoothDevice;
import com.fitbit.bluetooth.fbgatt.GattServerConnection;
import com.fitbit.bluetooth.fbgatt.GattServerTransaction;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.Situation;
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.strategies.Strategy;
import com.fitbit.bluetooth.fbgatt.util.GattStatus;

import timber.log.Timber;

/**
 * Send server response to read or write request ... should execute read or write against
 * characteristic or descriptor
 *
 * Created by iowens on 12/18/17.
 */

public class SendGattServerResponseTransaction extends GattServerTransaction {
    static final String NAME = "SendGattServerResponseTransaction";
    private final FitbitBluetoothDevice device;
    protected final int requestId;
    protected final int offset;
    protected final byte[] value;
    final int status;

    public SendGattServerResponseTransaction(GattServerConnection server, GattState successEndState,
                                             FitbitBluetoothDevice device, int requestId,
                                             int status, int offset, byte[] value) {
        super(server, successEndState);
        this.device = device;
        this.requestId = requestId;
        this.status = status;
        this.offset = offset;
        this.value = value;
    }

    public SendGattServerResponseTransaction(GattServerConnection server, GattState successEndState,
                                             FitbitBluetoothDevice device, int requestId,
                                             int status, int offset, byte[] value, long timeoutMillis) {
        super(server, successEndState, timeoutMillis);
        this.device = device;
        this.requestId = requestId;
        this.status = status;
        this.offset = offset;
        this.value = value;
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        super.transaction(callback);
        getGattServer().setState(GattState.SENDING_SERVER_RESPONSE);
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
        boolean success;
        try {
            success = getGattServer().getServer().sendResponse(device.getBtDevice(), requestId, status, offset, value);
        } catch (NullPointerException ex) {
            Timber.w(ex,"[%s] We are going to fail this tx due to the stack NPE, this is probably poor peripheral behavior, this should become a FW bug.", getDevice());
            success = false;
        }
        if (success) {
            getGattServer().setState(GattState.SEND_SERVER_RESPONSE_SUCCESS);
            builder.gattState(getGattServer().getGattState());
            builder.resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
            builder.responseStatus(GattStatus.getStatusForCode(status).ordinal());
            builder.data(value).
                    requestId(requestId).
                    offset(offset);
            mainThreadHandler.post(() -> {
                callCallbackWithTransactionResultAndRelease(callback, builder.build());
                getGattServer().setState(GattState.IDLE);
            });
        } else {
            getGattServer().setState(GattState.SEND_SERVER_RESPONSE_FAILURE);
            builder.gattState(getGattServer().getGattState());
            builder.resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
            builder.responseStatus(GattStatus.getStatusForCode(status).ordinal());
            builder.data(value).
                    requestId(requestId).
                    offset(offset);
            mainThreadHandler.post(() -> {
                callCallbackWithTransactionResultAndRelease(callback, builder.build());
                getGattServer().setState(GattState.IDLE);
                // we want to apply this strategy to every phone, so we will provide an empty target android
                // device
                Strategy strategy = strategyProvider.
                        getStrategyForPhoneAndGattConnection(null, null,
                                Situation.TRACKER_WENT_AWAY_DURING_GATT_OPERATION);
                if(strategy != null) {
                    strategy.applyStrategy();
                }
            });
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}
