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
import com.fitbit.bluetooth.fbgatt.tx.GattServerConnectTransaction;
import com.fitbit.bluetooth.fbgatt.util.GattDisconnectReason;

import android.bluetooth.BluetoothGatt;
import android.os.Handler;

/**
 * A mock of the gatt server connect transaction
 *
 * Created by iowens on 12/16/17.
 */

public class GattServerConnectMockTransaction extends GattServerConnectTransaction {
    private static final int REASONABLE_AMOUNT_OF_TIME_FOR_CONNECT = 1500;
    private boolean shouldFail;
    private final Handler mainHandler;
    public GattServerConnectMockTransaction(GattServerConnection server, GattState successEndState, FitbitBluetoothDevice device, boolean shouldFail) {
        super(server, successEndState, device);
        this.shouldFail = shouldFail;
        this.mainHandler = getGattServer().getMainHandler();
    }

    @Override
    public void transaction(GattTransactionCallback callback) {
        this.callback = callback;
        getGattServer().setState(GattState.CONNECTING);
        mainHandler.postDelayed(() -> {
            TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
            builder.responseStatus(GattDisconnectReason.getReasonForCode(shouldFail ? BluetoothGatt.GATT_FAILURE : BluetoothGatt.GATT_SUCCESS).ordinal());
            if(shouldFail) {
                getGattServer().setState(GattState.FAILURE_CONNECTING);
                builder.resultStatus(TransactionResult.TransactionResultStatus.FAILURE)
                        .gattState(getGattServer().getGattState());
                callCallbackWithTransactionResultAndRelease(callback, builder.build());
            } else {
                getGattServer().setState(GattState.CONNECTED);
                builder.resultStatus(TransactionResult.TransactionResultStatus.SUCCESS)
                        .gattState(getGattServer().getGattState());
                callCallbackWithTransactionResultAndRelease(callback, builder.build());
                getGattServer().setState(GattState.IDLE);
            }
        }, REASONABLE_AMOUNT_OF_TIME_FOR_CONNECT);
    }
}
