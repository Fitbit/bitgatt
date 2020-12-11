package com.fitbit.bluetooth.fbgatt.tx;

import com.fitbit.bluetooth.fbgatt.FitbitBluetoothDevice;

public interface CreateBondTransactionInterface {
    FitbitBluetoothDevice getDevice();

    void bondSuccess();

    void bondFailure();
}
