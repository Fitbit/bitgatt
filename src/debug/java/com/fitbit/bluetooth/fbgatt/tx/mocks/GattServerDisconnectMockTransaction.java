/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx.mocks;

import com.fitbit.bluetooth.fbgatt.FitbitBluetoothDevice;
import com.fitbit.bluetooth.fbgatt.GattServerConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.tx.GattServerDisconnectTransaction;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothProfile;
import android.os.Handler;

/**
 * A mock class for the gatt server disconnect transaction
 *
 * Created by iowens on 12/16/17.
 */

public class GattServerDisconnectMockTransaction extends GattServerDisconnectTransaction {
    private static final int REASONABLE_AMOUNT_OF_TIME_FOR_DISCONNECT = 1500;
    private boolean shouldFail = false;
    private final Handler mainHandler;

    public GattServerDisconnectMockTransaction(GattServerConnection server, GattState successEndState, FitbitBluetoothDevice device, boolean shouldFail) {
        super(server, successEndState, device);
        this.shouldFail = shouldFail;
        this.mainHandler = getGattServer().getMainHandler();
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        this.callback = callback;
        getGattServer().setState(GattState.DISCONNECTING);
        mainHandler.postDelayed(() -> {
            if (shouldFail) {
                onServerConnectionStateChange(null, BluetoothGatt.GATT_FAILURE, BluetoothProfile.STATE_CONNECTED);
            } else {
                onServerConnectionStateChange(null, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_DISCONNECTED);
            }
        }, REASONABLE_AMOUNT_OF_TIME_FOR_DISCONNECT);
    }
}
