/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.strategies;

import android.os.Handler;
import android.os.Looper;

import com.fitbit.bluetooth.fbgatt.AndroidDevice;
import com.fitbit.bluetooth.fbgatt.GattClientTransaction;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransaction;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.TransactionResult;

import androidx.annotation.Nullable;

/**
 * This will need to be delayed in its response because if a developer chains a descriptor
 * write to this operation at least on the Pixel 3 with Antares, it can lead to a 133
 * which is likely some sort of Android bug as the descriptor write should be entirely
 * unrelated to the subscription.
 */
public class DelaySubscriptionResultStrategy extends Strategy {
    private static final int NON_GATT_OPERATION_INTERACTION_DELAY_MS = 50;
    private GattClientTransaction tx;
    private Handler mainThreadHandler;
    private TransactionResult result;
    private GattTransactionCallback gattTransactionCallback;

    /**
     * Constructor for strategy
     * @param connection The current connection
     * @param currentAndroidDevice The current Android device
     */

    public DelaySubscriptionResultStrategy(@Nullable GattConnection connection, AndroidDevice currentAndroidDevice) {
        super(connection, currentAndroidDevice);
        mainThreadHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void applyStrategy(GattClientTransaction tx, TransactionResult result, GattTransactionCallback callback) {
        this.tx = tx;
        this.result = result;
        this.gattTransactionCallback = callback;
        applyStrategy();
    }

    @Override
    public void applyStrategy() {
        mainThreadHandler.postDelayed(() -> {
            tx.callCallbackWithTransactionResultAndRelease(gattTransactionCallback, result);
            // this can fail, but we are still idle because we can use the connection
            connection.setState(GattState.IDLE);
        }, NON_GATT_OPERATION_INTERACTION_DELAY_MS);
    }
}
