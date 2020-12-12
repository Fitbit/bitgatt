package com.fitbit.bluetooth.fbgatt.util;

import com.fitbit.bluetooth.fbgatt.FitbitBluetoothDevice;

import android.bluetooth.BluetoothDevice;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Main implementation of the [DeviceMatcherInterface].
 */
public class DeviceMatcher implements DeviceMatcherInterface {
    @Override
    public boolean matchDevices(@NonNull FitbitBluetoothDevice fitbitBtDevice, @Nullable BluetoothDevice btDevice) {
        return fitbitBtDevice != null && fitbitBtDevice.equals(btDevice);
    }
}
