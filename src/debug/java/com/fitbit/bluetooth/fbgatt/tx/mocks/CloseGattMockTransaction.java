/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx.mocks;

import android.os.Handler;
import androidx.annotation.Nullable;

import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.tx.CloseGattTransaction;

/**
 * Mock class for closing the gatt client
 *
 * Created by iowens on 1/22/18.
 */

public class CloseGattMockTransaction extends CloseGattTransaction {
    private static final int REASONABLE_AMOUNT_OF_TIME_FOR_CLOSE = 30;
    private final Handler mainHandler;
    public CloseGattMockTransaction(@Nullable GattConnection connection, GattState successEndState) {
        super(connection, successEndState);
        mainHandler = getConnection().getMainHandler();
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        this.callback = callback;
        getConnection().setState(GattState.CLOSING_GATT_CLIENT);
        mainHandler.postDelayed(() -> {
            getConnection().setState(GattState.CLOSE_GATT_CLIENT_SUCCESS);
            TransactionResult.Builder builder = new TransactionResult.Builder();
            builder.transactionName(getName());
            builder.resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
            builder.gattState(getConnection().getGattState());
            callCallbackWithTransactionResultAndRelease(callback, builder.build());
            getConnection().setState(GattState.DISCONNECTED);
        }, REASONABLE_AMOUNT_OF_TIME_FOR_CLOSE);
    }
}
