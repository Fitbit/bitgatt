/*
 *
 *  * Copyright 2019 Fitbit, Inc. All rights reserved.
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package com.fitbit.bluetooth.fbgatt.exception;

/**
 * This can be given if we try to start the scanner  without any filters being set
 * <p>
 * Created by ilepadatescu on 09/23/2019
 */
public class NoFiltersSetException extends BitGattStartException {
    public NoFiltersSetException() {
        super("Scanner has been instanced but no scan started since we don't have filters");
    }
}
