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
import com.fitbit.bluetooth.fbgatt.util.BluetoothUtils;

import android.bluetooth.BluetoothGattServer;

import timber.log.Timber;

/**
 * Will close out the existing gatt server reference.  Typically this shouldn't be necessary
 * but on some phones where BT off does not clear out services, this may help.  When BT is
 * turned back on, openGattServer will be called and the callbacks will be relinked.
 *
 * Only use this transaction if you know that you need to do so, and if you do, only use it
 * once BT is off, and probably after clearing services.  Be aware that removing services may
 * trigger a services changed notification on the remote peripheral.
 *
 * Created by iowens on 6/12/19.
 */

public class CloseGattServerTransaction extends GattServerTransaction {
    private static final String NAME = "CloseGattServerTransaction";
    public CloseGattServerTransaction(GattServerConnection server, GattState successEndState) {
        super(server, successEndState);
    }

    public CloseGattServerTransaction(GattServerConnection server, GattState successEndState, long timeoutMillis) {
        super(server, successEndState, timeoutMillis);
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        super.transaction(callback);
        getGattServer().setState(GattState.CLOSING_GATT_SERVER);
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
        BluetoothGattServer server = getGattServer().getServer();
        if(server == null) {
            Timber.w("The gatt server was null");
            getGattServer().setState(GattState.CLOSE_GATT_SERVER_FAILURE);
            builder.gattState(getGattServer().getGattState())
                .resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
            mainThreadHandler.post(() -> {
                callCallbackWithTransactionResultAndRelease(callback, builder.build());
                getGattServer().setState(GattState.IDLE);
            });
        } else {
            BluetoothUtils bluetoothUtils = new BluetoothUtils();
            if(bluetoothUtils.isBluetoothEnabled(FitbitGatt.getInstance().getAppContext())) {
                Timber.e("I hope you know what you are doing, you will not be able to operate on the gatt server again until BT is toggled.");
            }
            try {
                server.close();
                getGattServer().setState(GattState.CLOSE_GATT_SERVER_SUCCESS);
                builder.gattState(getGattServer().getGattState())
                    .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
                mainThreadHandler.post(() -> {
                    callCallbackWithTransactionResultAndRelease(callback, builder.build());
                    getGattServer().setState(GattState.IDLE);
                });
            } catch (NullPointerException ex) {
                Timber.w("As the client close can sometimes NPE internally, it is reasonable to assume that a similar thing occurred for the server");
                getGattServer().setState(GattState.CLOSE_GATT_SERVER_FAILURE);
                builder.gattState(getGattServer().getGattState())
                    .resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
                mainThreadHandler.post(() -> {
                    callCallbackWithTransactionResultAndRelease(callback, builder.build());
                    getGattServer().setState(GattState.IDLE);
                });
            }
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}
