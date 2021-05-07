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

import com.fitbit.bluetooth.fbgatt.TransactionResult;

import java.util.Map;

/**
 * Interface to be used for handling the results of executed GattCommands.
 */
public interface ResponseHandlerInterface {
    void onMessage(PluginCommandConfig config, String message);

    void onResponse(PluginCommandConfig config, TransactionResult result);

    void onResponse(PluginCommandConfig config, TransactionResult result, String error, Map<String, Object> map);

    void onError(PluginCommandConfig config, Exception e);
}
