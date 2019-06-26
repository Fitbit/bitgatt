/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import android.support.annotation.MainThread;
import android.support.annotation.NonNull;

/**
 * The callback via which we deliver the result of the gatt transaction, must always be delivered
 * on the ui thread
 *
 * Created by iowens on 11/6/17.
 */

public interface GattTransactionCallback {
    @MainThread
    void onTransactionComplete(@NonNull TransactionResult result);
}
