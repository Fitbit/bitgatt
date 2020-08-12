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
import com.fitbit.bluetooth.fbgatt.util.GattStatus;

import android.bluetooth.BluetoothGatt;
import androidx.annotation.Nullable;

import timber.log.Timber;

import static android.os.Build.VERSION_CODES.LOLLIPOP;

/**
 * Will request a connection interval change, on Android there are three levels basically low, mid, high
 * each one will negotiate an appropriate connection interval for that phone.
 *
 * The downside is that while you can request a connection interval, there is no response, the only
 * way to know what you got is to look at the logs.  This needs care as well because it's possible
 * to jam the gatt if the CI change comes from both the central and peripheral at the same time.
 *
 * Created by iowens on 12/16/17.
 */

public class RequestGattConnectionIntervalTransaction extends GattClientTransaction {

    private static final String NAME = "RequestGattConnectionIntervalTransaction";
    private final Speed speed;

    public RequestGattConnectionIntervalTransaction(@Nullable GattConnection connection, GattState successEndState, Speed connectionSpeed) {
        super(connection, successEndState);
        this.speed = connectionSpeed;
    }

    public RequestGattConnectionIntervalTransaction(@Nullable GattConnection connection, GattState successEndState, Speed connectionSpeed, long timeoutMillis) {
        super(connection, successEndState, timeoutMillis);
        this.speed = connectionSpeed;
    }

    public enum Speed {
        LOW(BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER),
        MID(BluetoothGatt.CONNECTION_PRIORITY_BALANCED),
        HIGH(BluetoothGatt.CONNECTION_PRIORITY_HIGH);

        int connectionPriority;
        Speed(int connectionPriority) {
            this.connectionPriority = connectionPriority;
        }

        public int getConnectionPriority(){
            return this.connectionPriority;
        }



    }
    @Override
    protected void transaction(GattTransactionCallback callback) {
        super.transaction(callback);
        getConnection().setState(GattState.REQUESTING_CONNECTION_INTERVAL_CHANGE);
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
        builder.resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
        boolean success = false;
        if(FitbitGatt.atLeastSDK(LOLLIPOP)) {
            BluetoothGatt localGatt = getConnection().getGatt();
            if(localGatt != null) {
                success = localGatt.requestConnectionPriority(speed.getConnectionPriority());
            } else {
                Timber.w("Couldn't request connection priority because gatt was null");
            }
            if(!success) {
                getConnection().setState(GattState.REQUEST_CONNECTION_INTERVAL_FAILURE);
                builder.responseStatus(GattStatus.GATT_NO_RESOURCES.getCode());
                builder.gattState(getConnection().getGattState());
                mainThreadHandler.post(() -> {
                    callCallbackWithTransactionResultAndRelease(callback, builder.build());
                    // even if we can't change the interval, we can still use the connection
                    getConnection().setState(GattState.IDLE);
                });
            } else {
                getConnection().setState(GattState.REQUEST_CONNECTION_INTERVAL_SUCCESS);
                builder.gattState(getConnection().getGattState());
                builder.resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
                mainThreadHandler.post(() -> {
                    callCallbackWithTransactionResultAndRelease(callback, builder.build());
                    // we were able to send the request, so we good.
                    getConnection().setState(GattState.IDLE);
                });
            }
        } else {
            getConnection().setState(GattState.REQUEST_CONNECTION_INTERVAL_FAILURE);
            builder.responseStatus(GattStatus.GATT_NO_RESOURCES.getCode());
            builder.gattState(getConnection().getGattState());
            mainThreadHandler.post(() -> {
                callCallbackWithTransactionResultAndRelease(callback, builder.build());
                // even if we can't change the interval, we can still use the connection
                getConnection().setState(GattState.IDLE);
            });
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}
