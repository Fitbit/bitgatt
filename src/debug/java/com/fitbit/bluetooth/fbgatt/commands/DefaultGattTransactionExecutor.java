/*
 *
 *  Copyright 2021 Fitbit, Inc. All rights reserved.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package com.fitbit.bluetooth.fbgatt.commands;

import com.fitbit.bluetooth.fbgatt.FitbitBluetoothDevice;
import com.fitbit.bluetooth.fbgatt.GattServerConnection;
import com.fitbit.bluetooth.fbgatt.GattServerTransaction;
import java.util.concurrent.CountDownLatch;
import androidx.annotation.Nullable;

/**
 * Default implementation of the GattCommandExecutorInterface; runs Gatt transactions synchronously.
 */
public class DefaultGattTransactionExecutor implements GattCommandExecutorInterface {
    private final GattServerConnection serverConnection;
    private final GattCommandExecutorCallback callback;
    @Nullable
    private final FitbitBluetoothDevice.DevicePropertiesChangedCallback devicePropertiesListener;
    private final CountDownLatch countDownLatch;

    public DefaultGattTransactionExecutor(GattServerConnection serverConnection, GattCommandExecutorCallback callback, @Nullable FitbitBluetoothDevice.DevicePropertiesChangedCallback devicePropertiesListener) {
        this.serverConnection = serverConnection;
        this.callback = callback;
        this.devicePropertiesListener = devicePropertiesListener;
        this.countDownLatch = new CountDownLatch(1);
    }

    @Override
    public void runGattServerTransaction(GattServerTransaction tx, String successMsg, String failureMsg, boolean isJsonFormat) {
        FitbitBluetoothDevice device = tx.getDevice();
        if (devicePropertiesListener != null && device != null) {
            device.addDevicePropertiesChangedListener(devicePropertiesListener);
        }
        serverConnection.runTx(tx, result -> {
            callback.onResult(result);

            countDownLatch.countDown();
            if (devicePropertiesListener != null && device != null) {
                device.removeDevicePropertiesChangedListener(devicePropertiesListener);
            }
        });

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            callback.onError(e);
        }
    }
}
