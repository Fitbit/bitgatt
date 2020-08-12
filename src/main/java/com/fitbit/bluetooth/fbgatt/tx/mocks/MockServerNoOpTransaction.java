/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx.mocks;

import com.fitbit.bluetooth.fbgatt.GattServerConnection;
import com.fitbit.bluetooth.fbgatt.GattServerTransaction;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import android.os.SystemClock;
import org.jetbrains.annotations.TestOnly;

/**
 * Simple transaction to use for testing
 */

@TestOnly
public class MockServerNoOpTransaction extends GattServerTransaction {
    private static final String NAME = "MockNoOpTransaction";
    private long sometime;

    public MockServerNoOpTransaction(GattServerConnection server, GattState successEndState, long timeToWait) {
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
