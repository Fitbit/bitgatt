package com.fitbit.bluetooth.fbgatt.util;

import com.fitbit.bluetooth.fbgatt.FitbitBluetoothDevice;

import android.bluetooth.BluetoothDevice;

public interface DeviceMatcherInterface {
    boolean matchDevices(FitbitBluetoothDevice fitbitBtDevice, BluetoothDevice btDevice);
}
