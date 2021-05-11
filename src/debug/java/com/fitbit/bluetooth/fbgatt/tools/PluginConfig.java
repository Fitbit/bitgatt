/*
 *
 *  Copyright 2021 Fitbit, Inc. All rights reserved.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package com.fitbit.bluetooth.fbgatt.tools;

import com.fitbit.bluetooth.fbgatt.ConnectionEventListener;
import com.fitbit.bluetooth.fbgatt.FitbitBluetoothDevice;
import com.fitbit.bluetooth.fbgatt.FitbitGatt;
import com.fitbit.bluetooth.fbgatt.logger.PluginLoggerInterface;
import android.content.Context;
import androidx.annotation.NonNull;

/**
 * Configuration interface for the main plugin.
 */
public interface PluginConfig {
    /**
     * Provide the Context to be used by the plugin commands.
     *
     * @return Context
     */
    @NonNull
    Context getContext();

    /**
     * Provide the FitbitGatt instance to be used by the plugin commands.
     *
     * @return FitbitGatt
     */
    @NonNull
    FitbitGatt getFitbitGatt();

    /**
     * Provide the PluginLoggerInterface to be used by the plugin commands.
     *
     * @return PluginLoggerInterface
     */
    @NonNull
    PluginLoggerInterface getLogger();

    /**
     * Provide the DevicePropertiesChangedCallback to be used by the plugin commands.
     *
     * @return DevicePropertiesChangedCallback
     */
    @NonNull
    FitbitBluetoothDevice.DevicePropertiesChangedCallback getDevicePropertiesListener();

    /**
     * Provide the ConnectionEventListener to be used by the plugin commands.
     *
     * @return ConnectionEventListener
     */
    @NonNull
    ConnectionEventListener getConnectionEventListener();
}
