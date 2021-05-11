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

import com.fitbit.bluetooth.fbgatt.BuildConfig;
import com.fitbit.bluetooth.fbgatt.commands.AbstractGattCommand;
import com.fitbit.bluetooth.fbgatt.commands.PluginCommandConfig;
import com.fitbit.bluetooth.fbgatt.logger.PluginLoggerInterface;

/**
 * Stetho command for reading the Gatt version.
 */
public class ReadGattLibVersionCommand extends AbstractGattCommand {
    public ReadGattLibVersionCommand(PluginLoggerInterface logger) {
        super("rglv", "read-gatt-lib-version", "Description: Print version of the GATT library in use", logger);
    }

    @Override
    public void run(PluginCommandConfig config) {
        String gattLibVersion = BuildConfig.VERSION_NAME;
        responseHandler.onMessage(config, gattLibVersion);
    }
}
