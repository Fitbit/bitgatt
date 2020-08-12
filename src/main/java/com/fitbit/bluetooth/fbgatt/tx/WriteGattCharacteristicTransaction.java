/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx;

import com.fitbit.bluetooth.fbgatt.GattClientTransaction;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransaction;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.Situation;
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.btcopies.BluetoothGattCharacteristicCopy;
import com.fitbit.bluetooth.fbgatt.strategies.Strategy;
import com.fitbit.bluetooth.fbgatt.util.GattDisconnectReason;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import androidx.annotation.Nullable;

import timber.log.Timber;

/**
 * Will write a characteristic from a remote device and populate a transaction result with
 * response
 * <p>
 * Created by iowens on 12/4/17.
 */

public class WriteGattCharacteristicTransaction extends GattClientTransaction {

    private static final String TAG = "WriteCharacteristicTransaction";

    private BluetoothGattCharacteristic characteristic;

    public WriteGattCharacteristicTransaction(@Nullable GattConnection connection, GattState successEndState, BluetoothGattCharacteristic characteristic) {
        super(connection, successEndState);
        this.characteristic = characteristic;
    }

    public WriteGattCharacteristicTransaction(@Nullable GattConnection connection, GattState successEndState, BluetoothGattCharacteristic characteristic, long timeoutMillis) {
        super(connection, successEndState, timeoutMillis);
        this.characteristic = characteristic;
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        super.transaction(callback);
        getConnection().setState(GattState.WRITING_CHARACTERISTIC);
        boolean success = false;
        BluetoothGatt localGatt = getConnection().getGatt();
        if(localGatt != null) {
            try {
                success = localGatt.writeCharacteristic(characteristic);
            } catch (NullPointerException ex) {
                Timber.w(ex, "[%s] We are going to fail this tx due to the stack NPE, this is probably poor peripheral behavior, this should become a FW bug.", getDevice());
                if (getDevice() != null) {
                    Timber.w("[%s] btDevice %s characteristic %s", getDevice(), getDevice().getBtDevice(), this.characteristic.getUuid());
                }
                // Ensure that the flag is set to false, and that is is
                // impossible to be anything else stepping through after
                // this ... strategy time
                success = false;
            }
        } else {
            Timber.w("Can't write characteristic because gatt was null");
        }
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
        if(!success) {
            getConnection().setState(GattState.WRITE_CHARACTERISTIC_FAILURE);
            builder.resultStatus(TransactionResult.TransactionResultStatus.FAILURE)
                    .gattState(getConnection().getGattState());
            mainThreadHandler.post(() -> {
                callCallbackWithTransactionResultAndRelease(callback, builder.build());
                getConnection().setState(GattState.IDLE);
                // we want to apply this strategy to every phone, so we will provide an empty target android
                // device
                Strategy strategy = strategyProvider.
                        getStrategyForPhoneAndGattConnection(null, getConnection(),
                                Situation.TRACKER_WENT_AWAY_DURING_GATT_OPERATION);
                if(strategy != null) {
                    strategy.applyStrategy();
                }
            });
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristicCopy characteristic, int status) {
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
        builder.responseStatus(GattDisconnectReason.getReasonForCode(status).ordinal());
        if (status == BluetoothGatt.GATT_SUCCESS) {
            getConnection().setState(GattState.WRITE_CHARACTERISTIC_SUCCESS);
            builder.data(characteristic.getValue())
                    .rssi(getConnection().getDevice().getRssi())
                    .gattState(getConnection().getGattState())
                    .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
            callCallbackWithTransactionResultAndRelease(callback, builder.build());
            getConnection().setState(GattState.IDLE);
        } else {
            getConnection().setState(GattState.WRITE_CHARACTERISTIC_FAILURE);
            builder.data(characteristic.getValue())
                    .gattState(getConnection().getGattState())
                    .resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
            callCallbackWithTransactionResultAndRelease(callback, builder.build());
        }
    }

    @Override
    public String getName() {
        return TAG;
    }
}
