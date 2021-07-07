/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import java.util.List;
import androidx.annotation.Nullable;

/**
 * Facade for {@link BluetoothManager}
 */
public class BluetoothManagerFacade {
    @Nullable
    private  final  BluetoothManager manager;

    public BluetoothManagerFacade(Context context) {
        manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
    }

    public BluetoothGattServer openGattServer(Context context, BluetoothGattServerCallback callback) {
        if(manager == null) {
            return  null;
        }
        return manager.openGattServer(context, callback);
    }

    public @Nullable BluetoothAdapter getAdapter(){
        return BluetoothAdapter.getDefaultAdapter();
    }

    public List<BluetoothDevice> getConnectedDevices(int profile) {
        if(manager == null) {
            return  null;
        }
       return manager.getConnectedDevices(profile);
    }
}
