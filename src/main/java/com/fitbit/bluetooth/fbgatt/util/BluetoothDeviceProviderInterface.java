package com.fitbit.bluetooth.fbgatt.util;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;

import javax.annotation.Nullable;

public interface BluetoothDeviceProviderInterface {
    @Nullable
    BluetoothDevice getFromIntent(@Nullable Intent intent);
}
