/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.util;

import android.bluetooth.BluetoothManager;
import android.content.Context;

import androidx.annotation.Nullable;

/**
 * Provides {@link android.bluetooth.BluetoothManager}
 */
public class BluetoothManagerProvider {

    /**
     * Get the {@link android.bluetooth.BluetoothManager}, will return null if Bluetooth feature not available
     */
    @Nullable
    public BluetoothManager get(Context context) {
        return (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
    }
}
