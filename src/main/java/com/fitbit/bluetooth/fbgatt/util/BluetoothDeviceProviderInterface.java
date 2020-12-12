package com.fitbit.bluetooth.fbgatt.util;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;

import androidx.annotation.Nullable;

/**
 * Interface to be used for retrieving a [BluetoothDevice] from the
 * [Intent]'s extras using the [BluetoothDevice.EXTRA_DEVICE].
 */
public interface BluetoothDeviceProviderInterface {
    /**
     * Returns the parsed [BluetoothDevice] if it can be parsed.
     *
     * @param intent - the [Intent] from which to retrieve the [BluetoothDevice]
     * @return the [BluetoothDevice] or null if it cannot be parsed or if the extras
     * do not hold such information.
     */
    @Nullable
    BluetoothDevice getFromIntent(@Nullable Intent intent);
}
