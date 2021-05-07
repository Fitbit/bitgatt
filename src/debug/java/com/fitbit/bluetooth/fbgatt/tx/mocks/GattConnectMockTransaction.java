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
import com.fitbit.bluetooth.fbgatt.tx.GattConnectTransaction;
import com.fitbit.bluetooth.fbgatt.util.GattDisconnectReason;
import com.fitbit.bluetooth.fbgatt.util.GattStatus;

/**
 * Mock gatt connect transaction
 *
 * Created by iowens on 11/7/17.
 */

public class GattConnectMockTransaction extends GattConnectTransaction {

    private static final String NAME = "GattConnectMockTransaction";
    private static final int REASONABLE_AMOUNT_OF_TIME_FOR_CONNECT = 1500;
    private boolean shouldFail = false;
    private final Handler mainHandler;

    public GattConnectMockTransaction(GattConnection connection, GattState successEndState, boolean shouldFail) {
        super(connection, successEndState);
        this.shouldFail = shouldFail;
        this.mainHandler = getConnection().getMainHandler();
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        this.callback = callback;
        boolean isAbleToStartConnect = getConnection().connect();
        if(!isAbleToStartConnect) {
            failWithNoResources();
        }

        getConnection().setState(GattState.CONNECTING);
        mainHandler.postDelayed(() -> {
            TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
            builder.responseStatus(GattDisconnectReason.getReasonForCode(shouldFail ? BluetoothGatt.GATT_FAILURE : BluetoothGatt.GATT_SUCCESS).ordinal());
            if(shouldFail) {
                getConnection().setState(GattState.DISCONNECTED);
                builder.responseStatus(GattDisconnectReason.GATT_CONN_TIMEOUT.getCode())
                        .gattState(getConnection().getGattState())
                        .resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
                callCallbackWithTransactionResultAndRelease(callback, builder.build());
            } else {
                getConnection().setState(GattState.CONNECTED);
                builder.gattState(getConnection().getGattState())
                        .responseStatus(0)
                        .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
                callCallbackWithTransactionResultAndRelease(callback, builder.build());
                getConnection().setState(GattState.IDLE);
            }
        }, REASONABLE_AMOUNT_OF_TIME_FOR_CONNECT);
    }

    private void failWithNoResources(){
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
        builder.responseStatus(GattStatus.GATT_NO_RESOURCES.getCode());
        getConnection().setState(GattState.DISCONNECTED);
        builder.rssi(getConnection().getDevice().getRssi())
                .gattState(getConnection().getGattState())
                .resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
        callCallbackWithTransactionResultAndRelease(callback, builder.build());
    }

    @Override
    public String getName() {
        return NAME;
    }
}
