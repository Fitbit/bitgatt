/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.btcopies;

import android.bluetooth.BluetoothGatt;

import java.util.UUID;

/**
 * Using instantiated gatt objects causes the gatt queue to lock up.  This is because for native
 * objects, there are a number of @hide annotated values, but there is no way at runtime to
 * get / set these values, they can only be set by the native implementation.
 * So to make it clear when using a copy and when using a native object.
 *
 * As a further precaution, this class will NOT be parcelable.  If you wish to export it, create
 * a different object that parcels the byte[] inside and the uuid.
 */

public class BluetoothGattDescriptorCopy {
    /**
     * Value used to enable notification for a client configuration descriptor
     */
    public static final byte[] ENABLE_NOTIFICATION_VALUE = {0x01, 0x00};

    /**
     * Value used to enable indication for a client configuration descriptor
     */
    public static final byte[] ENABLE_INDICATION_VALUE = {0x02, 0x00};

    /**
     * Value used to disable notifications or indicatinos
     */
    public static final byte[] DISABLE_NOTIFICATION_VALUE = {0x00, 0x00};

    /**
     * Descriptor read permission
     */
    public static final int PERMISSION_READ = 0x01;

    /**
     * Descriptor permission: Allow encrypted read operations
     */
    public static final int PERMISSION_READ_ENCRYPTED = 0x02;

    /**
     * Descriptor permission: Allow reading with man-in-the-middle protection
     */
    public static final int PERMISSION_READ_ENCRYPTED_MITM = 0x04;

    /**
     * Descriptor write permission
     */
    public static final int PERMISSION_WRITE = 0x10;

    /**
     * Descriptor permission: Allow encrypted writes
     */
    public static final int PERMISSION_WRITE_ENCRYPTED = 0x20;

    /**
     * Descriptor permission: Allow encrypted writes with man-in-the-middle
     * protection
     */
    public static final int PERMISSION_WRITE_ENCRYPTED_MITM = 0x40;

    /**
     * Descriptor permission: Allow signed write operations
     */
    public static final int PERMISSION_WRITE_SIGNED = 0x80;

    /**
     * Descriptor permission: Allow signed write operations with
     * man-in-the-middle protection
     */
    public static final int PERMISSION_WRITE_SIGNED_MITM = 0x100;

    /**
     * The UUID of this descriptor.
     */
    private UUID mUuid;

    /**
     * Instance ID for this descriptor.
     */
    private int mInstance;

    /**
     * Permissions for this descriptor
     */
    private int mPermissions;

    /**
     * Back-reference to the characteristic this descriptor belongs to.
     */
    private BluetoothGattCharacteristicCopy mCharacteristic;

    /**
     * The value for this descriptor.
     */
    private byte[] mValue;

    /**
     * Create a new BluetoothGattDescriptor.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param uuid The UUID for this descriptor
     * @param permissions Permissions for this descriptor
     */
    public BluetoothGattDescriptorCopy(UUID uuid, int permissions) {
        initDescriptor(null, uuid, 0, permissions);
    }

    /**
     * Create a new BluetoothGattDescriptor.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param characteristic The characteristic this descriptor belongs to
     * @param uuid The UUID for this descriptor
     * @param permissions Permissions for this descriptor
     */
    /*package*/ BluetoothGattDescriptorCopy(BluetoothGattCharacteristicCopy characteristic, UUID uuid,
                                        int instance, int permissions) {
        initDescriptor(characteristic, uuid, instance, permissions);
    }

    public BluetoothGattDescriptorCopy(UUID uuid, int instance, int permissions) {
        initDescriptor(null, uuid, instance, permissions);
    }

    private void initDescriptor(BluetoothGattCharacteristicCopy characteristic, UUID uuid,
                                int instance, int permissions) {
        mCharacteristic = characteristic;
        mUuid = uuid;
        mInstance = instance;
        mPermissions = permissions;
    }

    /**
     * Returns the characteristic this descriptor belongs to.
     *
     * @return The characteristic.
     */
    public BluetoothGattCharacteristicCopy getCharacteristic() {
        return mCharacteristic;
    }

    /**
     * Set the back-reference to the associated characteristic
     */
    /*package*/ void setCharacteristic(BluetoothGattCharacteristicCopy characteristic) {
        mCharacteristic = characteristic;
    }

    /**
     * Returns the UUID of this descriptor.
     *
     * @return UUID of this descriptor
     */
    public UUID getUuid() {
        return mUuid;
    }

    /**
     * Returns the instance ID for this descriptor.
     *
     * <p>If a remote device offers multiple descriptors with the same UUID,
     * the instance ID is used to distuinguish between descriptors.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @return Instance ID of this descriptor
     * @hide
     */
    public int getInstanceId() {
        return mInstance;
    }

    /**
     * Force the instance ID.
     *
     * @hide
     */
    public void setInstanceId(int instanceId) {
        mInstance = instanceId;
    }

    /**
     * Returns the permissions for this descriptor.
     *
     * @return Permissions of this descriptor
     */
    public int getPermissions() {
        return mPermissions;
    }

    /**
     * Returns the stored value for this descriptor
     *
     * <p>This function returns the stored value for this descriptor as
     * retrieved by calling {@link BluetoothGatt#readDescriptor}. The cached
     * value of the descriptor is updated as a result of a descriptor read
     * operation.
     *
     * @return Cached value of the descriptor
     */
    public byte[] getValue() {
        return mValue;
    }

    /**
     * Updates the locally stored value of this descriptor.
     *
     * <p>This function modifies the locally stored cached value of this
     * descriptor. To send the value to the remote device, call
     * {@link BluetoothGatt#writeDescriptor} to send the value to the
     * remote device.
     *
     * @param value New value for this descriptor
     * @return true if the locally stored value has been set, false if the requested value could not
     * be stored locally.
     */
    public boolean setValue(byte[] value) {
        mValue = value;
        return true;
    }
}
