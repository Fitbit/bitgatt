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
import com.fitbit.bluetooth.fbgatt.tx.ReadRssiTransaction;

import android.os.Handler;
import androidx.annotation.Nullable;

import java.util.Random;

/**
 * Read rssi transaction mock
 *
 * Created by iowens on 12/14/17.
 */

public class ReadRssiMockTransaction extends ReadRssiTransaction {

    private static final long REASONABLE_TIME_FOR_RSSI_READ = 250;
    private final int fakeRssi;
    private final boolean shouldFail;
    private final Handler mainHandler;

    public ReadRssiMockTransaction(@Nullable GattConnection connection, GattState successEndState, boolean shouldFail) {
        super(connection, successEndState);
        this.shouldFail = shouldFail;
        this.mainHandler = getConnection().getMainHandler();
        Random rn = new Random();
        int min = -254;
        int max = 0;
        fakeRssi = rn.nextInt(max - min + 1) + min;
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        this.callback = callback;
        getConnection().setState(GattState.READING_RSSI);
        mainHandler.postDelayed(() -> {
            if (shouldFail) {
                getConnection().setState(GattState.READ_RSSI_FAILURE);
                TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
                builder.gattState(getConnection().getGattState())
                        .resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
                callCallbackWithTransactionResultAndRelease(callback, builder.build());
                getConnection().setState(GattState.IDLE);
            } else {
                getConnection().setState(GattState.READ_RSSI_SUCCESS);
                TransactionResult.Builder builder = new TransactionResult.Builder();
                builder.gattState(getConnection().getGattState())
                        .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS)
                        .rssi(fakeRssi);
                callCallbackWithTransactionResultAndRelease(callback, builder.build());
                getConnection().setState(GattState.IDLE);
            }
        }, REASONABLE_TIME_FOR_RSSI_READ);
    }
}
