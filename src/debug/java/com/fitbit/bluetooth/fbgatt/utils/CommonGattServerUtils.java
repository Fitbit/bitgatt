/*
 *
 *  Copyright 2021 Fitbit, Inc. All rights reserved.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package com.fitbit.bluetooth.fbgatt.utils;

import com.fitbit.bluetooth.fbgatt.FitbitGatt;
import com.fitbit.bluetooth.fbgatt.GattServerConnection;
import com.fitbit.bluetooth.fbgatt.tools.PluginConfig;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import androidx.annotation.NonNull;

/**
 * Utils class for interaction with the BluetoothGattServer.
 */
public class CommonGattServerUtils {
    @NonNull
    private final BluetoothGattServer server;

    public CommonGattServerUtils(@NonNull PluginConfig config) {
        FitbitGatt fitbitGatt = config.getFitbitGatt();
        if (fitbitGatt == null) {
            throw new IllegalArgumentException("FitbitGatt must not be null");
        }

        GattServerConnection serverConnection = fitbitGatt.getServer();
        if (serverConnection == null) {
            throw new IllegalArgumentException("GattServerConnection must not be null");
        }

        this.server = serverConnection.getServer();
    }

    public boolean isServiceDuplicate(@NonNull String serviceUuid) {
        for (BluetoothGattService service : this.server.getServices()) {
            String currentServiceUuid = service.getUuid().toString();
            if (serviceUuid.equals(currentServiceUuid)) {
                return true;
            }
        }

        return false;
    }
}
