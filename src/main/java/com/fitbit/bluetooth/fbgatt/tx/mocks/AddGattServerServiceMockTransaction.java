/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx.mocks;

import android.bluetooth.BluetoothGattService;
import android.os.Handler;

import com.fitbit.bluetooth.fbgatt.GattServerConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.tx.AddGattServerServiceTransaction;

import java.util.concurrent.TimeUnit;

/**
 * Mocks the addition of a gatt server service to the gatt server
 *
 * Created by iowens on 11/16/17.
 */

public class AddGattServerServiceMockTransaction extends AddGattServerServiceTransaction {
    private static final String NAME = "AddGattServerServiceMockTransaction";
    private static final long REASONABLE_AMOUNT_OF_TIME_FOR_ADDING_SERVICE = TimeUnit.SECONDS.toMillis(2);
    private final Handler mainHandler;
    private boolean shouldFail;

    public AddGattServerServiceMockTransaction(GattServerConnection server, GattState successEndState, BluetoothGattService service, boolean shouldFail) {
        super(server, successEndState, service);
        this.shouldFail = shouldFail;
        this.mainHandler = getGattServer().getMainHandler();
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        this.callback = callback;
        getGattServer().setState(GattState.ADDING_SERVICE);
        mainHandler.postDelayed(() -> {
            if(shouldFail) {
                getGattServer().setState(GattState.ADD_SERVICE_FAILURE);
                TransactionResult.Builder transactionResultBuilder = new TransactionResult.Builder().transactionName(getName());
                transactionResultBuilder
                        .gattState(getGattServer().getGattState())
                        .resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
                callCallbackWithTransactionResultAndRelease(callback, transactionResultBuilder.build());
            } else {
                getGattServer().setState(GattState.ADD_SERVICE_SUCCESS);
                TransactionResult.Builder transactionResultBuilder = new TransactionResult.Builder();
                transactionResultBuilder
                        .gattState(getGattServer().getGattState())
                        .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
                callCallbackWithTransactionResultAndRelease(callback, transactionResultBuilder.build());
                getGattServer().setState(GattState.IDLE);
            }
        }, REASONABLE_AMOUNT_OF_TIME_FOR_ADDING_SERVICE);
    }

    @Override
    public String getName() {
        return NAME;
    }
}
