/*
 *
 *  * Copyright 2019 Fitbit, Inc. All rights reserved.
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.util.BluetoothManagerProvider;
import com.fitbit.bluetooth.fbgatt.util.BluetoothUtils;
import com.fitbit.bluetooth.fbgatt.util.GattUtils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import static com.fitbit.bluetooth.fbgatt.PeripheralScanner.BACKGROUND_SCAN_REQUEST_CODE;

/**
 * Allows us to inject external dependencies of {@link FitbitGatt} for easier testing
 *
 * Created by ilepadatescu on 09/23/2019
 */
class BitGattDependencyProvider {

    PeripheralScanner getNewPeripheralScanner(@NonNull PeripheralScanner.TrackerScannerListener listener, @NonNull FitbitGatt fbGatt) {
        return new PeripheralScanner(listener, fbGatt);
    }

    GattUtils getNewGattUtils() {
        return new GattUtils();
    }

    BluetoothUtils getBluetoothUtils() {
        return new BluetoothUtils();
    }

    BluetoothManagerProvider getBluetoothManagerProvider() {
        return new BluetoothManagerProvider();
    }

    LowEnergyAclListener getNewLowEnergyAclListener() {
        return new LowEnergyAclListener();
    }

    BluetoothRadioStatusListener getNewBluetoothRadioStatusListener(@NonNull Context context, boolean shouldInitialize) {
        return new BluetoothRadioStatusListener(context, shouldInitialize);
    }


    PendingIntent getNewScanPendingIntent(Context context, Intent regularIntent) {
        return PendingIntent.getBroadcast(context,
            BACKGROUND_SCAN_REQUEST_CODE, regularIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

}
