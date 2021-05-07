/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.logging;

import android.os.Build;
import android.util.Log;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import timber.log.Timber;

/**
 * Timber tree for debug logging
 */

public class BitgattDebugTree extends Timber.DebugTree {
    private static final int MAX_LOG_LENGTH = 4000;
    @Override
    protected void log(int priority, @Nullable String tag, @NonNull String message, @Nullable Throwable t) {
        // Workaround for devices that doesn't show lower priority logs
        if(Build.MANUFACTURER == null) {
            return;
        }
        if (Build.MANUFACTURER.equals("HUAWEI") || Build.MANUFACTURER.equals("samsung")) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG || priority == Log.INFO)
                priority = Log.ERROR;
        }
        if (priority >= Log.WARN) {
            if(message.length() < MAX_LOG_LENGTH) {
                super.log(priority, tag, message, t);
                return;
            }
            splitLogMessage(priority, tag, message, t);
        } else {
            if(message.length() < MAX_LOG_LENGTH) {
                super.log(priority, tag, message, t);
                return;
            }
            splitLogMessage(priority, tag, message, t);
        }
    }

    static void splitLogMessage(int priority, String tag, String message, Throwable t) {
        // Split by line, then ensure each line can fit into Log's maximum length.
        for (int i = 0, length = message.length(); i < length; i++) {
            int newline = message.indexOf('\n', i);
            newline = newline != -1 ? newline : length;
            do {
                int end = Math.min(newline, i + MAX_LOG_LENGTH);
                String part = message.substring(i, end);
                Log.println(priority, tag, part);
                i = end;
            } while (i < newline);
        }
    }
}
