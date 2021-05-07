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

import com.fitbit.bluetooth.fbgatt.FitbitGatt;
import com.fitbit.bluetooth.fbgatt.commands.PluginCommandConfig;
import com.fitbit.bluetooth.fbgatt.logger.PluginLoggerInterface;

/**
 * Stetho command to find nearby devices.
 */
public class FindNearbyDevicesGattClientCommand extends AbstractFindNearbyDevicesCommand {
    public FindNearbyDevicesGattClientCommand(FitbitGatt fitbitGatt, PluginLoggerInterface logger) {
        super("fnd", "find-nearby-devices", "Description: Will find nearby, connected, and bonded devices", fitbitGatt, logger);
    }

    @Override
    void onPostProcessing(PluginCommandConfig config) {
        fitbitGatt.startHighPriorityScan(fitbitGatt.getAppContext());
    }
}
