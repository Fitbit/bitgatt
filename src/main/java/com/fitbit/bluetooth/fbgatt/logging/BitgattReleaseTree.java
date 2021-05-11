/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.logging;

import android.util.Log;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import timber.log.Timber;

/**
 * Timber tree for release logging
 */

public class BitgattReleaseTree extends Timber.Tree {
    private static final int MAX_LOG_LENGTH = 4000;

    @Override
    protected void log(int priority, @Nullable String tag, @NonNull String message, @Nullable Throwable t) {
        if(priority >= Log.WARN) {
            if (message.length() < MAX_LOG_LENGTH) {
                Log.println(priority, tag, message);
                return;
            }
            BitgattDebugTree.splitLogMessage(priority, tag, message, t);
        }
    }
}
