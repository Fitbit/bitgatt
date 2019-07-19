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
import com.fitbit.bluetooth.fbgatt.btcopies.BluetoothGattCharacteristicCopy;
import com.fitbit.bluetooth.fbgatt.tx.ReadGattCharacteristicTransaction;
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.util.GattStatus;
import com.fitbit.bluetooth.fbgatt.util.GattUtils;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import timber.log.Timber;

/**
 * A delightful class, filled with stimulating code and whimsical
 * fancy for your reading pleasure
 *
 * Created by iowens on 11/17/17.
 */

public class ReadGattCharacteristicMockTransaction extends ReadGattCharacteristicTransaction {
    private static final long REASONABLE_TIME_FOR_CHARACTERISTIC_READ = 250;
    private final byte[] fakeData;
    private final boolean shouldFail;
    private final Handler mainHandler;
    private final BluetoothGattCharacteristic characteristic;

    public ReadGattCharacteristicMockTransaction(@Nullable GattConnection connection, GattState successEndState, BluetoothGattCharacteristic characteristic, byte[] fakeData, boolean shouldFail) {
        super(connection, successEndState, characteristic);
        this.fakeData = fakeData;
        this.shouldFail = shouldFail;
        this.characteristic = characteristic;
        this.mainHandler = getConnection().getMainHandler();
    }

    @Override
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public void transaction(GattTransactionCallback callback) {
        this.callback = callback;
        getConnection().setState(GattState.READING_CHARACTERISTIC);
        mainHandler.postDelayed(() -> {
            if(shouldFail) {
                onCharacteristicRead(null, new GattUtils().copyCharacteristic(characteristic), GattStatus.GATT_READ_NOT_PERMIT.getCode());
                getConnection().setState(GattState.READ_CHARACTERISTIC_FAILURE);
                TransactionResult.Builder transactionResultBuilder = new TransactionResult.Builder().transactionName(getName());
                transactionResultBuilder
                        .gattState(getConnection().getGattState())
                        .resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
                callCallbackWithTransactionResultAndRelease(callback, transactionResultBuilder.build());
            } else {
                onCharacteristicRead(null, new GattUtils().copyCharacteristic(characteristic), GattStatus.GATT_SUCCESS.getCode());
                getConnection().setState(GattState.READ_CHARACTERISTIC_SUCCESS);
                TransactionResult.Builder transactionResultBuilder = new TransactionResult.Builder();
                transactionResultBuilder
                        .gattState(getConnection().getGattState())
                        .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
                TransactionResult result = transactionResultBuilder.build();
                result.setData(fakeData);
                callCallbackWithTransactionResultAndRelease(callback, result);
                getConnection().setState(GattState.IDLE);
            }
        }, REASONABLE_TIME_FOR_CHARACTERISTIC_READ);
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristicCopy characteristic, int status) {
        Timber.v("Characteristic changed");
    }
}
