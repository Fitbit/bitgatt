/*
 *
 *  Copyright 2021 Fitbit, Inc. All rights reserved.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package com.fitbit.bluetooth.fbgatt.commands.client;

import com.fitbit.bluetooth.fbgatt.commands.AbstractGattCommand;
import com.fitbit.bluetooth.fbgatt.commands.PluginCommandConfig;
import com.fitbit.bluetooth.fbgatt.logger.PluginLoggerInterface;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import java.util.List;

/**
 * Stetho command for reading the number of active connections on Gatt.
 */
public class ReadNumGattActiveConnectionsCommand extends AbstractGattCommand {
    private final Context context;

    public ReadNumGattActiveConnectionsCommand(Context context, PluginLoggerInterface logger) {
        super("rngac", "read-num-gatt-active-connections", "Description: Read number of active connections on GATT", logger);
        this.context = context;
    }

    @Override
    public void run(PluginCommandConfig config) {
        BluetoothManager btManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (btManager == null) {
            return;
        }

        List<BluetoothDevice> devices = btManager.getConnectedDevices(BluetoothProfile.GATT);
        responseHandler.onMessage(config, Integer.toString(devices.size()));
    }
}
