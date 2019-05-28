/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx.mocks;

import com.fitbit.bluetooth.fbgatt.FitbitBluetoothDevice;
import com.fitbit.bluetooth.fbgatt.GattServerConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.tx.SendGattServerResponseTransaction;

import android.os.Handler;

/**
 * The mock class for the gatt transaction response
 *
 * Created by iowens on 12/18/17.
 */

public class SendGattServerResponseMockTransaction extends SendGattServerResponseTransaction {
    private static final long REASONABLE_TIME_FOR_SEND_RESPONSE = 10;
    private final boolean shouldFail;
    private final Handler mainHandler;
    public SendGattServerResponseMockTransaction(GattServerConnection server, GattState successEndState, FitbitBluetoothDevice device, int requestId, int status, int offset, byte[] value, boolean shouldFail) {
        super(server, successEndState, device, requestId, status, offset, value);
        this.shouldFail = shouldFail;
        this.mainHandler = getGattServer().getMainHandler();
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        this.callback = callback;
        mainHandler.postDelayed(() -> {
            getGattServer().setState(GattState.SENDING_SERVER_RESPONSE);
            TransactionResult.Builder builder = new TransactionResult.Builder();
            builder.transactionName(getName());
            if (shouldFail) {
                getGattServer().setState(GattState.SEND_SERVER_RESPONSE_FAILURE);
                builder.resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
                builder.gattState(getGattServer().getGattState()).
                        data(value).
                        requestId(requestId).
                        offset(offset);
                callCallbackWithTransactionResultAndRelease(callback, builder.build());
            } else {
                getGattServer().setState(GattState.SEND_SERVER_RESPONSE_SUCCESS);
                builder.resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
                builder.gattState(getGattServer().getGattState()).
                        data(value).
                        requestId(requestId).
                        offset(offset);
                callCallbackWithTransactionResultAndRelease(callback, builder.build());
                getGattServer().setState(GattState.IDLE);
            }
        }, REASONABLE_TIME_FOR_SEND_RESPONSE);
    }
}
