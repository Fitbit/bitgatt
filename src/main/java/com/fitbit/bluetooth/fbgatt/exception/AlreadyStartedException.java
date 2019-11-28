/*
 *
 *  Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package com.fitbit.bluetooth.fbgatt.exception;

/**
 * This is given if you are trying to start the same thing twice
 * <p>
 * Created by ilepadatescu on 09/30/19.
 */
public class AlreadyStartedException extends BitGattStartException {
    public AlreadyStartedException() {
        super("Component has already been started successfully");
    }
}
