/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx;

import com.fitbit.bluetooth.fbgatt.FitbitBluetoothDevice;
import com.fitbit.bluetooth.fbgatt.FitbitGatt;
import com.fitbit.bluetooth.fbgatt.GattClientTransaction;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransaction;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.util.GattDisconnectReason;
import com.fitbit.bluetooth.fbgatt.util.GattUtils;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothProfile;

import timber.log.Timber;

/**
 * Will disconnect from the gatt connection
 * <p>
 * Created by iowens on 11/10/17.
 */

public class GattDisconnectTransaction extends GattClientTransaction {

    public static final String NAME = "GattDisconnectionTransaction";

    public GattDisconnectTransaction(GattConnection connection, GattState successEndState) {
        super(connection, successEndState);
    }

    public GattDisconnectTransaction(GattConnection connection, GattState successEndState, long timeoutMillis) {
        super(connection, successEndState, timeoutMillis);
    }

    @Override
    public FitbitBluetoothDevice getDevice() {
        return getConnection().getDevice();
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        super.transaction(callback);
        // before we even do this, we need to check to see if the peripheral is already disconnected,
        // if it is, then let's just move it to state disconnected.
        if(getDevice() != null && !new GattUtils().
                isPerhipheralCurrentlyConnectedToPhone(FitbitGatt.getInstance().getAppContext(),
                        getDevice().getBtDevice())) {
            BluetoothGatt localGatt = getConnection().getGatt();
            if(localGatt != null) {
                onConnectionStateChange(localGatt,
                    BluetoothGatt.GATT_SUCCESS,
                    BluetoothProfile.STATE_DISCONNECTED);
            } else {
                Timber.w("The gatt was already null");
                TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
                getConnection().setState(GattState.DISCONNECTED);
                builder.resultStatus(TransactionResult.TransactionResultStatus.SUCCESS)
                    .rssi(getConnection().getDevice().getRssi())
                    .gattState(getConnection().getGattState());
                callCallbackWithTransactionResultAndRelease(callback, builder.build());
            }
        } else {
            getConnection().disconnect();
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
        builder.responseStatus(GattDisconnectReason.getReasonForCode(status).ordinal());
        if(status != BluetoothGatt.GATT_SUCCESS) {
            Timber.e("[%s] The gatt connection changed in error", getDevice());
        }
        if(newState == BluetoothProfile.STATE_DISCONNECTED) {
            getConnection().setState(GattState.DISCONNECTED);
            builder.resultStatus(TransactionResult.TransactionResultStatus.SUCCESS)
                    .rssi(getConnection().getDevice().getRssi())
                    .gattState(getConnection().getGattState());
            callCallbackWithTransactionResultAndRelease(callback, builder.build());
        } else if(newState == BluetoothProfile.STATE_CONNECTED) {
            getConnection().setState(GattState.CONNECTED);
            builder.resultStatus(TransactionResult.TransactionResultStatus.FAILURE)
                    .rssi(getConnection().getDevice().getRssi())
                    .gattState(getConnection().getGattState());
            callCallbackWithTransactionResultAndRelease(callback, builder.build());
        } else {
            Timber.d("[%s] The disconnection failed with error %s because something went wrong, or the OS doesn't know about that connection",
                    GattDisconnectReason.getReasonForCode(newState),
                    getDevice());
            getConnection().setState(GattState.DISCONNECTED);
            builder.resultStatus(TransactionResult.TransactionResultStatus.SUCCESS)
                    .rssi(getConnection().getDevice().getRssi())
                    .gattState(getConnection().getGattState());
            callCallbackWithTransactionResultAndRelease(callback, builder.build());
        }
    }
}
