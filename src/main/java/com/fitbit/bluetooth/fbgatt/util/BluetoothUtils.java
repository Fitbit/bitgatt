/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;

import androidx.annotation.Nullable;

/**
 * Bluetooth Utils
 */
public class BluetoothUtils {

    private final BluetoothManagerProvider bluetoothManagerProvider;

    public BluetoothUtils() {
        this(new BluetoothManagerProvider());
    }

    BluetoothUtils(BluetoothManagerProvider bluetoothManagerProvider) {
        this.bluetoothManagerProvider = bluetoothManagerProvider;
    }

    /**
     * Will fetch the bluetooth adapter or return null if it's not available
     *
     * @param context The android context
     * @return The bluetooth adapter or null
     */
    @Nullable
    public BluetoothAdapter getBluetoothAdapter(Context context) {
        BluetoothManager manager = bluetoothManagerProvider.get(context);
        if (manager == null) {
            return null;
        }
        return manager.getAdapter();
    }

    /**
     * Returns a {@link BluetoothLeScanner} object for Bluetooth LE scan operations.
     */
    @Nullable
    public BluetoothLeScanner getBluetoothLeScanner(Context context) {
        BluetoothAdapter adapter = getBluetoothAdapter(context);
        // Normally we should not check if ble is enabled due to the underlying implementation
        // which check for bluetooth state. This is added to ensure consistency on all OEM's
        if (adapter == null || !isBluetoothEnabled(context)) {
            return null;
        }
        return adapter.getBluetoothLeScanner();
    }

    /**
     * Return true if Bluetooth is currently enabled and ready for use.
     *
     * @return true if the local adapter is turned on
     */
    public boolean isBluetoothEnabled(Context context) {
        BluetoothAdapter adapter = getBluetoothAdapter(context);
        if (adapter == null) {
            return false;
        }
        return adapter.isEnabled();
    }
}
