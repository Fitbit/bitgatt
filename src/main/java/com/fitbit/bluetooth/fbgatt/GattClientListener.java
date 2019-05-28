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

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.Nullable;

/**
 * Interface for subscribers who want to listen directly to gatt client events.  These callbacks are
 * typically used for the transactions internally, but there is no benefit to preventing outsiders
 * from using this interface
 *
 * Created by iowens on 10/18/17.
 */

public interface GattClientListener {
    /**
     * Will return the {@link FitbitBluetoothDevice}
     * @return The {@link FitbitBluetoothDevice}
     */
    @Nullable
    FitbitBluetoothDevice getDevice();

    /**
     * Callback for when the physical layer of the connection between the phone and the peripheral
     * is updated
     * @param gatt The raw {@link BluetoothGatt} instance
     * @param txPhy The new TX Phy
     * @param rxPhy The new RX Phy
     * @param status The status after the change
     */
    void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status);

    /**
     * The result of a physical layer read request
     * @param gatt The raw {@link BluetoothGatt} instance
     * @param txPhy The new TX Phy
     * @param rxPhy The new RX Phy
     * @param status The status after the change
     */
    void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status);

    /**
     * The master callback, will inform whenever the client connection state changed, will return
     * things for newState like {@link BluetoothGatt#STATE_DISCONNECTED} it will also indicate that
     * a particular peripheral is connected, this
     * @param gatt The raw {@link BluetoothGatt} instance
     * @param status The operation status after the connection state change
     * @param newState The new state after the connection event
     */
    void onConnectionStateChange(BluetoothGatt gatt, int status, int newState);

    /**
     * The callback for after service discovery is completed on a remote client please keep in mind
     * that this is a shared resource, so this particular API might be called multiple times
     * @param gatt The raw {@link BluetoothGatt} instance
     * @param status The operation status after the service discovery, can indicated a failed discovery
     */
    void onServicesDiscovered(BluetoothGatt gatt, int status);

    /**
     * The raw characteristic read result, please keep in mind that the characteristic handed back is a live object
     * and that the value payload can change while you are holding it
     * @param gatt The raw {@link BluetoothGatt} instance
     * @param characteristic The {@link BluetoothGattCharacteristic} that changed
     * @param status The read operation status
     */
    void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristicCopy characteristic, int status);
    /**
     * The raw characteristic write result, please keep in mind that the characteristic handed back is a live object
     * and that the value payload can change while you are holding it
     * @param gatt The raw {@link BluetoothGatt} instance
     * @param characteristic The {@link BluetoothGattCharacteristic} that changed
     * @param status The read operation status
     */
    void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristicCopy characteristic, int status);

    /**
     * The characteristic change callback, will be called if the remote side notifies on a characteristic that
     * the client has both written the characteristic notification to as well as called
     * {@link BluetoothGatt#setCharacteristicNotification(BluetoothGattCharacteristic, boolean)}
     * @param gatt The raw {@link BluetoothGatt} instance
     * @param characteristic The {@link BluetoothGattCharacteristic} that changed
     */
    void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristicCopy characteristic);

    /**
     * The callback that will provide the result for a {@link BluetoothGatt#readDescriptor(BluetoothGattDescriptor)} call
     * @param gatt The raw {@link BluetoothGatt} instance
     * @param descriptor The {@link BluetoothGattDescriptor} that was read
     * @param status The operation status
     */
    void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptorCopy descriptor, int status);
    /**
     * The callback that will provide the result for a {@link BluetoothGatt#writeDescriptor(BluetoothGattDescriptor)} call
     * @param gatt The raw {@link BluetoothGatt} instance
     * @param descriptor The {@link BluetoothGattDescriptor} that was read
     * @param status The operation status
     */
    void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptorCopy descriptor, int status);

    /**
     * Will be called back when a reliable write eventually completes {@link BluetoothGatt#executeReliableWrite()}
     * @param gatt The raw {@link BluetoothGatt} instance
     * @param status The operation status
     */
    void onReliableWriteCompleted(BluetoothGatt gatt, int status);

    /**
     * The callback for the result of an {@link BluetoothGatt#readRemoteRssi()} call
     * @param gatt The raw {@link BluetoothGatt} instance
     * @param rssi The RSSI value
     * @param status The operation status
     */
    void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status);

    /**
     * The callback for the result of an {@link BluetoothGatt#requestMtu(int)} call, remember that this
     * can and probably will be denied on many phones
     * @param gatt The raw {@link BluetoothGatt} instance
     * @param mtu The new MTU, or the old one if unchanged
     * @param status The operation status
     */
    void onMtuChanged(BluetoothGatt gatt, int mtu, int status);
}
