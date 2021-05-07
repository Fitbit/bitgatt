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

import com.fitbit.bluetooth.fbgatt.FitbitBluetoothDevice;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.logger.PluginLoggerInterface;
import androidx.annotation.NonNull;

/**
 * Base command that needs to notified of device properties changes.
 */
abstract class AbstractGattClientPropertiesListenerCommand extends AbstractGattClientCommand {
    private final FitbitBluetoothDevice.DevicePropertiesChangedCallback devicePropertiesChangedCallback;

    protected AbstractGattClientPropertiesListenerCommand(String shortName, String fullName, String description, PluginLoggerInterface logger, FitbitBluetoothDevice.DevicePropertiesChangedCallback devicePropertiesChangedCallback) {
        super(shortName, fullName, description, logger);
        this.devicePropertiesChangedCallback = devicePropertiesChangedCallback;
    }

    @Override
    protected void preExecute(@NonNull GattConnection connection) {
        connection.getDevice().addDevicePropertiesChangedListener(devicePropertiesChangedCallback);
    }

    @Override
    protected void postExecute(@NonNull GattConnection connection) {
        connection.getDevice().removeDevicePropertiesChangedListener(devicePropertiesChangedCallback);
    }
}
