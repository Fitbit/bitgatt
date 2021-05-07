/*
 *
 *  Copyright 2021 Fitbit, Inc. All rights reserved.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package com.fitbit.bluetooth.fbgatt.commands;

import com.fitbit.bluetooth.fbgatt.GattServerTransaction;
import com.fitbit.bluetooth.fbgatt.TransactionResult;

/**
 * Interface to be used for executing GattTransactions and handling the results.
 */
public interface GattCommandExecutorInterface {
    void runGattServerTransaction(GattServerTransaction tx, String successMsg, String failureMsg, boolean isJsonFormat);

    interface GattCommandExecutorCallback {
        void onResult(TransactionResult result);

        void onError(Exception e);
    }
}
