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

import android.bluetooth.le.ScanFilter;

import java.util.ArrayList;

/**
 * Stetho command to find nearby devices in the background.
 */
public class FindNearbyDevicesBgGattClientCommand extends AbstractFindNearbyDevicesCommand {
    public FindNearbyDevicesBgGattClientCommand(FitbitGatt fitbitGatt, PluginLoggerInterface logger) {
        super("fndbkgnd", "find-nearby-devices-background", "Description: Will find nearby devices using the pending intent background scan", fitbitGatt, logger);
    }

    @Override
    void onPostProcessing(PluginCommandConfig config) {
        ScanFilter filter = new ScanFilter.Builder().build();
        ArrayList<ScanFilter> scanFilters = new ArrayList<>(1);
        scanFilters.add(filter);

        if (fitbitGatt.getAppContext() == null) {
            onError(config, new IllegalStateException("AppContext must not be null; not starting scanner"));
            return;
        }
        boolean didStart = fitbitGatt.startSystemManagedPendingIntentScan(fitbitGatt.getAppContext(), scanFilters);
        if (!didStart) {
            onMessage(config, "Scanner couldn't be started");
        }
    }
}
