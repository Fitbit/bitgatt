/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx;

import com.fitbit.bluetooth.fbgatt.FitbitGatt;
import com.fitbit.bluetooth.fbgatt.GattClientTransaction;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransaction;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.util.GattDisconnectReason;

import android.bluetooth.BluetoothGatt;
import androidx.annotation.Nullable;

import timber.log.Timber;

import static android.os.Build.VERSION_CODES.LOLLIPOP;

/**
 * Request a new MTU
 * <p>
 * Created by iowens on 12/14/17.
 */

public class RequestMtuGattTransaction extends GattClientTransaction {

    public static final String NAME = "RequestMtuGattTransaction";

    private final int mtu;

    public RequestMtuGattTransaction(@Nullable GattConnection connection, GattState successEndState, int mtu) {
        super(connection, successEndState);
        this.mtu = mtu;
    }

    public RequestMtuGattTransaction(@Nullable GattConnection connection, GattState successEndState, int mtu, long timeoutMillis) {
        super(connection, successEndState, timeoutMillis);
        this.mtu = mtu;
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        super.transaction(callback);
        getConnection().setState(GattState.REQUESTING_MTU);
        boolean success = false;
        if(FitbitGatt.atLeastSDK(LOLLIPOP)) {
            BluetoothGatt localGatt = getConnection().getGatt();
            if(localGatt != null) {
                success = localGatt.requestMtu(this.mtu);
            } else {
                Timber.w("Couldn't request a new MTU because the gatt was null");
            }
            if(success) {
                return;
            }
        } else {
            Timber.v("[%s] This can only be done on Lollipop and higher", getDevice());
        }
        getConnection().setState(GattState.REQUEST_MTU_FAILURE);
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
        builder.gattState(getConnection().getGattState())
                .resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
        mainThreadHandler.post(() -> {
            callCallbackWithTransactionResultAndRelease(callback, builder.build());
            getConnection().setState(GattState.IDLE);
        });
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
        builder.responseStatus(GattDisconnectReason.getReasonForCode(status).ordinal());
        if(status == BluetoothGatt.GATT_SUCCESS) {
            getConnection().setState(GattState.REQUEST_MTU_SUCCESS);
            builder.resultStatus(TransactionResult.TransactionResultStatus.SUCCESS)
                    .gattState(getConnection().getGattState())
                    .mtu(mtu);
            callCallbackWithTransactionResultAndRelease(callback, builder.build());
            getConnection().setState(GattState.IDLE);
        } else {
            getConnection().setState(GattState.REQUEST_MTU_FAILURE);
            builder.resultStatus(TransactionResult.TransactionResultStatus.FAILURE)
                    .gattState(getConnection().getGattState())
                    .mtu(mtu);
            callCallbackWithTransactionResultAndRelease(callback, builder.build());
            // even if we fail to request the MTU then we can still use the connection
            getConnection().setState(GattState.IDLE);
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}
