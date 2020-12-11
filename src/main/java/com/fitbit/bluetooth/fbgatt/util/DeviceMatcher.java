package com.fitbit.bluetooth.fbgatt.util;

import com.fitbit.bluetooth.fbgatt.FitbitBluetoothDevice;

import android.bluetooth.BluetoothDevice;

public class DeviceMatcher implements DeviceMatcherInterface {
    @Override
    public boolean matchDevices(FitbitBluetoothDevice fitbitBtDevice, BluetoothDevice btDevice) {
        return fitbitBtDevice != null && fitbitBtDevice.equals(btDevice);
    }
}
