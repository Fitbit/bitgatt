/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.util;

import com.fitbit.bluetooth.fbgatt.BuildConfig;
import com.fitbit.bluetooth.fbgatt.btcopies.BluetoothGattCharacteristicCopy;
import com.fitbit.bluetooth.fbgatt.btcopies.BluetoothGattDescriptorCopy;
import com.fitbit.bluetooth.fbgatt.btcopies.BluetoothGattServiceCopy;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.Nullable;
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
                new BluetoothGattCharacteristicCopy(characteristic.getUuid(),
                        characteristic.getProperties(), characteristic.getPermissions());
        if (characteristic.getValue() != null) {
            newCharacteristic.setValue(Arrays.copyOf(characteristic.getValue(), characteristic.getValue().length));
        }
        if (!characteristic.getDescriptors().isEmpty()) {
            for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                BluetoothGattDescriptorCopy newDescriptor = new BluetoothGattDescriptorCopy(descriptor.getUuid(), descriptor.getPermissions());
                if (descriptor.getValue() != null) {
                    newDescriptor.setValue(Arrays.copyOf(descriptor.getValue(), descriptor.getValue().length));
                }
                newCharacteristic.addDescriptor(newDescriptor);
            }
        }
        if (characteristic.getService() != null) {
            BluetoothGattServiceCopy newService = new BluetoothGattServiceCopy(characteristic.getService().getUuid(), characteristic.getService().getType());
            newService.addCharacteristic(newCharacteristic);
        }
        return newCharacteristic;
    }

    /**
     * Will fetch the bluetooth adapter or return null if it's not available
     *
     * @param context The android context
     * @return The bluetooth adapter or null
     *
     * @deprecated see {{@link BluetoothUtils}}
     */
    @Deprecated
    public @Nullable
    BluetoothAdapter getBluetoothAdapter(Context context) {
        BluetoothManager manager = getBluetoothManager(context);
        if (manager == null) {
            return null;
        }
        BluetoothAdapter adapter = manager.getAdapter();
        if (adapter == null) {
            return null;
        }
        return adapter;
    }

    @Deprecated
    public @Nullable
    BluetoothManager getBluetoothManager(Context context) {
        return (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
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
                new BluetoothGattDescriptorCopy(descriptor.getUuid(),
                        descriptor.getPermissions());
        if (newDescriptor.getValue() != null) {
            newDescriptor.setValue(Arrays.copyOf(descriptor.getValue(), descriptor.getValue().length));
        }
        if (newDescriptor.getCharacteristic() != null) {
            BluetoothGattCharacteristicCopy oldCharacteristic = newDescriptor.getCharacteristic();
            BluetoothGattCharacteristicCopy copyOfCharacteristic = new BluetoothGattCharacteristicCopy(oldCharacteristic.getUuid(), oldCharacteristic.getProperties(), oldCharacteristic.getPermissions());
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
        BluetoothGattServiceCopy newService = new BluetoothGattServiceCopy(service.getUuid(), service.getType());
        if (!service.getIncludedServices().isEmpty()) {
            for (BluetoothGattService includedService : service.getIncludedServices()) {
                BluetoothGattServiceCopy newGattService = new BluetoothGattServiceCopy(includedService.getUuid(), includedService.getType());
                newService.addService(newGattService);
            }
        }
        if (!service.getCharacteristics().isEmpty()) {
            // why not use the copy characteristic method, it will implicitly link itself to the null service
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                BluetoothGattCharacteristicCopy newCharacteristic = new BluetoothGattCharacteristicCopy(characteristic.getUuid(), characteristic.getProperties(), characteristic.getPermissions());
                if (characteristic.getValue() != null) {
                    newCharacteristic.setValue(Arrays.copyOf(characteristic.getValue(), characteristic.getValue().length));
                }
                // why not use the copy descriptor method?  It will implicitly link itself to the null characteristic
                for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                    BluetoothGattDescriptorCopy newDescriptor = new BluetoothGattDescriptorCopy(descriptor.getUuid(), descriptor.getPermissions());
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
     * In release this method will return  "Unknown Name".
     *
     * @param localGatt The {@link BluetoothGatt} instance
     * @return The device name or "unknown" if null
     */
    public String debugSafeGetBtDeviceName(@Nullable BluetoothGatt localGatt) {
        //The problem with accessing this is that it may not be cached on some phones
        // resulting a long blocking operation
        if(BuildConfig.DEBUG) {
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

    public String debugSafeGetBtDeviceName(@Nullable BluetoothDevice device) {
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

    /**
     * Will take an int from the Major Device Class and translate it into a string
     *
     * @param devType The device type numerical value
     * @return The string value
     */

    public String getDevTypeDescription(int devType) {
        switch (devType) {
            case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                return "CLASSIC";
            case BluetoothDevice.DEVICE_TYPE_DUAL:
                return "DUAL";
            case BluetoothDevice.DEVICE_TYPE_LE:
                return "LE";
            case BluetoothDevice.DEVICE_TYPE_UNKNOWN:
                return "UNKNOWN";
            default:
                return "UNKNOWN" + Integer.toString(devType);
        }
    }

    /**
     * Will take an int from the Major Device Class and translate it into a string
     *
     * @param majDevClass The major device class numerical value
     * @return The string value
     */

    public String getMajDevClassDescription(int majDevClass) {
        switch (majDevClass) {
            case BluetoothClass.Device.Major.AUDIO_VIDEO:
                return "AUDIO_VIDEO";
            case BluetoothClass.Device.Major.COMPUTER:
                return "COMPUTER";
            case BluetoothClass.Device.Major.HEALTH:
                return "HEALTH";
            case BluetoothClass.Device.Major.IMAGING:
                return "IMAGING";
            case BluetoothClass.Device.Major.MISC:
                return "MISC";
            case BluetoothClass.Device.Major.NETWORKING:
                return "NETWORKING";
            case BluetoothClass.Device.Major.PERIPHERAL:
                return "PERIPHERAL";
            case BluetoothClass.Device.Major.PHONE:
                return "PHONE";
            case BluetoothClass.Device.Major.TOY:
                return "TOY";
            case BluetoothClass.Device.Major.UNCATEGORIZED:
                return "UNCATEGORIZED";
            case BluetoothClass.Device.Major.WEARABLE:
                return "WEARABLE";
            default:
                return "UNKNOWN" + Integer.toString(majDevClass);
        }
    }

    /**
     * Will take an int from the Device Class and translate it into a string
     *
     * @param devClass The device class numerical value
     * @return The string value
     */

    public String getDevClassDescription(int devClass) {
        switch (devClass) {
            case BluetoothClass.Device.AUDIO_VIDEO_CAMCORDER:
                return "AUDIO_VIDEO_CAMCORDER";
            case BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO:
                return "AUDIO_VIDEO_CAR_AUDIO";
            case BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE:
                return "AUDIO_VIDEO_HANDSFREE";
            case BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES:
                return "AUDIO_VIDEO_HEADPHONES";
            case BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO:
                return "AUDIO_VIDEO_HIFI_AUDIO";
            case BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER:
                return "AUDIO_VIDEO_LOUDSPEAKER";
            case BluetoothClass.Device.AUDIO_VIDEO_MICROPHONE:
                return "AUDIO_VIDEO_MICROPHONE";
            case BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO:
                return "AUDIO_VIDEO_PORTABLE_AUDIO";
            case BluetoothClass.Device.AUDIO_VIDEO_SET_TOP_BOX:
                return "AUDIO_VIDEO_SET_TOP_BOX";
            case BluetoothClass.Device.AUDIO_VIDEO_UNCATEGORIZED:
                return "AUDIO_VIDEO_UNCATEGORIZED";
            case BluetoothClass.Device.AUDIO_VIDEO_VCR:
                return "AUDIO_VIDEO_VCR";
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CAMERA:
                return "AUDIO_VIDEO_VIDEO_CAMERA";
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CONFERENCING:
                return "AUDIO_VIDEO_VIDEO_CONFERENCING";
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER:
                return "AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER";
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY:
                return "AUDIO_VIDEO_VIDEO_GAMING_TOY";
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_MONITOR:
                return "AUDIO_VIDEO_VIDEO_MONITOR";
            case BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET:
                return "AUDIO_VIDEO_WEARABLE_HEADSET";
            case BluetoothClass.Device.COMPUTER_DESKTOP:
                return "COMPUTER_DESKTOP";
            case BluetoothClass.Device.COMPUTER_HANDHELD_PC_PDA:
                return "COMPUTER_HANDHELD_PC_PDA";
            case BluetoothClass.Device.COMPUTER_LAPTOP:
                return "COMPUTER_LAPTOP";
            case BluetoothClass.Device.COMPUTER_PALM_SIZE_PC_PDA:
                return "COMPUTER_PALM_SIZE_PC_PDA";
            case BluetoothClass.Device.COMPUTER_SERVER:
                return "COMPUTER_SERVER";
            case BluetoothClass.Device.COMPUTER_UNCATEGORIZED:
                return "COMPUTER_UNCATEGORIZED";
            case BluetoothClass.Device.COMPUTER_WEARABLE:
                return "COMPUTER_WEARABLE";
            case BluetoothClass.Device.HEALTH_BLOOD_PRESSURE:
                return "HEALTH_BLOOD_PRESSURE";
            case BluetoothClass.Device.HEALTH_DATA_DISPLAY:
                return "HEALTH_DATA_DISPLAY";
            case BluetoothClass.Device.HEALTH_GLUCOSE:
                return "HEALTH_GLUCOSE";
            case BluetoothClass.Device.HEALTH_PULSE_OXIMETER:
                return "HEALTH_PULSE_OXIMETER";
            case BluetoothClass.Device.HEALTH_PULSE_RATE:
                return "HEALTH_PULSE_RATE";
            case BluetoothClass.Device.HEALTH_THERMOMETER:
                return "HEALTH_THERMOMETER";
            case BluetoothClass.Device.HEALTH_UNCATEGORIZED:
                return "HEALTH_UNCATEGORIZED";
            case BluetoothClass.Device.HEALTH_WEIGHING:
                return "HEALTH_WEIGHING";
            case BluetoothClass.Device.PHONE_CELLULAR:
                return "PHONE_CELLULAR";
            case BluetoothClass.Device.PHONE_CORDLESS:
                return "PHONE_CORDLESS";
            case BluetoothClass.Device.PHONE_ISDN:
                return "PHONE_ISDN";
            case BluetoothClass.Device.PHONE_MODEM_OR_GATEWAY:
                return "PHONE_MODEM_OR_GATEWAY";
            case BluetoothClass.Device.PHONE_SMART:
                return "PHONE_SMART";
            case BluetoothClass.Device.PHONE_UNCATEGORIZED:
                return "PHONE_UNCATEGORIZED";
            case BluetoothClass.Device.TOY_CONTROLLER:
                return "TOY_CONTROLLER";
            case BluetoothClass.Device.TOY_DOLL_ACTION_FIGURE:
                return "TOY_DOLL_ACTION_FIGURE";
            case BluetoothClass.Device.TOY_GAME:
                return "TOY_GAME";
            case BluetoothClass.Device.TOY_ROBOT:
                return "TOY_ROBOT";
            case BluetoothClass.Device.TOY_UNCATEGORIZED:
                return "TOY_UNCATEGORIZED";
            case BluetoothClass.Device.TOY_VEHICLE:
                return "TOY_VEHICLE";
            case BluetoothClass.Device.WEARABLE_GLASSES:
                return "WEARABLE_GLASSES";
            case BluetoothClass.Device.WEARABLE_HELMET:
                return "WEARABLE_HELMET";
            case BluetoothClass.Device.WEARABLE_JACKET:
                return "WEARABLE_JACKET";
            case BluetoothClass.Device.WEARABLE_PAGER:
                return "WEARABLE_PAGER";
            case BluetoothClass.Device.WEARABLE_UNCATEGORIZED:
                return "WEARABLE_UNCATEGORIZED";
            case BluetoothClass.Device.WEARABLE_WRIST_WATCH:
                return "WEARABLE_WRIST_WATCH";
            case BluetoothClass.Device.Major.UNCATEGORIZED:
                return "UNCATEGORIZED";
            default:
                return "UNKNOWN" + Integer.toString(devClass);
        }
    }
}
