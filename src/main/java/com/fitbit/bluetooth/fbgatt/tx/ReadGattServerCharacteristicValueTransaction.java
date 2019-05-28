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
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.util.GattStatus;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.Nullable;

/**
 * Will read a characteristic from a local gatt server and populate a transaction result with
 * response
 *
 * Created by iowens on 12/4/17.
 */

public class ReadGattServerCharacteristicValueTransaction extends GattServerTransaction {

    private static final String TAG = "ReadGattServerCharacteristicValueTransaction";

    private BluetoothGattCharacteristic characteristic;
    private BluetoothGattService service;

    public ReadGattServerCharacteristicValueTransaction(@Nullable GattServerConnection connection, GattState successEndState, BluetoothGattService service, BluetoothGattCharacteristic characteristic) {
        super(connection, successEndState);
        this.characteristic = characteristic;
        this.service = service;
    }

    public ReadGattServerCharacteristicValueTransaction(@Nullable GattServerConnection connection, GattState successEndState, BluetoothGattService service, BluetoothGattCharacteristic characteristic, long timeoutMillis) {
        super(connection, successEndState, timeoutMillis);
        this.characteristic = characteristic;
        this.service = service;
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        super.transaction(callback);
        getGattServer().setState(GattState.READING_CHARACTERISTIC);
        BluetoothGattCharacteristic localCharacteristic = service.getCharacteristic(this.characteristic.getUuid());
        if(localCharacteristic == null) {
            respondWithError(null, callback);
            return;
        }
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
        byte[] value = localCharacteristic.getValue();
        if(null != value) {
            // success
            builder.responseStatus(GattStatus.GATT_SUCCESS.ordinal());
                getGattServer().setState(GattState.READ_CHARACTERISTIC_SUCCESS);
                builder.data(value)
                        .gattState(getGattServer().getGattState())
                        .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS)
                        .serviceUuid(service.getUuid())
                        .characteristicUuid(localCharacteristic.getUuid());
            callCallbackWithTransactionResultAndRelease(callback, builder.build());
            getGattServer().setState(GattState.IDLE);
        } else {
            // failure
            respondWithError(localCharacteristic, callback);
        }
    }

    private void respondWithError(@Nullable BluetoothGattCharacteristic characteristic, GattTransactionCallback callback) {
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
        builder.responseStatus(GattStatus.GATT_ERROR.ordinal());
        getGattServer().setState(GattState.READ_CHARACTERISTIC_FAILURE);
        builder.data(characteristic == null ? new byte[0] : characteristic.getValue())
                .gattState(getGattServer().getGattState())
                .resultStatus(TransactionResult.TransactionResultStatus.FAILURE)
                .serviceUuid(service.getUuid())
                .characteristicUuid(characteristic == null ? null : characteristic.getUuid());
        callCallbackWithTransactionResultAndRelease(callback, builder.build());
        getGattServer().setState(GattState.IDLE);
    }

    @Override
    public String getName() {
        return TAG;
    }
}
