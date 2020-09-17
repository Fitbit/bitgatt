/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx;

import android.bluetooth.BluetoothGatt;
import androidx.annotation.Nullable;

import com.fitbit.bluetooth.fbgatt.GattClientTransaction;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransaction;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.util.GattDisconnectReason;

/**
 * To read the RSSI from a remote device
 * <p>
 * Created by iowens on 12/14/17.
 */

public class ReadRssiTransaction extends GattClientTransaction {

    public static final String NAME = "ReadRssiTransaction";

    public ReadRssiTransaction(@Nullable GattConnection connection, GattState successEndState) {
        super(connection, successEndState);
    }

    public ReadRssiTransaction(@Nullable GattConnection connection, GattState successEndState, long timeoutMillis) {
        super(connection, successEndState, timeoutMillis);
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        super.transaction(callback);
        getConnection().setState(GattState.READING_RSSI);
        BluetoothGatt localGatt = getConnection().getGatt();
        boolean success = false;
        if(localGatt != null) {
            success = localGatt.readRemoteRssi();
        }
        if(!success) {
            getConnection().setState(GattState.READ_RSSI_FAILURE);
            TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
            builder.gattState(getConnection().getGattState())
                    .resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
            mainThreadHandler.post(() -> {
                callCallbackWithTransactionResultAndRelease(callback, builder.build());
                getConnection().setState(GattState.IDLE);
            });
        }
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
        builder.responseStatus(GattDisconnectReason.getReasonForCode(status).ordinal());
        if(status == BluetoothGatt.GATT_SUCCESS) {
            getConnection().setState(GattState.READ_RSSI_SUCCESS);
            getConnection().getDevice().setRssi(rssi);
            builder.gattState(getConnection().getGattState())
                    .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS)
                    .rssi(rssi);
            callCallbackWithTransactionResultAndRelease(callback, builder.build());
            getConnection().setState(GattState.IDLE);
        } else {
            getConnection().setState(GattState.READ_RSSI_FAILURE);
            builder.gattState(getConnection().getGattState())
                    .resultStatus(TransactionResult.TransactionResultStatus.FAILURE)
                    .rssi(rssi);
            callCallbackWithTransactionResultAndRelease(callback, builder.build());
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}
