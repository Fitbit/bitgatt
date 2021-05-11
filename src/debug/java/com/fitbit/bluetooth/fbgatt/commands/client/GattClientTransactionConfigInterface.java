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

import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattServerConnection;
import com.fitbit.bluetooth.fbgatt.commands.GattTransactionConfigInterface;
import androidx.annotation.NonNull;

/**
 * Interface to be used for providing the required data for the GattClientTransactions.
 */
public interface GattClientTransactionConfigInterface extends GattTransactionConfigInterface {
    int getTxPhy();

    int getRxPhy();

    int getPhyOptions();

    String getConnectionInterval();

    int getMtu();

    void setConnection(@NonNull GattConnection connection);

    @NonNull
    GattConnection getConnection();

    @NonNull
    GattServerConnection getServerConnection();
}
