/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx;

import com.fitbit.bluetooth.fbgatt.FitbitBluetoothDevice;
import com.fitbit.bluetooth.fbgatt.GattClientTransaction;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransaction;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.util.GattDisconnectReason;
import com.fitbit.bluetooth.fbgatt.util.GattStatus;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothProfile;

import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * Obtain a connected gatt instance.
 * <p>
 * Created by iowens on 11/6/17.
 */

public class GattConnectTransaction extends GattClientTransaction {

    public static final String NAME = "GattConnectTransaction";
    private static final long CONNECTION_TIMEOUT = TimeUnit.SECONDS.toMillis(30);

    public GattConnectTransaction(GattConnection connection, GattState successEndState) {
        super(connection, successEndState);
        setTimeout(CONNECTION_TIMEOUT);
    }

    public GattConnectTransaction(GattConnection connection, GattState successEndState, long timeoutMillis) {
        super(connection, successEndState, timeoutMillis);
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        super.transaction(callback);
        boolean isAbleToStartConnect = getConnection().connect();
        if(!isAbleToStartConnect) {
            failWithNoResources();
        }
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

    @Override
    public FitbitBluetoothDevice getDevice() {
        return getConnection().getDevice();
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
        builder.responseStatus(GattDisconnectReason.getReasonForCode(newState).ordinal());
        if(newState == BluetoothProfile.STATE_DISCONNECTED) {
            getConnection().setState(GattState.DISCONNECTED);
                    builder.rssi(getConnection().getDevice().getRssi())
                    .gattState(getConnection().getGattState())
                    .resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
            callCallbackWithTransactionResultAndRelease(callback, builder.build());
        } else if (newState == BluetoothProfile.STATE_CONNECTED) {
            // we'll put the connected gatt into the connection here
            getConnection().setState(GattState.CONNECTED);
            builder.gattState(getConnection().getGattState())
                    .rssi(getConnection().getDevice().getRssi())
                    .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
            callCallbackWithTransactionResultAndRelease(callback, builder.build());
        } else {
            Timber.v("[%s] Irrelevant state: %s, gatt state: %s", getDevice(), newState, getConnection().getGattState());
        }
        if(GattState.CONNECTED.equals(getConnection().getGattState())) {
            getConnection().setState(GattState.IDLE);
        }
    }
}
