/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx.mocks;

import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattServerConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransaction;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.TransactionResult;

import android.os.SystemClock;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.TestOnly;

/**
 * Simple transaction to use for testing
 */

@TestOnly
public class MockNoOpTransaction extends GattTransaction {
    private static final String NAME = "MockNoOpTransaction";
    private long sometime;
    public MockNoOpTransaction(@Nullable GattConnection connection, GattState successEndState, long timeToWait) {
        super(connection, successEndState);
        this.sometime = timeToWait;
    }

    public MockNoOpTransaction(GattServerConnection server, GattState successEndState, long timeToWait) {
        super(server, successEndState);
        this.sometime = timeToWait;
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        // I'm just going to block for some time, this is for testing and is meant to be hideous.
        SystemClock.sleep(sometime);
        callCallbackWithTransactionResultAndRelease(callback, new TransactionResult.Builder().transactionName(NAME).resultStatus(TransactionResult.TransactionResultStatus.SUCCESS).build());
    }

    @Override
    public String getName() {
        return NAME;
    }
}
