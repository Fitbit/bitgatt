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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import androidx.annotation.VisibleForTesting;
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

    /**
     * To ensure that we remove any registered instances for anyone using this listener because
     * there is a limit of 1000 global listeners per Android process, looping could end up
     * hitting this.
     */
    private static AtomicBoolean aclListenerRegistered = new AtomicBoolean(false);
    @VisibleForTesting
    static AtomicInteger timesRegistered = new AtomicInteger(0);

    private IntentFilter[] filters = {
        new IntentFilter(BluetoothDevice.ACTION_FOUND),
        new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED),
        new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED),
        new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
    };

    synchronized void register(Context context) {
        // we want to ensure that we are only registered once for this
        if(aclListenerRegistered.get()) {
            Timber.d("Already registered this receiver");
            return;
        }

        for (IntentFilter f : filters) {
            context.registerReceiver(this, f);
        }
        int newValue = timesRegistered.incrementAndGet();
        // just to help someone debug if this is happening too much
        Timber.d("Acl listener registered, %d times", newValue);
        aclListenerRegistered.set(true);
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        // if we are receiving, then we are registered, can happen when added from manifest
        if(!aclListenerRegistered.getAndSet(true)) {
            timesRegistered.incrementAndGet();
        }
        String action = intent.getAction();
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        Timber.i("BT change received !");
        if (device == null) {
            Timber.d("BT Device is null");
            return;
        }
        FitbitBluetoothDevice fbDevice = new FitbitBluetoothDevice(device);
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            Timber.i("%s Device found", fbDevice.getName());
            // device was discovered in scan but we don't necessarily want to just add it unless
            // it's connected to the phone
        }
        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            Timber.i("%s Device is now connected", fbDevice.getName());
            // device was connected to the phone, does not mean device is connected to the
            // app, this can create a new connection because we are certain that the connection
            // is not in the cache already
            if (!FitbitGatt.getInstance().isDeviceInConnections(fbDevice)) {
                FitbitGatt.getInstance().putConnectionIntoDevices(fbDevice, new GattConnection(fbDevice, context.getMainLooper()));
            }
        }
        if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            Timber.i("%s Device is disconnected", fbDevice.getName());
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
                    Timber.i("%s Notifying listeners of connection disconnected", fbDevice.getName());
                    if (FitbitGatt.getInstance().getPeripheralScanner() != null) {
                        FitbitGatt.getInstance().getPeripheralScanner().onDeviceDisconnected(device);
                    }
                    FitbitGatt.getInstance().notifyListenersOfConnectionDisconnected(gattConnection);
                });
            }
        }
    }

    synchronized void unregister(Context context) {
        if(aclListenerRegistered.get()) {
            int newValue = timesRegistered.decrementAndGet();
            Timber.d("Unregistered receiver, new times registered value, %d", newValue);
            try {
                context.unregisterReceiver(this);
            }catch(IllegalArgumentException ex) {
                Timber.d(ex,"It's telling us we didn't register this");
            }
            aclListenerRegistered.set(false);
        }
    }
}
