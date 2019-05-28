/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

/**
 * Just a wrapper for gatt transaction to help clarify the transactions
 *
 * Created by iowens on 12/13/17.
 */

public class GattServerTransaction extends GattTransaction {
    public GattServerTransaction(GattServerConnection server, GattState successEndState) {
        super(server, successEndState);
    }

    public GattServerTransaction(GattServerConnection server, GattState successEndState, long timeoutMillis) {
        super(server, successEndState, timeoutMillis);
    }

    @Override
    public String getName() {
        return null;
    }
}
