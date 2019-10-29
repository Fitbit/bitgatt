/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx;

import com.fitbit.bluetooth.fbgatt.FitbitGatt;
import com.fitbit.bluetooth.fbgatt.GattServerConnection;
import com.fitbit.bluetooth.fbgatt.GattServerTransaction;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.util.GattDisconnectReason;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;

import timber.log.Timber;

/**
 * Will perform a transaction adding a service to the gatt server.  The caller should check first
 * as to whether the service exists before adding this service or the result will be a failure.  Keep
 * in mind that another process or application can manipulate the services and characteristics on
 * the phone in different ways, so it is always prudent to check and ensure that the state of the
 * characteristics, descriptors, and services are to your liking before proceeding.
 *
 * Created by iowens on 11/15/17.
 */

public class AddGattServerServiceTransaction extends GattServerTransaction {

    private static final String NAME = "AddGattServerServiceTransaction";

    private BluetoothGattService service;

    public AddGattServerServiceTransaction(GattServerConnection server, GattState successEndState, BluetoothGattService service) {
        super(server, successEndState);
        this.service = service;
    }

    public AddGattServerServiceTransaction(GattServerConnection server, GattState successEndState, BluetoothGattService service, long timeoutMillis) {
        super(server, successEndState, timeoutMillis);
        this.service = service;
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        super.transaction(callback);
        getGattServer().setState(GattState.ADDING_SERVICE);
        if(getGattServer().getServer() == null) {
            mainThreadHandler.post(() -> {
                TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
                Timber.w("The GATT Server was not started yet, did you start the gatt?");
                builder.responseStatus(GattDisconnectReason.getReasonForCode(GattDisconnectReason.GATT_CONN_NO_RESOURCES.getCode()).ordinal());
                getGattServer().setState(GattState.ADD_SERVICE_FAILURE);
                Timber.e("The gatt service could not be added: %s", service.getUuid());
                builder.gattState(getGattServer().getGattState())
                        .serviceUuid(service.getUuid())
                        .resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
                callCallbackWithTransactionResultAndRelease(callback, builder.build());
            });
        } else {
            if(doesGattServerServiceAlreadyExist(service)) {
                TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
                builder.responseStatus(GattDisconnectReason.getReasonForCode(GattDisconnectReason.GATT_CONN_NO_RESOURCES.getCode()).ordinal());
                getGattServer().setState(GattState.ADD_SERVICE_FAILURE);
                Timber.w("The gatt service %s, is a duplicate, and could not be added to server", service.getUuid());
                builder.gattState(getGattServer().getGattState())
                        .serviceUuid(service.getUuid())
                        .resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
                callCallbackWithTransactionResultAndRelease(callback, builder.build());
                getGattServer().setState(GattState.IDLE);
                return;
            }
            boolean success = false;
            try {
                success = getGattServer().getServer().addService(service);
            } catch ( NullPointerException npe ) {
                // this is likely due to several reasons, but mostly that the bluetooth service was gone while adding
                // no reason to make a non-fatal here
                Timber.i(npe, "There was an internal android stack null pointer exception adding the service, failing");
                // success still false
            }
            if(!success) {
                TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
                builder.responseStatus(GattDisconnectReason.getReasonForCode(GattDisconnectReason.GATT_CONN_NO_RESOURCES.getCode()).ordinal());
                getGattServer().setState(GattState.ADD_SERVICE_FAILURE);
                Timber.w("The gatt service %s, failed to be added at the gatt level, try again later.", service.getUuid());
                builder.gattState(getGattServer().getGattState())
                    .serviceUuid(service.getUuid())
                    .resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
                callCallbackWithTransactionResultAndRelease(callback, builder.build());
                getGattServer().setState(GattState.IDLE);
            }
        }
    }

    @Override
    public void onServerServiceAdded(int status, BluetoothGattService service) {
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
        builder.responseStatus(GattDisconnectReason.getReasonForCode(status).ordinal());
        if (status == BluetoothGatt.GATT_SUCCESS) {
            getGattServer().setState(GattState.ADD_SERVICE_SUCCESS);
            Timber.v("Gatt service was added to the gatt server successfully");
            builder.gattState(getGattServer().getGattState())
                    .serviceUuid(service.getUuid())
                    .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
            callCallbackWithTransactionResultAndRelease(callback, builder.build());
            getGattServer().setState(GattState.IDLE);
        } else {
            getGattServer().setState(GattState.ADD_SERVICE_FAILURE);
            Timber.e("The gatt service could not be added: %s", service.getUuid());
            builder.gattState(getGattServer().getGattState())
                    .serviceUuid(service.getUuid())
                    .resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
            callCallbackWithTransactionResultAndRelease(callback, builder.build());
            getGattServer().setState(GattState.IDLE);
        }
    }

    /**
     * True if the gatt server service already exists, false if it doesn't
     * @param service The bluetooth service that is trying to add
     * @return true if the service is already present, false if it is not
     */

    private boolean doesGattServerServiceAlreadyExist(BluetoothGattService service) {
        return FitbitGatt.getInstance().getServer().getServer().getService(service.getUuid()) != null;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
