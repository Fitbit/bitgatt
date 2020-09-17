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
import com.fitbit.bluetooth.fbgatt.btcopies.BluetoothGattDescriptorCopy;
import com.fitbit.bluetooth.fbgatt.strategies.Strategy;
import com.fitbit.bluetooth.fbgatt.util.GattDisconnectReason;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattDescriptor;
import androidx.annotation.Nullable;

import timber.log.Timber;

/**
 * Will read a descriptor on a characteristic
 * <p>
 * Created by iowens on 12/6/17.
 */

public class ReadGattDescriptorTransaction extends GattClientTransaction {
    private static final String NAME = "ReadGattDescriptorTransaction";

    BluetoothGattDescriptor descriptor;

    public ReadGattDescriptorTransaction(@Nullable GattConnection connection, GattState successEndState, BluetoothGattDescriptor descriptor) {
        super(connection, successEndState);
        this.descriptor = descriptor;
    }

    public ReadGattDescriptorTransaction(@Nullable GattConnection connection, GattState successEndState, BluetoothGattDescriptor descriptor, long timeoutMillis) {
        super(connection, successEndState, timeoutMillis);
        this.descriptor = descriptor;
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        super.transaction(callback);
        getConnection().setState(GattState.READING_DESCRIPTOR);
        boolean success = false;
        BluetoothGatt localGatt = getConnection().getGatt();
        if(localGatt != null) {
            try {
                success = localGatt.readDescriptor(descriptor);
            } catch (NullPointerException ex) {
                Timber.w(ex, "[%s] We are going to fail this tx due to the stack NPE, this is probably poor peripheral behavior, this should become a FW bug.", getDevice());
                if (getDevice() != null) {
                    Timber.w("[%s] btDevice %s characteristic %s", getDevice(), getDevice().getBtDevice(), this.descriptor.getUuid());
                }
                // Ensure that the flag is set to false, and that is is
                // impossible to be anything else stepping through after
                // this ... strategy time
                success = false;
            }
        } else {
            Timber.w("Couldn't read descriptor because gatt was null");
        }
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
        if (!success) {
            getConnection().setState(GattState.READ_DESCRIPTOR_FAILURE);
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
                if (strategy != null) {
                    strategy.applyStrategy();
                }
            });
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptorCopy descriptor, int status) {
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
        builder.responseStatus(GattDisconnectReason.getReasonForCode(status).ordinal());
        if(status == BluetoothGatt.GATT_SUCCESS) {
            getConnection().setState(GattState.READ_DESCRIPTOR_SUCCESS);
            builder.resultStatus(TransactionResult.TransactionResultStatus.SUCCESS)
                    .gattState(getConnection().getGattState())
                    .rssi(getConnection().getDevice().getRssi())
                    .data(descriptor.getValue())
                    .descriptorUuid(descriptor.getUuid());
            callCallbackWithTransactionResultAndRelease(callback, builder.build());
            getConnection().setState(GattState.IDLE);
        } else {
            getConnection().setState(GattState.READ_DESCRIPTOR_FAILURE);
            builder.resultStatus(TransactionResult.TransactionResultStatus.FAILURE)
                    .gattState(getConnection().getGattState())
                    .data(descriptor.getValue())
                    .descriptorUuid(descriptor.getUuid());
            callCallbackWithTransactionResultAndRelease(callback, builder.build());
        }
    }
}