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
import com.fitbit.bluetooth.fbgatt.tools.GattPluginCommandInterface;

import java.util.ArrayList;
import java.util.List;

/**
 * Stetho command for displaying available commands.
 */
public class HelpCommand extends AbstractGattCommand {
    private final List<GattPluginCommandInterface> supportedCommands;

    public HelpCommand(ArrayList<GattPluginCommandInterface> commands, PluginLoggerInterface logger) {
        super("h", "help", "Description: Will print this help", logger);
        this.supportedCommands = commands;
    }

    @Override
    public void run(PluginCommandConfig config) {
        for (GattPluginCommandInterface command : this.supportedCommands) {
            String message = command.getFullName();
            if (command.getShortName() != null && !command.getShortName().isEmpty()) {
                message = message.concat(", " + command.getShortName());
            }

            message = message.concat(" | " + command.getDescription());
            onMessage(config, message);
        }
    }
}
