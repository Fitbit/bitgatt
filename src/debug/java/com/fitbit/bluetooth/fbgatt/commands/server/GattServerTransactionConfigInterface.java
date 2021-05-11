/*
 *
 *  Copyright 2021 Fitbit, Inc. All rights reserved.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package com.fitbit.bluetooth.fbgatt.commands.server;

import com.fitbit.bluetooth.fbgatt.GattServerConnection;
import com.fitbit.bluetooth.fbgatt.commands.GattTransactionConfigInterface;
import androidx.annotation.NonNull;

/**
 * Interface to be used for providing the required data for the GattServerTransactions.
 */
public interface GattServerTransactionConfigInterface extends GattTransactionConfigInterface {
    String getRequestId();

    String getStatus();

    String getOffset();

    @NonNull
    GattServerConnection getServerConnection();
}
