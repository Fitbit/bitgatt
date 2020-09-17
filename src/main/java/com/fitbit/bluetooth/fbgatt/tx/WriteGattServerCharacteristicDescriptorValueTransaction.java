/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx;

import com.fitbit.bluetooth.fbgatt.GattServerConnection;
import com.fitbit.bluetooth.fbgatt.GattServerTransaction;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.Situation;
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.strategies.Strategy;
import com.fitbit.bluetooth.fbgatt.util.GattStatus;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import androidx.annotation.Nullable;

import timber.log.Timber;

/**
 * Will write a characteristic descriptor from a local gatt server and populate a transaction result with
 * response.  This and {@link WriteGattServerCharacteristicValueTransaction} are sort of conveniences
 * for testing, there is no clear reason why one wouldn't perform these operations in Java, they have
 * no impact to the state machine.  But there is no harm in mainstream code using these transactions.
 *
 * Created by iowens on 12/4/17.
 */

public class WriteGattServerCharacteristicDescriptorValueTransaction extends GattServerTransaction {

    private static final String TAG = "WriteGattServerCharacteristicDescriptorTransaction";

    private BluetoothGattCharacteristic characteristic;
    private BluetoothGattService service;
    private BluetoothGattDescriptor descriptor;
    private byte[] data;

    public WriteGattServerCharacteristicDescriptorValueTransaction(@Nullable GattServerConnection connection, GattState successEndState, BluetoothGattService service, BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor descriptor, byte[] data) {
        super(connection, successEndState);
        this.descriptor = descriptor;
        this.characteristic = characteristic;
        this.service = service;
        this.data = data;
    }

    public WriteGattServerCharacteristicDescriptorValueTransaction(@Nullable GattServerConnection connection, GattState successEndState, BluetoothGattService service, BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor descriptor, byte[] data, long timeoutMillis) {
        super(connection, successEndState, timeoutMillis);
        this.descriptor = descriptor;
        this.characteristic = characteristic;
        this.service = service;
        this.data = data;
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        super.transaction(callback);
        getGattServer().setState(GattState.WRITING_DESCRIPTOR);
        BluetoothGattCharacteristic localCharacteristic = service.getCharacteristic(this.characteristic.getUuid());
        if(localCharacteristic == null) {
            respondWithError(null, null, callback);
            return;
        }
        BluetoothGattDescriptor localDescriptor = localCharacteristic.getDescriptor(this.descriptor.getUuid());
        if(localDescriptor == null) {
            respondWithError(null, null, callback);
            return;
        }
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
        try {
            if (localDescriptor.setValue(data)) {
                // success
                builder.responseStatus(GattStatus.GATT_SUCCESS.ordinal());
                getGattServer().setState(GattState.WRITE_DESCRIPTOR_SUCCESS);
                builder.data(localCharacteristic.getValue())
                        .gattState(getGattServer().getGattState())
                        .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS)
                        .serviceUuid(service.getUuid())
                        .characteristicUuid(localCharacteristic.getUuid())
                        .descriptorUuid(localDescriptor.getUuid());
                callCallbackWithTransactionResultAndRelease(callback, builder.build());
                getGattServer().setState(GattState.IDLE);
            } else {
                // failure
                respondWithError(localCharacteristic, localDescriptor, callback);
            }
        } catch (NullPointerException ex) {
            Timber.w(ex,"[%s] We are going to fail this tx due to the stack NPE, this is probably poor peripheral behavior, this should become a FW bug.", getDevice());
            respondWithError(localCharacteristic, localDescriptor, callback);
            Strategy strategy = strategyProvider.
                    getStrategyForPhoneAndGattConnection(null, null,
                            Situation.TRACKER_WENT_AWAY_DURING_GATT_OPERATION);
            if (strategy != null) {
                strategy.applyStrategy();
            }
        }
    }

    private void respondWithError(@Nullable BluetoothGattCharacteristic characteristic, @Nullable BluetoothGattDescriptor descriptor, GattTransactionCallback callback) {
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
        builder.responseStatus(GattStatus.GATT_ERROR.ordinal());
        getGattServer().setState(GattState.WRITE_DESCRIPTOR_FAILURE);
        builder.data(descriptor == null ? new byte[0] : descriptor.getValue())
                .gattState(getGattServer().getGattState())
                .resultStatus(TransactionResult.TransactionResultStatus.FAILURE)
                .serviceUuid(service.getUuid())
                .characteristicUuid(characteristic == null ? null : characteristic.getUuid())
                .descriptorUuid(descriptor == null ? null : descriptor.getUuid());
        callCallbackWithTransactionResultAndRelease(callback, builder.build());
        getGattServer().setState(GattState.IDLE);
    }

    @Override
    public String getName() {
        return TAG;
    }
}
