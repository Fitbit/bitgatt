package com.fitbit.bluetooth.fbgatt.util;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;

import javax.annotation.Nullable;

public class BluetoothDeviceProvider implements BluetoothDeviceProviderInterface{
    @Nullable
    @Override
    public BluetoothDevice getFromIntent(@Nullable Intent intent) {
        return intent != null ? (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE): null;
    }
}
