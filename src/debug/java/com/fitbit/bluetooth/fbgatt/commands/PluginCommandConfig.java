/*
 *
 *  Copyright 2021 Fitbit, Inc. All rights reserved.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package com.fitbit.bluetooth.fbgatt.commands;

import com.fitbit.bluetooth.fbgatt.GattServerConnection;
import com.fitbit.bluetooth.fbgatt.tools.ConsumerInterface;
import com.fitbit.bluetooth.fbgatt.utils.GattConnectionUtils;
import java.util.Iterator;
import androidx.annotation.NonNull;

/**
 * Base config of a command
 */
public interface PluginCommandConfig {
    @NonNull
    GattServerConnection getServerConnection();

    @NonNull
    GattConnectionUtils getConnectionUtils();

    Iterator<String> getArgs();

    @NonNull
    ConsumerInterface getConsumer();

    boolean isJsonFormat();
}
