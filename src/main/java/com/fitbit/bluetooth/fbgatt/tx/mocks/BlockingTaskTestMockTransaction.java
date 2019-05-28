/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx.mocks;

import android.bluetooth.BluetoothGattCharacteristic;

import com.fitbit.bluetooth.fbgatt.FitbitGatt;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransaction;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.util.GattUtils;

import java.util.Objects;

import timber.log.Timber;

public class BlockingTaskTestMockTransaction extends GattTransaction {

    private static final String NAME = "BlockingTaskTestMockTransaction";
    private static final long OVERRIDE_TIMEOUT = 1000L;
    private static long startTime;

    public BlockingTaskTestMockTransaction(GattConnection connection, GattState destinationState, BluetoothGattCharacteristic characteristic) {
        super(connection, destinationState);
        Timber.v("Characteristic uuid : %s", characteristic.getUuid().toString());
        setTimeout(OVERRIDE_TIMEOUT);
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        super.transaction(callback);
        // let's do something useful with the adapter
        GattUtils utils = new GattUtils();
        boolean is2MsymSupported = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            is2MsymSupported = Objects.requireNonNull(utils.getBluetoothAdapter(FitbitGatt.getInstance().getAppContext())).isLe2MPhySupported();
        }
        Timber.i("2 msym is supported? %b", is2MsymSupported);
        startTime = System.currentTimeMillis();
        // we will wait here and let the tx timeout
        synchronized (NAME) {
            try {
                NAME.wait(4000);
            } catch (InterruptedException e) {
                Timber.e(e, "This should fail whatever test, because this is supposed to be blocking");
                callCallbackWithTransactionResultAndRelease(callback, new TransactionResult.Builder().transactionName(NAME).resultStatus(TransactionResult.TransactionResultStatus.FAILURE).build());
            }
        }
    }

    @Override
    protected void onGattClientTransactionTimeout(GattConnection connection) {
        // when the timeout has elapsed we will release the tx execution thread
        Timber.i("Handling timeout, unlocking thread");
        synchronized (NAME) {
            NAME.notify();
        }
        long finishTime = System.currentTimeMillis();
        Timber.i("Elasped time %dms", finishTime - startTime);
    }

    @Override
    public String getName(){
        return NAME;
    }
}
