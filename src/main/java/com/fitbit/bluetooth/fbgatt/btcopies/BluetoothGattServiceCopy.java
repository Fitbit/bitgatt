/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.btcopies;

import android.bluetooth.BluetoothDevice;
import java.util.ArrayList;
import java.util.List;
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

public class BluetoothGattServiceCopy {
    /**
     * Primary service
     */
    public static final int SERVICE_TYPE_PRIMARY = 0;

    /**
     * Secondary service (included by primary services)
     */
    public static final int SERVICE_TYPE_SECONDARY = 1;

    /**
     * The UUID of this service.
     */
    private UUID mUuid;

    /**
     * Service type (Primary/Secondary).
     */
    private int mServiceType;

    /**
     * List of characteristics included in this service.
     */
    private List<BluetoothGattCharacteristicCopy> mCharacteristics;

    /**
     * List of included services for this service.
     */
    private List<BluetoothGattServiceCopy> mIncludedServices;

    /**
     * Create a new BluetoothGattService.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param uuid The UUID for this service
     * @param serviceType The type of this service,
     * {@link BluetoothGattServiceCopy#SERVICE_TYPE_PRIMARY}
     * or {@link BluetoothGattServiceCopy#SERVICE_TYPE_SECONDARY}
     */
    public BluetoothGattServiceCopy(UUID uuid, int serviceType) {
        mUuid = uuid;
        mServiceType = serviceType;
        mCharacteristics = new ArrayList<BluetoothGattCharacteristicCopy>();
        mIncludedServices = new ArrayList<BluetoothGattServiceCopy>();
    }

    /**
     * Create a new BluetoothGattService
     *
     */
    /*package*/ BluetoothGattServiceCopy(BluetoothDevice device, UUID uuid,
                                     int instanceId, int serviceType) {
        mUuid = uuid;
        mServiceType = serviceType;
        mCharacteristics = new ArrayList<BluetoothGattCharacteristicCopy>();
        mIncludedServices = new ArrayList<BluetoothGattServiceCopy>();
    }

    /**
     * Create a new BluetoothGattService
     *
     */
    public BluetoothGattServiceCopy(UUID uuid, int instanceId, int serviceType) {
        mUuid = uuid;
        mServiceType = serviceType;
        mCharacteristics = new ArrayList<BluetoothGattCharacteristicCopy>();
        mIncludedServices = new ArrayList<BluetoothGattServiceCopy>();
    }

    /**
     * Add an included service to this service.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param service The service to be added
     * @return true, if the included service was added to the service
     */
    public boolean addService(BluetoothGattServiceCopy service) {
        mIncludedServices.add(service);
        return true;
    }

    /**
     * Add a characteristic to this service.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param characteristic The characteristics to be added
     * @return true, if the characteristic was added to the service
     */
    public boolean addCharacteristic(BluetoothGattCharacteristicCopy characteristic) {
        mCharacteristics.add(characteristic);
        characteristic.setService(this);
        return true;
    }

    /**
     * Returns the UUID of this service
     *
     * @return UUID of this service
     */
    public UUID getUuid() {
        return mUuid;
    }

    /**
     * Get the type of this service (primary/secondary)
     */
    public int getType() {
        return mServiceType;
    }

    /**
     * Get the list of included GATT services for this service.
     *
     * @return List of included services or empty list if no included services were discovered.
     */
    public List<BluetoothGattServiceCopy> getIncludedServices() {
        return mIncludedServices;
    }

    /**
     * Returns a list of characteristics included in this service.
     *
     * @return Characteristics included in this service
     */
    public List<BluetoothGattCharacteristicCopy> getCharacteristics() {
        return mCharacteristics;
    }

    /**
     * Returns a characteristic with a given UUID out of the list of
     * characteristics offered by this service.
     *
     * <p>This is a convenience function to allow access to a given characteristic
     * without enumerating over the list returned by {@link #getCharacteristics}
     * manually.
     *
     * <p>If a remote service offers multiple characteristics with the same
     * UUID, the first instance of a characteristic with the given UUID
     * is returned.
     *
     * @return GATT characteristic object or null if no characteristic with the given UUID was
     * found.
     */
    public BluetoothGattCharacteristicCopy getCharacteristic(UUID uuid) {
        for (BluetoothGattCharacteristicCopy characteristic : mCharacteristics) {
            if (uuid.equals(characteristic.getUuid())) {
                return characteristic;
            }
        }
        return null;
    }
}
