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

/**
 * Will clear the services on the local gatt server
 *
 * Created by iowens on 12/19/17.
 */

public class ClearServerServicesTransaction extends GattServerTransaction {
    private static final String NAME = "ClearServerServicesTransaction";
    public ClearServerServicesTransaction(GattServerConnection server, GattState successEndState) {
        super(server, successEndState);
    }

    public ClearServerServicesTransaction(GattServerConnection server, GattState successEndState, long timeoutMillis) {
        super(server, successEndState, timeoutMillis);
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        super.transaction(callback);
        getGattServer().setState(GattState.CLEARING_GATT_SERVER_SERVICES);
        getGattServer().getServer().clearServices();
        getGattServer().setState(GattState.CLEAR_GATT_SERVER_SERVICES_SUCCESS);
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
        builder.gattState(getGattServer().getGattState())
                .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
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
