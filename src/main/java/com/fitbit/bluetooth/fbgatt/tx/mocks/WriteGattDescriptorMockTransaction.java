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
import com.fitbit.bluetooth.fbgatt.tx.WriteGattDescriptorTransaction;
import com.fitbit.bluetooth.fbgatt.util.GattStatus;
import com.fitbit.bluetooth.fbgatt.util.GattUtils;

import android.bluetooth.BluetoothGattDescriptor;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * The mock write gatt descriptor mock transaction
 *
 * Created by iowens on 12/7/17.
 */

public class WriteGattDescriptorMockTransaction extends WriteGattDescriptorTransaction {
    private static final long REASONABLE_TIME_FOR_DESCRIPTOR_WRITE = 250;
    private final byte[] fakeData;
    private final boolean shouldFail;
    private final Handler mainHandler;
    private final BluetoothGattDescriptor descriptor;
    private long timeToWait = REASONABLE_TIME_FOR_DESCRIPTOR_WRITE;

    public WriteGattDescriptorMockTransaction(@Nullable GattConnection connection, GattState successEndState, BluetoothGattDescriptor descriptor, byte[] fakeData, boolean shouldFail, long timeToWait) {
        this(connection, successEndState, descriptor, fakeData, shouldFail);
        this.timeToWait = timeToWait;
    }

    public WriteGattDescriptorMockTransaction(@Nullable GattConnection connection, GattState successEndState, BluetoothGattDescriptor descriptor, byte[] fakeData, boolean shouldFail) {
        super(connection, successEndState, descriptor);
        this.fakeData = fakeData;
        this.shouldFail = shouldFail;
        this.descriptor = descriptor;
        this.mainHandler = getConnection().getMainHandler();
    }

    @Override
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public void transaction(GattTransactionCallback callback) {
        this.callback = callback;
        getConnection().setState(GattState.WRITING_DESCRIPTOR);
        mainHandler.postDelayed(() -> {
            TransactionResult.Builder transactionResultBuilder = new TransactionResult.Builder().transactionName(getName());
            if(shouldFail) {
                onDescriptorWrite(null, new GattUtils().copyDescriptor(descriptor), GattStatus.GATT_WRITE_NOT_PERMIT.getCode());
                getConnection().setState(GattState.WRITE_DESCRIPTOR_FAILURE);
                transactionResultBuilder
                        .gattState(getConnection().getGattState())
                        .resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
                TransactionResult result = transactionResultBuilder.build();
                result.setData(fakeData);
                callCallbackWithTransactionResultAndRelease(callback, result);
            } else {
                onDescriptorWrite(null, new GattUtils().copyDescriptor(descriptor), GattStatus.GATT_SUCCESS.getCode());
                getConnection().setState(GattState.WRITE_DESCRIPTOR_SUCCESS);
                transactionResultBuilder
                        .gattState(getConnection().getGattState())
                        .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
                TransactionResult result = transactionResultBuilder.build();
                result.setData(fakeData);
                callCallbackWithTransactionResultAndRelease(callback, result);
                getConnection().setState(GattState.IDLE);
            }
        }, timeToWait);
    }
}
