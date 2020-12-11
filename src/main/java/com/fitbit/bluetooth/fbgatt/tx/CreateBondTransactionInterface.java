package com.fitbit.bluetooth.fbgatt.tx;

import com.fitbit.bluetooth.fbgatt.FitbitBluetoothDevice;

/**
 * Interface to be used as a bridge between the [CreateBondTransactionReceiver
 * and the [CreateBondTransaction].
 */
public interface CreateBondTransactionInterface {
    FitbitBluetoothDevice getDevice();

    void bondSuccess();

    void bondFailure();
}
