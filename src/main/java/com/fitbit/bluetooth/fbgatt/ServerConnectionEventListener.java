/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import android.bluetooth.BluetoothDevice;
import androidx.annotation.NonNull;

/**
 * Will allow a connection user to establish a listener for asynchronous events from the gatt
 * server
 *
 * NonNull for kotlin
 *
 * Created by iowens on 12/2/17.
 */

public interface ServerConnectionEventListener {
    /**
     * Will notify the listener that the server MTU has changed
     * @param device The bluetooth device
     * @param result a transaction result with all of the detail needed to act on this information
     * @param connection The gatt server connection object originating this event
     */
    void onServerMtuChanged(@NonNull BluetoothDevice device, @NonNull TransactionResult result, @NonNull GattServerConnection connection);
    /**
     * Will notify the listener that the server connection state has changed for a given device
     * @param device The bluetooth device
     * @param result a transaction result with all of the detail needed to act on this information
     * @param connection The gatt server connection object originating this event
     */
    void onServerConnectionStateChanged(@NonNull BluetoothDevice device, @NonNull TransactionResult result, @NonNull GattServerConnection connection);
    /**
     * Will notify the listener that the server has received a characteristic write request
     * @param device The bluetooth device
     * @param result a transaction result with all of the detail needed to act on this information
     * @param connection The gatt server connection object originating this event
     */
    void onServerCharacteristicWriteRequest(@NonNull BluetoothDevice device, @NonNull TransactionResult result, @NonNull GattServerConnection connection);
    /**
     * Will notify the listener that the server has received a characteristic read request
     * @param device The bluetooth device
     * @param result a transaction result with all of the detail needed to act on this information
     * @param connection The gatt server connection object originating this event
     */
    void onServerCharacteristicReadRequest(@NonNull BluetoothDevice device, @NonNull TransactionResult result, @NonNull GattServerConnection connection);
    /**
     * Will notify the listener that the server has received a descriptor write request
     * @param device The bluetooth device
     * @param result a transaction result with all of the detail needed to act on this information
     * @param connection The gatt server connection object originating this event
     */
    void onServerDescriptorWriteRequest(@NonNull BluetoothDevice device, @NonNull TransactionResult result, @NonNull GattServerConnection connection);
    /**
     * Will notify the listener that the server has received a descriptor read request
     * @param device The bluetooth device
     * @param result a transaction result with all of the detail needed to act on this information
     * @param connection The gatt server connection object originating this event
     */
    void onServerDescriptorReadRequest(@NonNull BluetoothDevice device, @NonNull TransactionResult result, @NonNull GattServerConnection connection);
}
