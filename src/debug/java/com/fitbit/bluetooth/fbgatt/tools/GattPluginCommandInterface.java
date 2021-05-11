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

import com.fitbit.bluetooth.fbgatt.commands.PluginCommandConfig;

/**
 * Interface for Gatt plugin commands.
 */
public interface GattPluginCommandInterface {
    /**
     * Get the fullname of the command.
     *
     * @return the fullname of the command
     */
    String getFullName();

    /**
     * Get the short version of the command name.
     *
     * @return the short version of the command name
     */
    String getShortName();

    /**
     * Get the description of the command.
     *
     * @return the description of the command
     */
    String getDescription();

    /**
     * Start executing the command based on a given configuration.
     *
     * @param config the configuration to be used for running the command
     */
    void run(PluginCommandConfig config);
}
