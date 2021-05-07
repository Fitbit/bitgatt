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
import com.fitbit.bluetooth.fbgatt.commands.AbstractGattCommand;
import com.fitbit.bluetooth.fbgatt.commands.PluginCommandConfig;
import com.fitbit.bluetooth.fbgatt.logger.PluginLoggerInterface;

/**
 * Stetho command to stop background scanning.
 */
public class StopBackgroundScanGattClientCommand extends AbstractGattCommand {
    private final FitbitGatt fitbitGatt;

    public StopBackgroundScanGattClientCommand(FitbitGatt fitbitGatt, PluginLoggerInterface logger) {
        super("sbs", "stop-background-scan", "Description: Will stop the background scanner", logger);
        this.fitbitGatt = fitbitGatt;
    }

    @Override
    public void run(PluginCommandConfig config) {
        fitbitGatt.stopSystemManagedPendingIntentScan();
        String result = "Successfully stopped the managed system scans";
        config.getConsumer().consumeResult(result);
    }
}
