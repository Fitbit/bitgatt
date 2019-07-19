/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx.mocks;

import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.tx.RequestGattConnectionIntervalTransaction;
import com.fitbit.bluetooth.fbgatt.util.GattStatus;

import android.os.Handler;
import androidx.annotation.Nullable;

import timber.log.Timber;

/**
 * The mock for requesting the interval
 *
 * Created by iowens on 12/16/17.
 */

public class RequestGattConnectionIntervalMockTransaction extends RequestGattConnectionIntervalTransaction {
    private static final long REASONABLE_TIME_FOR_REQUEST_CI_CHANGE = 10;
    private final Speed fakeSpeed;
    private final boolean shouldFail;
    private final Handler mainHandler;

    public RequestGattConnectionIntervalMockTransaction(@Nullable GattConnection connection, GattState successEndState, Speed connectionSpeed, boolean shouldFail) {
        super(connection, successEndState, connectionSpeed);
        this.shouldFail = shouldFail;
        this.fakeSpeed = connectionSpeed;
        this.mainHandler = getConnection().getMainHandler();
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        this.callback = callback;
        getConnection().setState(GattState.REQUESTING_CONNECTION_INTERVAL_CHANGE);
        this.mainHandler.postDelayed(() -> {
            if(shouldFail) {
                Timber.v("Didn't set speed %s", fakeSpeed.name());
                callCallbackWithTransactionResultAndRelease(callback, new TransactionResult.Builder().
                        transactionName(getName()).
                        resultStatus(TransactionResult.TransactionResultStatus.FAILURE).
                        responseStatus(GattStatus.GATT_INSUF_RESOURCE.getCode()).build());
            } else {
                callCallbackWithTransactionResultAndRelease(callback, new TransactionResult.Builder().
                        transactionName(getName()).
                        resultStatus(TransactionResult.TransactionResultStatus.SUCCESS).
                        build());
            }
            getConnection().setState(GattState.IDLE);
        }, REASONABLE_TIME_FOR_REQUEST_CI_CHANGE);
    }
}
