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
import com.fitbit.bluetooth.fbgatt.util.GattStatus;

import android.bluetooth.BluetoothGatt;
import androidx.annotation.Nullable;

import timber.log.Timber;

/**
 * Will discover services on a gatt client
 * <p>
 * Created by iowens on 12/19/17.
 */

public class GattClientDiscoverServicesTransaction extends GattClientTransaction {
    public static final String NAME = "GattClientDiscoverServices";

    public GattClientDiscoverServicesTransaction(@Nullable GattConnection connection, GattState successEndState) {
        super(connection, successEndState);
    }

    public GattClientDiscoverServicesTransaction(@Nullable GattConnection connection, GattState successEndState, long timeoutMillis) {
        super(connection, successEndState, timeoutMillis);
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        super.transaction(callback);
        getConnection().setState(GattState.DISCOVERING);
        boolean success;
        BluetoothGatt localGatt = getConnection().getGatt();
        if(localGatt == null) {
            Timber.w("The gatt was null during discovery, are you sure the connection wasn't cancelled?  Please make sure to handle the transaction results.");
            success = false;
        } else {
            success = localGatt.discoverServices();
        }
        if(!success) {
            getConnection().setState(GattState.DISCOVERY_FAILURE);
            TransactionResult.Builder builder = new TransactionResult.Builder();
            builder.gattState(getConnection().getGattState())
                    .resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
            mainThreadHandler.post(() -> {
                callCallbackWithTransactionResultAndRelease(callback, builder.build());
                getConnection().setState(GattState.IDLE);
            });
        } else {
            synchronized (NAME) {
                try {
                    NAME.wait(DEFAULT_GATT_TRANSACTION_TIMEOUT);
                } catch (InterruptedException e) {
                    Timber.e(e, "[%s] Not the end of the world, we'll just let it go", getDevice());
                }
            }
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
        builder.serverServices(gatt.getServices());
        builder.responseStatus(GattStatus.getStatusForCode(status).ordinal());
        if(status == BluetoothGatt.GATT_SUCCESS) {
            getConnection().setState(GattState.DISCOVERY_SUCCESS);
            builder.resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
        } else {
            getConnection().setState(GattState.DISCOVERY_FAILURE);
            builder.resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
        }
        callCallbackWithTransactionResultAndRelease(callback, builder.build());
        getConnection().setState(GattState.IDLE);
        synchronized (NAME) {
            NAME.notify();
        }
    }

    @Override
    protected void onGattClientTransactionTimeout(GattConnection connection) {
        synchronized (NAME) {
            NAME.notify();
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}
