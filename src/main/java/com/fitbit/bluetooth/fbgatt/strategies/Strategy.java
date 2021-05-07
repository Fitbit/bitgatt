/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.strategies;

import com.fitbit.bluetooth.fbgatt.AndroidDevice;
import com.fitbit.bluetooth.fbgatt.GattClientTransaction;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattServerTransaction;
import com.fitbit.bluetooth.fbgatt.GattTransaction;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.TransactionResult;

import androidx.annotation.Nullable;
import timber.log.Timber;

public abstract class Strategy {
    final GattConnection connection;
    final AndroidDevice currentAndroidDevice;

    Strategy(@Nullable GattConnection connection, AndroidDevice currentAndroidDevice) {
        this.connection = connection;
        this.currentAndroidDevice = currentAndroidDevice;
    }

    public abstract void applyStrategy();

    /**
     * Optional call providing result and callback
     * @param tx The current gatt transaction
     * @param result The transaction result
     * @param callback The gatt transaction callback
     */
    public void applyStrategy(GattClientTransaction tx, TransactionResult result, GattTransactionCallback callback) {
        // if used should be overridden
        Timber.e("Need to override implementation, do not call super");
    }

    /**
     * Optional call providing result and callback
     * @param tx The current gatt transaction
     * @param result The transaction result
     * @param callback The gatt transaction callback
     */
    public void applyStrategy(GattServerTransaction tx, TransactionResult result, GattTransactionCallback callback) {
        // if used should be overridden
        Timber.e("Need to override implementation, do not call super");
    }
}
