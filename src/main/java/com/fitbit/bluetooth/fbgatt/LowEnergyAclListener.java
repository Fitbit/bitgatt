/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.tx.GattDisconnectTransaction;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import timber.log.Timber;

/**
 * ACL callback listener for fitbit gatt, will respond to peripheral connections to phone, and
 * disconnections from the phone. Note, this does not necessarily respond to disconnections / connections
 * from / to your process, I.E. BluetoothGatt#connect(...) / BluetoothDevice#connectGatt(...), and
 * BluetoothGatt#disconnect()
 *
 * Created by iowens on 7/19/19.
 */

public class LowEnergyAclListener extends BroadcastReceiver {
    private IntentFilter[] filters = {
        new IntentFilter(BluetoothDevice.ACTION_FOUND),
        new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED),
        new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED),
        new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
    };

    void register(Context context) {
        for (IntentFilter f : filters) {
            context.registerReceiver(this, f);
        }
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        Timber.i("BT change received !");
        if (device == null) {
            Timber.d("BT Device is null");
            return;
        }
        FitbitBluetoothDevice fbDevice = new FitbitBluetoothDevice(device);
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            Timber.i("%s Device found", device.getName());
            // device was discovered in scan but we don't necessarily want to just add it unless
            // it's connected to the phone
        }
        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            Timber.i("%s Device is now connected", device.getName());
            // device was connected to the phone, does not mean device is connected to the
            // app, this can create a new connection because we are certain that the connection
            // is not in the cache already
            if (!FitbitGatt.getInstance().isDeviceInConnections(fbDevice)) {
                FitbitGatt.getInstance().putConnectionIntoDevices(fbDevice, new GattConnection(fbDevice, context.getMainLooper()));
            }
        }
        if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            Timber.i("%s Device is disconnected", device.getName());
            GattConnection gattConnection = FitbitGatt.getInstance().getConnection(fbDevice);
            if (gattConnection != null) {
                // we must use the transaction because if we do not the connection will not be
                // blocked and a caller can perform some operation on a disconnecting device
                GattDisconnectTransaction tx = new GattDisconnectTransaction(gattConnection, GattState.DISCONNECTED);
                gattConnection.runTx(tx, result -> {
                    if (result.resultStatus.equals(TransactionResult.TransactionResultStatus.SUCCESS)) {
                        Timber.v("Successful disconnection");
                    } else if (result.resultStatus.equals(TransactionResult.TransactionResultStatus.INVALID_STATE)) {
                        Timber.i("The disconnect is being handled at the callback level");
                    } else {
                        Timber.w("Failed to disconnect");
                    }
                    Timber.i("%s Notifying listeners of connection disconnected", device.getName());
                    if (FitbitGatt.getInstance().getPeripheralScanner() != null) {
                        FitbitGatt.getInstance().getPeripheralScanner().onDeviceDisconnected(device);
                    }
                    FitbitGatt.getInstance().notifyListenersOfConnectionDisconnected(gattConnection);
                });
            }
        }
    }
}
