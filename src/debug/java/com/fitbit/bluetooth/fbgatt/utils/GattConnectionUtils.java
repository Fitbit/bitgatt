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

import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.tools.GattServerConnectionConsts;
import android.bluetooth.BluetoothDevice;
import java.util.ArrayList;
import androidx.annotation.NonNull;
import static android.bluetooth.BluetoothDevice.DEVICE_TYPE_CLASSIC;
import static android.bluetooth.BluetoothDevice.DEVICE_TYPE_DUAL;
import static android.bluetooth.BluetoothDevice.DEVICE_TYPE_LE;

/**
 * Utils class for GattConnection management.
 */
public class GattConnectionUtils {
    public ArrayList<GattConnection> clientConnections = new ArrayList<>();

    public void onClientDiscovered(GattConnection clientConnection) {
        if(clientConnections.contains(clientConnection)) {
            return;
        }

        this.clientConnections.add(clientConnection);
    }

    public void onClientDisconnected(GattConnection clientConnection) {
        this.clientConnections.remove(clientConnection);
    }

    public Iterable<String> getConnectionMacs() {
        ArrayList<String> macs = new ArrayList<>();
        for (GattConnection connection : clientConnections) {
            macs.add(connection.getDevice().getAddress());
        }

        return macs;
    }

    public GattConnection getConnectionForMac(String mac) {
        for (GattConnection c : this.clientConnections) {
            if (c.getDevice().getAddress().equals(mac)) {
                return c;
            }
        }

        return null;
    }

    @NonNull
    public GattServerConnectionConsts.DeviceType getDeviceType(@NonNull BluetoothDevice device) {
        switch (device.getType()) {
            case DEVICE_TYPE_CLASSIC:
                return GattServerConnectionConsts.DeviceType.DEVICE_TYPE_CLASSIC;
            case DEVICE_TYPE_DUAL:
                return GattServerConnectionConsts.DeviceType.DEVICE_TYPE_DUAL;
            case DEVICE_TYPE_LE:
                return GattServerConnectionConsts.DeviceType.DEVICE_TYPE_LE;
            default:
                return GattServerConnectionConsts.DeviceType.DEVICE_TYPE_UNKNOWN;
        }
    }
}
