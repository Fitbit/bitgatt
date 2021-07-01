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
import java.util.List;

/**
 * A transaction to get the gatt server services hosted
 * by the local gatt server.  This exists primarily to prevent
 * the internal CME that can occur if we are reading services
 * and adding, by making this a transaction a caller should not
 * read and add at the same time.
 *
 * Created by iowens on 12/19/17.
 */

public class GetGattServerServicesTransaction extends GattServerTransaction {
    private static final String NAME = "GetServerServicesTransaction";

    public GetGattServerServicesTransaction(GattServerConnection server, GattState successEndState) {
        super(server, successEndState);
    }

    public GetGattServerServicesTransaction(GattServerConnection server, GattState successEndState, long timeoutMillis) {
        super(server, successEndState, timeoutMillis);
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        super.transaction(callback);
        getGattServer().setState(GattState.GETTING_SERVER_SERVICES);
        List<BluetoothGattService> gattServices = getGattServer().getServer().getServices();
        getGattServer().setState(GattState.GET_SERVER_SERVICES_SUCCESS);
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
        builder.serverServices(gattServices)
                .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS)
                .gattState(getGattServer().getGattState());
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
