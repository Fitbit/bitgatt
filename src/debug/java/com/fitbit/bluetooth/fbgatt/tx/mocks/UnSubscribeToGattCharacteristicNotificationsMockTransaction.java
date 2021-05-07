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
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.tx.UnSubscribeToGattCharacteristicNotificationsTransaction;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;
import androidx.annotation.Nullable;

/**
 * A mock unsubscribe transaction
 *
 * Created by iowens on 12/7/17.
 */

public class UnSubscribeToGattCharacteristicNotificationsMockTransaction extends UnSubscribeToGattCharacteristicNotificationsTransaction {
    private static final long REASONABLE_TIME_FOR_UNSUBSCRIPTION = 250;
    private final byte[] fakeData;
    private final boolean shouldFail;
    private final Handler mainHandler;

    public UnSubscribeToGattCharacteristicNotificationsMockTransaction(@Nullable GattConnection connection, GattState successEndState, BluetoothGattCharacteristic characteristic, byte[] fakeData, boolean shouldFail) {
        super(connection, successEndState, characteristic);
        this.fakeData = fakeData;
        this.shouldFail = shouldFail;
        this.mainHandler = getConnection().getMainHandler();
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        this.callback = callback;
        getConnection().setState(GattState.DISABLING_CHARACTERISTIC_NOTIFICATION);
        mainHandler.postDelayed(() -> {
            TransactionResult.Builder transactionResultBuilder = new TransactionResult.Builder().transactionName(getName());
            if(shouldFail) {
                getConnection().setState(GattState.DISABLING_CHARACTERISTIC_NOTIFICATION);
                transactionResultBuilder
                        .gattState(getConnection().getGattState())
                        .resultStatus(TransactionResult.TransactionResultStatus.TIMEOUT);
                TransactionResult result = transactionResultBuilder.build();
                result.setData(fakeData);
                callCallbackWithTransactionResultAndRelease(callback, result);
            } else {
                getConnection().setState(GattState.ENABLE_CHARACTERISTIC_NOTIFICATION_SUCCESS);
                transactionResultBuilder
                        .gattState(getConnection().getGattState())
                        .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
                TransactionResult result = transactionResultBuilder.build();
                result.setData(fakeData);
                callCallbackWithTransactionResultAndRelease(callback, result);
            }
            getConnection().setState(GattState.IDLE);
        }, REASONABLE_TIME_FOR_UNSUBSCRIPTION);
    }
}
