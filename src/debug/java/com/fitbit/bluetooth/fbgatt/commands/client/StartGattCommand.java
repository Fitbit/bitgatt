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

import android.content.Context;
import android.os.ParcelUuid;

import java.util.ArrayList;
import java.util.List;

/**
 * Stetho command for starting the Gatt server.
 */
public class StartGattCommand extends AbstractGattCommand {
    private final Context context;
    private final FitbitGatt fitbitGatt;

    public StartGattCommand(Context context, FitbitGatt fitbitGatt, PluginLoggerInterface logger) {
        super("init", "init", "Description: Will initialize the gatt server and start passively scanning for devices", logger);
        this.context = context;
        this.fitbitGatt = fitbitGatt;
    }

    @Override
    public void run(PluginCommandConfig config) {
        try {
            fitbitGatt.startGattClient(context);
            fitbitGatt.startGattServer(context);

            List<ParcelUuid> serviceUuids = new ArrayList<>();
            serviceUuids.add(ParcelUuid.fromString("ADABFB00-6E7D-4601-BDA2-BFFAA68956BA"));

            fitbitGatt.setScanServiceUuidFilters(serviceUuids);
            fitbitGatt.startHighPriorityScan(fitbitGatt.getAppContext());
            fitbitGatt.initializeScanner(context);

            onSuccess(config, "Gatt Ready");
        } catch (Exception e) {
            onError(config, e);
        }
    }
}
