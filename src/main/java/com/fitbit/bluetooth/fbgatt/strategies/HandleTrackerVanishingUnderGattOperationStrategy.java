/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.strategies;

import com.fitbit.bluetooth.fbgatt.AndroidDevice;
import com.fitbit.bluetooth.fbgatt.FitbitGatt;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import android.bluetooth.BluetoothGatt;
import java.util.concurrent.TimeUnit;
import androidx.annotation.Nullable;
import timber.log.Timber;

/**
 * In the case that we have a tracker that has disconnected, in the middle of a write / read /
 * server response, we should mark that connection as disconnected and force the timeout wait with
 * a 133 unknown ( connection interface unknown ) gatt status.
 */

public class HandleTrackerVanishingUnderGattOperationStrategy extends Strategy {

    private static final long WAIT_TIME_FOR_DISCONNECTION = TimeUnit.SECONDS.toMillis(1);

    public HandleTrackerVanishingUnderGattOperationStrategy(@Nullable GattConnection connection, AndroidDevice androidDevice) {
        super(connection, androidDevice);
    }

    @Override
    public void applyStrategy() {
        Timber.d("Applying tracker vanishing while in gatt operation strategy");
        if(connection != null) {
            connection.setState(GattState.DISCONNECTING);
            BluetoothGatt localGatt = connection.getGatt();
            if(localGatt != null) {
                localGatt.disconnect();
            } else {
                Timber.d("Could not apply strategy because the gatt was null");
            }
        }
        // we might not get the gatt disconnect callback if the if is lost,
        // so in this scenario we will have to wait for the client_if to dump for sure,
        // at least 1s on pixel
        Timber.v("Waiting %dms for the client_if to dump", WAIT_TIME_FOR_DISCONNECTION);
        if(connection != null) {
            connection.getMainHandler().postDelayed(() -> {
                connection.setState(GattState.DISCONNECTED);
                if (FitbitGatt.getInstance().getServer() != null) {
                    FitbitGatt.getInstance().getServer().setState(GattState.DISCONNECTED);
                }
                Timber.v("Strategy is done, gatt can be used again");
            }, WAIT_TIME_FOR_DISCONNECTION);
        }
    }
}
