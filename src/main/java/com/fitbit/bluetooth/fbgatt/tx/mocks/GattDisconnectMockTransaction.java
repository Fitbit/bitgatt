/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx.mocks;

import android.bluetooth.BluetoothGatt;
import android.os.Handler;

import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.tx.GattDisconnectTransaction;
import com.fitbit.bluetooth.fbgatt.util.GattDisconnectReason;

import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * A mock of the disconnect task
 *
 * Created by iowens on 11/10/17.
 */

public class GattDisconnectMockTransaction extends GattDisconnectTransaction {

    private static final long REASONABLE_AMOUNT_OF_TIME_FOR_DISCONNECT = TimeUnit.SECONDS.toMillis(1);
    private final Handler mainHandler;
    private final boolean shouldFail;

    public GattDisconnectMockTransaction(GattConnection connection, GattState successEndState, boolean shouldFail) {
        super(connection, successEndState);
        this.shouldFail = shouldFail;
        this.mainHandler = getConnection().getMainHandler();
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        this.callback = callback;
        mainHandler.postDelayed(() -> {
            TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
            builder.responseStatus(GattDisconnectReason.getReasonForCode(shouldFail ? BluetoothGatt.GATT_FAILURE : BluetoothGatt.GATT_SUCCESS).ordinal());
            if(shouldFail) {
                getConnection().setState(GattState.CONNECTED);
                builder.responseStatus(GattDisconnectReason.GATT_CONN_TIMEOUT.getCode())
                        .gattState(getConnection().getGattState())
                        .resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
                callCallbackWithTransactionResultAndRelease(callback, builder.build());
            } else {
                getConnection().setState(GattState.DISCONNECTED);
                builder.gattState(getConnection().getGattState())
                        .responseStatus(0)
                        .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
                callCallbackWithTransactionResultAndRelease(callback, builder.build());
            }
        }, REASONABLE_AMOUNT_OF_TIME_FOR_DISCONNECT);
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        Timber.v("Do nothing, this is a mock");

    }
}
