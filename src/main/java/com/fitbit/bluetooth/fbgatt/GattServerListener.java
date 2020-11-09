/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.btcopies.BluetoothGattCharacteristicCopy;
import com.fitbit.bluetooth.fbgatt.btcopies.BluetoothGattDescriptorCopy;
import com.fitbit.bluetooth.fbgatt.util.GattStatus;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

/**
 * This interface is public realistically only because of the relationship between packages
 * if you totally know what you are doing and you want to directly manage the relationship
 * between the raw callbacks and the gatt server then understand several things.  First
 * that the {@link BluetoothGattDescriptor} and the {@link BluetoothGattCharacteristic} that
 * are returned in these callbacks are all shallow copies, they do not have the {@link BluetoothGattDescriptor#getCharacteristic()}
 * cloned, and the {@link BluetoothGattCharacteristic#getDescriptors()} etc ... deep copying
 * <p>
 * If you want to generically handle callbacks and you are a library user, you should use
 * {@link ServerConnectionEventListener}.  It will provide all of the UUIDs you need to fetch
 * the server descriptors and characteristics in the form of a {@link TransactionResult} provided
 * to the callback
 */

interface GattServerListener {
    /**
     * *DO NOT USE UNLESS YOU KNOW WHAT YOU ARE DOING!!!* library internal method for notifying the {@link FitbitGatt} that the connection
     * state has changed
     *
     * @param device   The bluetooth device
     * @param status   The status
     * @param newState The new state
     */
    void onServerConnectionStateChange(BluetoothDevice device, int status, int newState);

    /**
     * *DO NOT USE UNLESS YOU KNOW WHAT YOU ARE DOING!!!* library internal method for notifying the {@link FitbitGatt} that
     * a server service was added
     *
     * @param status  The status of the operation
     * @param service The pointer to the {@link BluetoothGattService} that was added, note this can change while you are holding it.
     */
    void onServerServiceAdded(int status, BluetoothGattService service);

    /**
     * *DO NOT USE UNLESS YOU KNOW WHAT YOU ARE DOING!!!* library internal method for notifying the {@link FitbitGatt} that the server received
     * a characteristic read request
     *
     * @param device         The bluetooth device
     * @param requestId      The request id
     * @param offset         The offset
     * @param characteristic A shallow copy of the characteristic. Stuff will be missing!!!!
     */
    void onServerCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristicCopy characteristic);

    /**
     * *DO NOT USE UNLESS YOU KNOW WHAT YOU ARE DOING!!!* library internal method for notifying the {@link FitbitGatt} that the server received
     * a characteristic read request
     *
     * @param device         The bluetooth device
     * @param requestId      The request id
     * @param characteristic A shallow copy of the characteristic. Stuff will be missing!!!!
     * @param preparedWrite  whether this is a prepared write
     * @param responseNeeded Whether a response is required or not
     * @param offset         The offset
     * @param value          A deep copy of the value from the remote device, this will not change
     */
    void onServerCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristicCopy characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value);

    /**
     * *DO NOT USE UNLESS YOU KNOW WHAT YOU ARE DOING!!!* library internal method for notifying the {@link FitbitGatt} that the server received
     * a descriptor read request
     *
     * @param device     The bluetooth device
     * @param requestId  The request id
     * @param offset     The offset
     * @param descriptor A shallow copy of the descriptor. Stuff will be missing!!!!!
     */
    void onServerDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptorCopy descriptor);

    /**
     * *DO NOT USE UNLESS YOU KNOW WHAT YOU ARE DOING!!!* library internal method for notifying the {@link FitbitGatt} that the server received
     * a descriptor write request
     *
     * @param device         The bluetooth device
     * @param requestId      The request id
     * @param descriptor     A shallow copy of the descriptor. Stuff will be missing!!!!!
     * @param preparedWrite  whether this is a prepared write
     * @param responseNeeded Whether a response is required or not
     * @param offset         The offset
     * @param value          A deep copy of the value from the remote device, this will not change
     */
    void onServerDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptorCopy descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value);

    /**
     * *DO NOT USE UNLESS YOU KNOW WHAT YOU ARE DOING!!!* library internal method for notifying the {@link FitbitGatt} that the server
     * executed a write
     *
     * @param device    The bluetooth device
     * @param requestId The request id
     * @param execute   Whether it should execute the write or not
     */
    void onServerExecuteWrite(BluetoothDevice device, int requestId, boolean execute);

    /**
     * *DO NOT USE UNLESS YOU KNOW WHAT YOU ARE DOING!!!* library internal method for notifying the {@link FitbitGatt} that the server
     * sent a notification
     *
     * @param device The bluetooth device
     * @param status The status
     */
    void onServerNotificationSent(BluetoothDevice device, GattStatus status);

    /**
     * *DO NOT USE UNLESS YOU KNOW WHAT YOU ARE DOING!!!* library internal method for notifying the {@link FitbitGatt} that the server
     * updated the MTU
     *
     * @param device The bluetooth device
     * @param mtu    The mtu
     */
    void onServerMtuChanged(BluetoothDevice device, int mtu);

    /**
     * *DO NOT USE UNLESS YOU KNOW WHAT YOU ARE DOING!!!* library internal method for notifying the {@link FitbitGatt} that the server
     * updated the physical layer
     *
     * @param device The bluetooth device
     * @param txPhy  The tx physical layer const
     * @param rxPhy  The rx physical layer const
     * @param status The operation status
     */
    void onServerPhyUpdate(BluetoothDevice device, int txPhy, int rxPhy, int status);

    /**
     * *DO NOT USE UNLESS YOU KNOW WHAT YOU ARE DOING!!!* library internal method for notifying the {@link FitbitGatt} that the server
     * read the physical layer
     *
     * @param device The bluetooth device
     * @param txPhy  The tx physical layer const
     * @param rxPhy  The rx physical layer const
     * @param status The operation status
     */
    void onServerPhyRead(BluetoothDevice device, int txPhy, int rxPhy, int status);
}
