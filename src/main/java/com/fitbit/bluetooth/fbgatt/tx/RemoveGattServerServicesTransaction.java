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

import android.bluetooth.BluetoothGattService;

/**
 * Will remove a gatt server service from the server
 *
 * Created by iowens on 12/19/17.
 */

public class RemoveGattServerServicesTransaction extends GattServerTransaction {
    private static final String NAME = "RemoveGattServerServicesTransaction";
    private final BluetoothGattService service;

    public RemoveGattServerServicesTransaction(GattServerConnection server, GattState successEndState, BluetoothGattService service) {
        super(server, successEndState);
        this.service = service;
    }

    public RemoveGattServerServicesTransaction(GattServerConnection server, GattState successEndState, BluetoothGattService service, long timeoutMillis) {
        super(server, successEndState, timeoutMillis);
        this.service = service;
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        super.transaction(callback);
        getGattServer().setState(GattState.REMOVING_SERVER_SERVICE);
        boolean success = getGattServer().getServer().removeService(this.service);
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
        builder.serviceUuid(this.service.getUuid());
        if(success) {
            getGattServer().setState(GattState.REMOVE_SERVER_SERVICE_SUCCESS);
            builder.resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
        } else {
            getGattServer().setState(GattState.REMOVE_SERVER_SERVICE_FAILURE);
            builder.resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
        }
        builder.gattState(getGattServer().getGattState());
        mainThreadHandler.post(() -> {
            callCallbackWithTransactionResultAndRelease(callback, builder.build());
            getGattServer().setState(GattState.IDLE);
        });
    }

    @Override
    public String getName() {
        return NAME;
    }
}
