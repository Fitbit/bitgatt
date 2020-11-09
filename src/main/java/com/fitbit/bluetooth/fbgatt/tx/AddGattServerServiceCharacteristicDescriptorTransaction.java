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
import com.fitbit.bluetooth.fbgatt.util.GattDisconnectReason;
import com.fitbit.bluetooth.fbgatt.util.GattStatus;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import timber.log.Timber;

/**
 * Will add a gatt server service characteristic descriptor to the local gatt server.  The caller should check
 * before adding for the existence of this descriptor on the provided characteristic or the result
 * will be a failure
 *
 * Created by iowens on 8/20/18.
 */
public class AddGattServerServiceCharacteristicDescriptorTransaction extends GattServerTransaction {
    private static final String NAME = "AddGattServerServiceCharacteristicTransaction";

    private BluetoothGattService service;
    private BluetoothGattCharacteristic characteristic;
    private BluetoothGattDescriptor descriptor;

    public AddGattServerServiceCharacteristicDescriptorTransaction(GattServerConnection server, GattState successEndState, BluetoothGattService service, BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor descriptor) {
        super(server, successEndState);
        this.service = service;
        this.characteristic = characteristic;
        this.descriptor = descriptor;
    }

    public AddGattServerServiceCharacteristicDescriptorTransaction(GattServerConnection server, GattState successEndState, BluetoothGattService service, BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor descriptor, long timeoutMillis) {
        super(server, successEndState, timeoutMillis);
        this.service = service;
        this.characteristic = characteristic;
        this.descriptor = descriptor;
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        super.transaction(callback);
        getGattServer().setState(GattState.ADDING_SERVICE_CHARACTERISTIC_DESCRIPTOR);
        if(getGattServer().getServer() == null) {
            mainThreadHandler.post(() -> {
                respondWithError("The GATT Server was not started yet, did you start the gatt?", callback);
            });
        } else {
            BluetoothGattService serverService = getGattServer().getServer().getService(service.getUuid());
            if(null != serverService){
                BluetoothGattCharacteristic serviceChar = serverService.getCharacteristic(characteristic.getUuid());
                if(null != serviceChar) {
                    if(doesDescriptorAlreadyExist(serviceChar, descriptor)) {
                        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
                        builder.disconnectReason(GattDisconnectReason.GATT_CONN_NO_RESOURCES);
                        getGattServer().setState(GattState.ADD_SERVICE_CHARACTERISTIC_DESCRIPTOR_FAILURE);
                        Timber.w("The gatt service characteristic descriptor %s, is a duplicate, and could not be added to characteristic: %s, on service: %s",
                                serviceChar.getUuid(), descriptor.getUuid(), service.getUuid());
                        builder.gattState(getGattServer().getGattState())
                                .serviceUuid(service.getUuid())
                                .characteristicUuid(characteristic.getUuid())
                                .descriptorUuid(descriptor.getUuid())
                                .resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
                        callCallbackWithTransactionResultAndRelease(callback, builder.build());
                        getGattServer().setState(GattState.IDLE);
                        return;
                    }
                    if (serviceChar.addDescriptor(descriptor)) {
                        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
                        builder.serviceUuid(serverService.getUuid())
                                .characteristicUuid(characteristic.getUuid())
                                .descriptorUuid(descriptor.getUuid());
                        builder.responseStatus(GattStatus.GATT_SUCCESS);
                        getGattServer().setState(GattState.ADD_SERVICE_CHARACTERISTIC_DESCRIPTOR_SUCCESS);
                        Timber.e("The gatt service characteristic descriptor could not be added: %s", service.getUuid());
                        builder.gattState(getGattServer().getGattState())
                                .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
                        callCallbackWithTransactionResultAndRelease(callback, builder.build());
                        getGattServer().setState(GattState.IDLE);
                    } else {
                        respondWithError("Couldn't add the descriptor to the local gatt characteristic", callback);
                    }
                } else {
                    respondWithError("The GATT Server service was not hosting the characteristic.", callback);
                }
            } else {
                respondWithError("The GATT Server was not hosting the service.", callback);
            }
        }
    }

    private void respondWithError(String message, GattTransactionCallback callback) {
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
        Timber.w(message);
        builder.disconnectReason(GattDisconnectReason.GATT_CONN_NO_RESOURCES);
        getGattServer().setState(GattState.ADD_SERVICE_CHARACTERISTIC_DESCRIPTOR_FAILURE);
        Timber.e("The gatt service characteristic descriptor could not be added: %s", service.getUuid());
        builder.gattState(getGattServer().getGattState())
                .serviceUuid(service.getUuid())
                .characteristicUuid(characteristic.getUuid())
                .descriptorUuid(descriptor.getUuid())
                .resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
        callCallbackWithTransactionResultAndRelease(callback, builder.build());
        getGattServer().setState(GattState.IDLE);
    }

    /**
     * Will return true if the descriptor already exists on the gatt server, false otherwise
     * @param characteristic The existing characteristic on the service
     * @param descriptor The descriptor that we are trying to add
     * @return True if the descriptor exists, false if it does not
     */

    private boolean doesDescriptorAlreadyExist(BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor descriptor) {
        return characteristic.getDescriptor(descriptor.getUuid()) != null;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
