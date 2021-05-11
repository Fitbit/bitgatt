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

/**
 * Interface to be used for defining GattCommands.
 *
 * @param <Transaction> the resulting type of the GattTransaction (Server/Client)
 * @param <Config> the configuration to be used for generating the transaction
 */
public interface GattCommandInterface<Transaction, Config> {
    CommandRequest<Transaction> buildTransaction(Config config);

    String getSuccessMsg(Config config);

    String getFailureMsg(Config config);
}
