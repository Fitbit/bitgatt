/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.util;

import com.fitbit.bluetooth.fbgatt.btcopies.BluetoothGattCharacteristicCopy;
import com.fitbit.bluetooth.fbgatt.btcopies.BluetoothGattDescriptorCopy;
import com.fitbit.bluetooth.fbgatt.btcopies.BluetoothGattServiceCopy;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.support.annotation.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import timber.log.Timber;

/**
 * A simple utility class for gatt helper methods
 * <p>
 * Created by iowens on 6/5/18.
 */
public class GattUtils {
    /**
     * To prevent the characteristic from changing out from under us we need to copy it
     * <p>
     * This may happen under high throughput/concurrency
     *
     * @param characteristic The characteristic to be copied
     * @return The shallow-ish copy of the characteristic
     */
    public @Nullable
    BluetoothGattCharacteristicCopy copyCharacteristic(@Nullable BluetoothGattCharacteristic characteristic) {
        if (null == characteristic || null == characteristic.getUuid()) {
            return null;
        }
        BluetoothGattCharacteristicCopy newCharacteristic =
                new BluetoothGattCharacteristicCopy(UUID.fromString(characteristic.getUuid().toString()),
                        characteristic.getProperties(), characteristic.getPermissions());
        if (characteristic.getValue() != null) {
            newCharacteristic.setValue(Arrays.copyOf(characteristic.getValue(), characteristic.getValue().length));
        }
        if (!characteristic.getDescriptors().isEmpty()) {
            for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                BluetoothGattDescriptorCopy newDescriptor = new BluetoothGattDescriptorCopy(UUID.fromString(descriptor.getUuid().toString()), descriptor.getPermissions());
                if (descriptor.getValue() != null) {
                    newDescriptor.setValue(Arrays.copyOf(descriptor.getValue(), descriptor.getValue().length));
                }
                newCharacteristic.addDescriptor(newDescriptor);
            }
        }
        if (characteristic.getService() != null) {
            BluetoothGattServiceCopy newService = new BluetoothGattServiceCopy(UUID.fromString(characteristic.getService().getUuid().toString()), characteristic.getService().getType());
            newService.addCharacteristic(newCharacteristic);
        }
        return newCharacteristic;
    }

    /**
     * Will fetch the bluetooth adapter or return null if it's not available
     *
     * @param context The android context
     * @return The bluetooth adapter or null
     */

    public @Nullable
    BluetoothAdapter getBluetoothAdapter(Context context) {
        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager == null) {
            return null;
        }
        BluetoothAdapter adapter = manager.getAdapter();
        if (adapter == null) {
            return null;
        }
        return adapter;
    }

    /**
     * To prevent the descriptor from changing out from under us we need to copy it
     * <p>
     * This may happen under high throughput/concurrency
     *
     * @param descriptor The descriptor to be copied
     * @return The shallow-ish copy of the descriptor
     */
    public @Nullable
    BluetoothGattDescriptorCopy copyDescriptor(@Nullable BluetoothGattDescriptor descriptor) {
        if (null == descriptor || null == descriptor.getUuid()) {
            return null;
        }
        BluetoothGattDescriptorCopy newDescriptor =
                new BluetoothGattDescriptorCopy(UUID.fromString(descriptor.getUuid().toString()),
                        descriptor.getPermissions());
        if (newDescriptor.getValue() != null) {
            newDescriptor.setValue(Arrays.copyOf(descriptor.getValue(), descriptor.getValue().length));
        }
        if (newDescriptor.getCharacteristic() != null) {
            BluetoothGattCharacteristicCopy oldCharacteristic = newDescriptor.getCharacteristic();
            BluetoothGattCharacteristicCopy copyOfCharacteristic = new BluetoothGattCharacteristicCopy(UUID.fromString(oldCharacteristic.getUuid().toString()), oldCharacteristic.getProperties(), oldCharacteristic.getPermissions());
            if (oldCharacteristic.getValue() != null) {
                copyOfCharacteristic.setValue(Arrays.copyOf(oldCharacteristic.getValue(), oldCharacteristic.getValue().length));
            }

            copyOfCharacteristic.addDescriptor(newDescriptor);
        }
        return newDescriptor;
    }

    /**
     * Will return the copy of the service
     *
     * @param service The gatt service
     * @return a shallow-ish copy of the service
     */

    public @Nullable
    BluetoothGattServiceCopy copyService(@Nullable BluetoothGattService service) {
        if (null == service || null == service.getUuid()) {
            return null;
        }
        BluetoothGattServiceCopy newService = new BluetoothGattServiceCopy(UUID.fromString(service.getUuid().toString()), service.getType());
        if (!service.getIncludedServices().isEmpty()) {
            for (BluetoothGattService includedService : service.getIncludedServices()) {
                BluetoothGattServiceCopy newGattService = new BluetoothGattServiceCopy(UUID.fromString(includedService.getUuid().toString()), includedService.getType());
                newService.addService(newGattService);
            }
        }
        if (!service.getCharacteristics().isEmpty()) {
            // why not use the copy characteristic method, it will implicitly link itself to the null service
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                BluetoothGattCharacteristicCopy newCharacteristic = new BluetoothGattCharacteristicCopy(UUID.fromString(characteristic.getUuid().toString()), characteristic.getProperties(), characteristic.getPermissions());
                if (characteristic.getValue() != null) {
                    newCharacteristic.setValue(Arrays.copyOf(characteristic.getValue(), characteristic.getValue().length));
                }
                // why not use the copy descriptor method?  It will implicitly link itself to the null characteristic
                for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                    BluetoothGattDescriptorCopy newDescriptor = new BluetoothGattDescriptorCopy(UUID.fromString(descriptor.getUuid().toString()), descriptor.getPermissions());
                    if (descriptor.getValue() != null) {
                        newDescriptor.setValue(Arrays.copyOf(descriptor.getValue(), descriptor.getValue().length));
                    }
                    newCharacteristic.addDescriptor(newDescriptor);
                }
                newService.addCharacteristic(newCharacteristic);
            }
        }
        return newService;
    }

    /**
     * In some operations, it is important for us to be able to determine if a particular perhiperhal
     * is actually connected to the phone at the ACL level as our access to the GATT is somewhat
     * limited.  We understand that some phones lie, saying that all of it's bonded devices are connected,
     * like the HTC One M9 on 5.0.2
     *
     * @param context The android context
     * @param device  The bluetooth device which we want to check it's status
     * @return True if the device is connected to the phone, false if not
     */

    public boolean isPerhipheralCurrentlyConnectedToPhone(@Nullable Context context, @Nullable BluetoothDevice device) {
        if (context == null || device == null) {
            return false;
        }
        BluetoothManager mgr = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (mgr != null) {
            List<BluetoothDevice> devices = mgr.getConnectedDevices(BluetoothProfile.GATT);
            return devices.contains(device);
        } else {
            return false;
        }
    }

    /**
     * Protect against NPEs while getting the bluetooth device name if we suspect that it might
     * not be set on the remote peripheral
     *
     * @param localGatt The {@link BluetoothGatt} instance
     * @return The device name or "unknown" if null
     */

    public String safeGetBtDeviceName(@Nullable BluetoothGatt localGatt) {
        try {
            if (localGatt == null || localGatt.getDevice() == null) {
                throw new NullPointerException("The device was null inside of the gatt");
            }
            String btName = localGatt.getDevice().getName();
            if (btName != null) {
                return btName;
            }
        } catch (NullPointerException ex) {
            // https://fabric.io/fitbit7/android/apps/com.fitbit.fitbitmobile/issues/5a9637c68cb3c2fa63fa1333?time=last-seven-days
            Timber.e(ex, "get name internal to the gatt failed with an Parcel Read Exception NPE");
        }
        return "Unknown Name";
    }

    /**
     * Protect against NPEs while getting the bluetooth device name if we suspect that it might
     * not be set on the remote peripheral
     *
     * @param device The {@link BluetoothDevice} instance
     * @return The device name or "unknown" if null
     */

    public String safeGetBtDeviceName(@Nullable BluetoothDevice device) {
        try {
            if (device == null) {
                throw new NullPointerException("The device was null inside of the gatt");
            }
            String btName = device.getName();
            if (btName != null) {
                return btName;
            }
        } catch (NullPointerException ex) {
            // https://fabric.io/fitbit7/android/apps/com.fitbit.fitbitmobile/issues/5a9637c68cb3c2fa63fa1333?time=last-seven-days
            Timber.e(ex, "get name internal to the adapter failed with an  Parcel Read Exception NPE");
        }
        return "Unknown Name";
    }

    /**
     * Will take an int from the bond state intent extra and translate it into a string
     *
     * @param bondState The bond state integer
     * @return The string value
     */

    public String getBondStateDescription(int bondState) {
        switch (bondState) {
            case BluetoothDevice.BOND_NONE:
                return "NONE";
            case BluetoothDevice.BOND_BONDED:
                return "BONDED";
            case BluetoothDevice.BOND_BONDING:
                return "BONDING";
            default:
                return "UNKNOWN";
        }
    }

}
