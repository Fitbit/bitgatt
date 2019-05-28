/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx;

import com.fitbit.bluetooth.fbgatt.FitbitBluetoothDevice;
import com.fitbit.bluetooth.fbgatt.GattServerConnection;
import com.fitbit.bluetooth.fbgatt.GattServerTransaction;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.util.GattStatus;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.TransactionResult;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothProfile;

import timber.log.Timber;

/**
 * Will connect to a provided bluetooth device via the gatt server this may trigger a service
 * discovery.
 *
 * Created by iowens on 12/16/17.
 */

public class GattServerConnectTransaction extends GattServerTransaction {

    private static final String NAME = "GattServerConnectTransaction";

    private final FitbitBluetoothDevice device;

    public GattServerConnectTransaction(GattServerConnection server, GattState successEndState, FitbitBluetoothDevice device) {
        super(server, successEndState);
        this.device = device;
    }

    public GattServerConnectTransaction(GattServerConnection server, GattState successEndState, FitbitBluetoothDevice device, long timeoutMillis) {
        super(server, successEndState, timeoutMillis);
        this.device = device;
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        super.transaction(callback);
        getGattServer().setState(GattState.CONNECTING);
        getGattServer().connect(this.device);
    }

    @Override
    public void onServerConnectionStateChange(BluetoothDevice device, int status, int newState) {
        Timber.d("[%s] Connection result %s", getDevice(), GattStatus.values()[status].name());
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
        if(status == BluetoothGatt.GATT_SUCCESS) {
            if(newState == BluetoothProfile.STATE_CONNECTED) {
                getGattServer().setState(GattState.CONNECTED);
                builder.resultStatus(TransactionResult.TransactionResultStatus.SUCCESS)
                        .gattState(getGattServer().getGattState());
                callCallbackWithTransactionResultAndRelease(callback, builder.build());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                getGattServer().setState(GattState.DISCONNECTED);
                builder.resultStatus(TransactionResult.TransactionResultStatus.FAILURE)
                        .gattState(getGattServer().getGattState());
                callCallbackWithTransactionResultAndRelease(callback, builder.build());
                getGattServer().setState(GattState.IDLE);
            }
        } else {
            getGattServer().setState(GattState.FAILURE_CONNECTING);
            builder.resultStatus(TransactionResult.TransactionResultStatus.FAILURE)
                    .gattState(getGattServer().getGattState());
            callCallbackWithTransactionResultAndRelease(callback, builder.build());
            getGattServer().setState(GattState.DISCONNECTED);
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}
