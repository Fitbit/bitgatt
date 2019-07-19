/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import androidx.annotation.NonNull;

/**
 * Will allow a connection user to establish a listener for asynchronous events
 * Non-Null annotations are for Kotlin
 *
 * Created by iowens on 12/2/17.
 */

public interface ConnectionEventListener {
    /**
     * Will be called when a characteristic notification happens on any connection
     * @param result The {@link TransactionResult} mapping to this event
     * @param connection The {@link GattConnection} for which this event originated
     */
    void onClientCharacteristicChanged(@NonNull TransactionResult result, @NonNull GattConnection connection);

    /**
     * Will be called if the gatt client connection disconnected or connected
     * @param result The {@link TransactionResult} mapping to this event
     * @param connection The {@link GattConnection} for which this event originated
     */
    void onClientConnectionStateChanged(@NonNull TransactionResult result, @NonNull GattConnection connection);

    /**
     * Will be called if the OS does another service discovery because the 0x1800 service changed
     * characteristic was notified
     * @param result The {@link TransactionResult} mapping to this event
     * @param connection The {@link GattConnection} for which this event originated
     */
    void onServicesDiscovered(@NonNull TransactionResult result, @NonNull GattConnection connection);

    /**
     * Will be called if the MTU changed on the client connection
     * @param result The {@link TransactionResult} mapping to this event
     * @param connection The {@link GattConnection} for which this event originated
     */
    void onMtuChanged(@NonNull TransactionResult result, @NonNull GattConnection connection);

    /**
     * Will be called if the physical layer changed
     * @param result The {@link TransactionResult} mapping to this event
     * @param connection The {@link GattConnection} for which this event originated
     */
    void onPhyChanged(@NonNull TransactionResult result, @NonNull GattConnection connection);
}
