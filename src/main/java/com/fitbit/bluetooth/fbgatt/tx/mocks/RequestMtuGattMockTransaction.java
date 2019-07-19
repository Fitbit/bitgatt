/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx.mocks;

import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.tx.RequestMtuGattTransaction;

import android.bluetooth.BluetoothGatt;
import android.os.Handler;
import androidx.annotation.Nullable;

/**
 * Mock class for requesting an MTU change
 *
 * Created by iowens on 12/14/17.
 */

public class RequestMtuGattMockTransaction extends RequestMtuGattTransaction {
    private static final long REASONABLE_TIME_FOR_MTU_REQUEST = 25;
    private final int fakeMtu;
    private final boolean shouldFail;
    private final Handler mainHandler;
    public RequestMtuGattMockTransaction(@Nullable GattConnection connection, GattState successEndState, int mtu, boolean shouldFail) {
        super(connection, successEndState, mtu);
        this.fakeMtu = mtu;
        this.shouldFail = shouldFail;
        this.mainHandler = getConnection().getMainHandler();
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        this.callback = callback;
        getConnection().setState(GattState.REQUESTING_MTU);
        mainHandler.postDelayed(() -> {
            if (shouldFail) {
                onMtuChanged(null, 23, BluetoothGatt.GATT_FAILURE);
            } else {
                onMtuChanged(null, fakeMtu, BluetoothGatt.GATT_SUCCESS);
            }
        }, REASONABLE_TIME_FOR_MTU_REQUEST);
    }
}
