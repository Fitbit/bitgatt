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

import java.util.UUID;

import androidx.annotation.Nullable;

/**
 * This error is given when we managed to start the gatt server
 * but were unable to add the services given to it
 * <p>
 * Created by ilepadatescu on 09/21/2019
 */
public class AddingServiceOnStartException extends BitGattStartException {
    private UUID serviceUUID = null;

    public AddingServiceOnStartException(@Nullable UUID serviceUUID) {
        super(String.format("We failed adding services on start of gatt server service %s", serviceUUID != null ? serviceUUID.toString() : "UUID was null"));
        this.serviceUUID = serviceUUID;
    }

    @Nullable
    public UUID getServiceUUID() {
        return serviceUUID;
    }
}
