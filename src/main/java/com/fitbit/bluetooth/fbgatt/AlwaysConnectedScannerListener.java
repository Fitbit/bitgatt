/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

/**
 * Will notify listeners of devices that are newly connected so that they can continue to discover
 * services, etc ... this listener will not notify about disconnections, because the scanner is
 * supposed to keep the device connected, therefore there is nothing to do if the device disconnects
 * but wait until it's connected again
 *
 * Created by iowens on 6/10/19.
 */

public interface AlwaysConnectedScannerListener {
    /**
     * Will be called back when the device connects
     *
     * @param connection the newly connected peripheral's connection object
     */
    void onPeripheralConnected(GattConnection connection);

    /**
     * The transaction result failure of the connection
     * @param result
     */
    void onPeripheralConnectionError(TransactionResult result);
}
