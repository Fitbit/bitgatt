/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx.mocks;

import android.bluetooth.BluetoothGattDescriptor;
import android.os.Handler;
import androidx.annotation.Nullable;

import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.tx.ReadGattDescriptorTransaction;

/**
 * Mock class for reading a descriptor
 *
 * Created by iowens on 12/6/17.
 */

public class ReadGattDescriptorMockTransaction extends ReadGattDescriptorTransaction {

    private static final long REASONABLE_TIME_FOR_DESCRIPTOR_READ = 250;
    private final byte[] fakeData;
    private final boolean shouldFail;
    private final Handler mainHandler;

    public ReadGattDescriptorMockTransaction(@Nullable GattConnection connection, GattState successEndState, BluetoothGattDescriptor descriptor, byte[] fakeData, boolean shouldFail) {
        super(connection, successEndState, descriptor);
        this.fakeData = fakeData;
        this.shouldFail = shouldFail;
        this.mainHandler = getConnection().getMainHandler();
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        this.callback = callback;
        getConnection().setState(GattState.READING_DESCRIPTOR);
        mainHandler.postDelayed(() -> {
            if(shouldFail) {
                getConnection().setState(GattState.READ_DESCRIPTOR_FAILURE);
                TransactionResult.Builder transactionResultBuilder = new TransactionResult.Builder().transactionName(getName());
                transactionResultBuilder
                        .gattState(getConnection().getGattState())
                        .resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
                TransactionResult result = transactionResultBuilder.build();
                result.setData(fakeData);
                callCallbackWithTransactionResultAndRelease(callback, result);
            } else {
                getConnection().setState(GattState.READ_DESCRIPTOR_SUCCESS);
                TransactionResult.Builder transactionResultBuilder = new TransactionResult.Builder();
                transactionResultBuilder
                        .gattState(getConnection().getGattState())
                        .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
                TransactionResult result = transactionResultBuilder.build();
                result.setData(fakeData);
                callCallbackWithTransactionResultAndRelease(callback, result);
                getConnection().setState(GattState.IDLE);
            }
        }, REASONABLE_TIME_FOR_DESCRIPTOR_READ);
    }
}
