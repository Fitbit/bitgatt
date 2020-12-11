package com.fitbit.bluetooth.fbgatt.util;

import com.fitbit.bluetooth.fbgatt.FitbitBluetoothDevice;

import android.bluetooth.BluetoothDevice;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Util interface to be used for matching a [FitbitBluetoothDevice] to
 * a [BluetoothDevice].
 */
public interface DeviceMatcherInterface {
    /**
     * Method checking if the [FitbitBluetoothDevice] and the [BluetoothDevice]
     * represent the same object.
     *
     * @param fitbitBtDevice - to be used for matching
     * @param btDevice - to be used for matching
     *
     * @return true if they represent the same object, false otherwise.
     */
    boolean matchDevices(@Nonnull FitbitBluetoothDevice fitbitBtDevice, @Nullable BluetoothDevice btDevice);
}
