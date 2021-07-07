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
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.util.GattDisconnectReason;
import com.fitbit.bluetooth.fbgatt.util.GattStatus;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothProfile;
import timber.log.Timber;

/**
 * Gatt server disconnect transaction, will either cancel a connection attempt in progress, or
 * signal disconnect to a connected device.  This will not directly affect any client connections
 * to the same device, however the device may disconnect all connections when disconnected by the
 * gatt server.
 *
 * Created by iowens on 12/16/17.
 */

public class GattServerDisconnectTransaction extends GattServerTransaction {
    private static final String NAME = "GattServerDisconnectTransaction";
    private final FitbitBluetoothDevice device;

    public GattServerDisconnectTransaction(GattServerConnection server, GattState successEndState, FitbitBluetoothDevice device) {
        super(server, successEndState);
        this.device = device;
    }

    public GattServerDisconnectTransaction(GattServerConnection server, GattState successEndState, FitbitBluetoothDevice device, long timeoutMillis) {
        super(server, successEndState, timeoutMillis);
        this.device = device;
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        super.transaction(callback);
        getGattServer().setState(GattState.DISCONNECTING);
        getGattServer().disconnect(device);
    }

    @Override
    public void onServerConnectionStateChange(BluetoothDevice device, int status, int newState) {
        Timber.d("[%s] Gatt State %s, Disconnect Reason : %s", getDevice(), GattStatus.values()[status].name(),
                GattDisconnectReason.getReasonForCode(newState));
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
        builder.responseStatus(GattDisconnectReason.getReasonForCode(status).ordinal());
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                getGattServer().setState(GattState.DISCONNECTED);
                builder.gattState(getGattServer().getGattState())
                        .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
                callCallbackWithTransactionResultAndRelease(callback, builder.build());
            } else if (newState == BluetoothProfile.STATE_CONNECTED) {
                getGattServer().setState(GattState.CONNECTED);
                builder.gattState(getGattServer().getGattState())
                        .resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
                callCallbackWithTransactionResultAndRelease(callback, builder.build());
            }
        } else {
            builder.gattState(getGattServer().getGattState())
                    .resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
            callCallbackWithTransactionResultAndRelease(callback, builder.build());
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}
