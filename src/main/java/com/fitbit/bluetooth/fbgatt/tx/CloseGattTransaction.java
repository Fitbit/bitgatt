/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx;

import com.fitbit.bluetooth.fbgatt.GattClientTransaction;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransaction;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.TransactionResult;

import android.bluetooth.BluetoothGatt;
import androidx.annotation.Nullable;

import timber.log.Timber;

/**
 * Will close the gatt server, on Marshmallow+ this will also release the client if
 * <p>
 * Created by iowens on 1/22/18.
 */

public class CloseGattTransaction extends GattClientTransaction {
    private static final String NAME = "CloseGattTransaction";

    public CloseGattTransaction(@Nullable GattConnection connection, GattState successEndState) {
        super(connection, successEndState);
    }

    public CloseGattTransaction(@Nullable GattConnection connection, GattState successEndState, long timeoutMillis) {
        super(connection, successEndState, timeoutMillis);
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        super.transaction(callback);
        getConnection().setState(GattState.CLOSING_GATT_CLIENT);
        BluetoothGatt localGatt = getConnection().getGatt();
        if(localGatt != null) {
            try {
                localGatt.close();
                getConnection().justClearGatt();
            } catch (NullPointerException e) {
                Timber.w(e, "[%s] If the underlying connection is gone, on some platforms, this can throw an NPE", getConnection().getDevice());
            }
        }
        getConnection().setState(GattState.CLOSE_GATT_CLIENT_SUCCESS);
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
        builder.resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
        builder.gattState(getConnection().getGattState());
        mainThreadHandler.post(() -> {
            callCallbackWithTransactionResultAndRelease(callback, builder.build());
            getConnection().setState(GattState.DISCONNECTED);
        });
    }

    @Override
    public String getName() {
        return NAME;
    }
}
