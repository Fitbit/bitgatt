/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.exception;

/**
 * Defines base error class for Bitgatt errors that can occur when starting a component of bitgatt
 * <p>
 * Created by ilepadatescu on 09/20/2019
 */
public abstract class BitGattStartException extends Exception {

    public BitGattStartException(String message) {
        super(message);
    }
}
