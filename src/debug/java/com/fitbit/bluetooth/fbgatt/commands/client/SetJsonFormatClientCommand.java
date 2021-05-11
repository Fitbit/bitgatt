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
import com.fitbit.bluetooth.fbgatt.tools.ConsumerInterface;
import com.fitbit.bluetooth.fbgatt.utils.OutputFormatStateController;

import com.facebook.stetho.dumpapp.ArgsHelper;

/**
 * Stetho command for toggling the output format to JSON.
 */
public class SetJsonFormatClientCommand extends AbstractGattCommand {
    private static final String JSON_ON_VALUE = "on";
    private static final String JSON_OFF_VALUE = "off";

    private final OutputFormatStateController formatController;

    public SetJsonFormatClientCommand(OutputFormatStateController formatController, PluginLoggerInterface logger) {
        super("sjo", "set-json-output", "on/off\n\nDescription: Will enable json command line output or disable it", logger);
        this.formatController = formatController;
    }

    @Override
    public void run(PluginCommandConfig config) {
        String isJsonString = ArgsHelper.nextOptionalArg(config.getArgs(), null);
        ConsumerInterface consumer = config.getConsumer();
        String result = null;
        if (JSON_ON_VALUE.equalsIgnoreCase(isJsonString)) {
            formatController.setJsonFormat(true);
            result = "JSON format set to TRUE";
        } else if (JSON_OFF_VALUE.equalsIgnoreCase(isJsonString)) {
            formatController.setJsonFormat(false);
            result = "JSON format set to FALSE";
        }

        if (result != null) {
            consumer.consumeResult(result);
        } else {
            consumer.consumeError(new IllegalArgumentException("usage: dumpapp set-json-format on/off"));
        }
    }
}
