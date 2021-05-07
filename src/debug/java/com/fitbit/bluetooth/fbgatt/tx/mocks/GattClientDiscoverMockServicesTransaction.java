/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx.mocks;

import android.bluetooth.BluetoothGattService;
import android.os.Handler;
import androidx.annotation.Nullable;

import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.tx.GattClientDiscoverServicesTransaction;
import com.fitbit.bluetooth.fbgatt.util.GattStatus;

import java.util.List;

/**
 * Mock class to simulate service discovery
 *
 * Created by iowens on 12/19/17.
 */

public class GattClientDiscoverMockServicesTransaction extends GattClientDiscoverServicesTransaction {
    private static final long REASONABLE_AMOUNT_OF_TIME_FOR_DISCONNECT = 250;
    private final Handler mainHandler;
    private final boolean shouldFail;
    private final List<BluetoothGattService> services;
    public GattClientDiscoverMockServicesTransaction(@Nullable GattConnection connection, GattState successEndState, List<BluetoothGattService> services, boolean shouldFail) {
        super(connection, successEndState);
        this.services = services;
        this.shouldFail = shouldFail;
        this.mainHandler = getConnection().getMainHandler();
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        this.callback = callback;
        getConnection().setState(GattState.DISCOVERING);
        this.mainHandler.postDelayed(() ->{
            TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
            if(shouldFail) {
                getConnection().setState(GattState.DISCOVERY_FAILURE);
                builder.responseStatus(GattStatus.GATT_ERROR.ordinal());
                builder.resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
                builder.gattState(getConnection().getGattState());
                callCallbackWithTransactionResultAndRelease(callback, builder.build());
            } else {
                getConnection().setState(GattState.DISCOVERY_FAILURE);
                builder.responseStatus(GattStatus.GATT_SUCCESS.ordinal());
                builder.resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
                builder.serverServices(this.services);
                builder.gattState(getConnection().getGattState());
                callCallbackWithTransactionResultAndRelease(callback, builder.build());
                getConnection().setState(GattState.IDLE);
            }
        }, REASONABLE_AMOUNT_OF_TIME_FOR_DISCONNECT);
    }
}
