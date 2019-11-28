/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.exception;

/**
 * This error is given when trying to start a part of bitgatt and bluetooth adapter is null
 * <p>
 * Created by ilepadatescu on 09/20/2019
 */
public class BluetoothNotEnabledException extends BitGattStartException {

    public BluetoothNotEnabledException() {
        super("Bluetooth is not enabled");
    }
}
