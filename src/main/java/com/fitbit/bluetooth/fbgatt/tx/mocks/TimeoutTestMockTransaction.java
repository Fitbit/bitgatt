/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx.mocks;

import com.fitbit.bluetooth.fbgatt.FitbitGatt;
import com.fitbit.bluetooth.fbgatt.GattClientTransaction;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransaction;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.util.BluetoothUtils;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.Objects;

import timber.log.Timber;

/**
 * A test transaction to ensure that we have the ability to correctly timeout a transaction, this
 * should only be used in instrumented tests, or runtime tests
 */

public class TimeoutTestMockTransaction extends GattClientTransaction {

    private static final String NAME = "TimeoutTestMockTransaction";
    private static final long OVERRIDE_TIMEOUT = 1000L;

    public TimeoutTestMockTransaction(GattConnection connection, GattState destinationState, BluetoothGattCharacteristic characteristic) {
        super(connection, destinationState);
        setTimeout(OVERRIDE_TIMEOUT);
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        super.transaction(callback);
        // let's do something useful with the adapter
        BluetoothUtils bluetoothUtils = new BluetoothUtils();
        boolean is2MsymSupported = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            is2MsymSupported = Objects.requireNonNull(bluetoothUtils.getBluetoothAdapter(FitbitGatt.getInstance().getAppContext())).isLe2MPhySupported();
        }
        Timber.i("2 msym is supported? %b", is2MsymSupported);
        // this transaction is designed to timeout to ensure that it blocks the execution thread
    }

    @Override
    public String getName(){
        return NAME;
    }
}
