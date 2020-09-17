/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx;

import com.fitbit.bluetooth.fbgatt.FitbitGatt;
import com.fitbit.bluetooth.fbgatt.GattClientTransaction;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransaction;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.util.GattDisconnectReason;

import android.bluetooth.BluetoothGatt;

import timber.log.Timber;

import static android.os.Build.VERSION_CODES.O;

/**
 * Will request a physical layer change from the client, may or may not be allowed, but either way
 * will leave the connection in a usable state
 */

public class RequestGattClientPhyChangeTransaction extends GattClientTransaction {
    public static final String NAME = "RequestGattClientPhyChangeTransaction";

    private int txPhy;
    private int rxPhy;
    private int phyOptions;

    public RequestGattClientPhyChangeTransaction(GattConnection connection, GattState successEndState, int txPhy, int rxPhy, int phyOptions) {
        super(connection, successEndState);
        this.txPhy = txPhy;
        this.rxPhy = rxPhy;
        this.phyOptions = phyOptions;
    }

    public RequestGattClientPhyChangeTransaction(GattConnection connection, GattState successEndState, int txPhy, int rxPhy, int phyOptions, long timeoutMillis) {
        super(connection, successEndState, timeoutMillis);
        this.txPhy = txPhy;
        this.rxPhy = rxPhy;
        this.phyOptions = phyOptions;
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        super.transaction(callback);
        getConnection().setState(GattState.REQUESTING_PHY_CHANGE);
        BluetoothGatt localGatt = getConnection().getGatt();
        if(FitbitGatt.atLeastSDK(O)) {
            // of course this would have no boolean result, why would it follow the same pattern as
            // mtu or connection priority? :sigh:
            if(localGatt != null) {
                localGatt.setPreferredPhy(txPhy, rxPhy, phyOptions);
            } else {
                Timber.w("Couldn't set the phy because gatt was null");
                TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
                getConnection().setState(GattState.REQUEST_PHY_CHANGE_FAILURE);
                builder.resultStatus(TransactionResult.TransactionResultStatus.FAILURE)
                    .gattState(getConnection().getGattState());
                callCallbackWithTransactionResultAndRelease(callback, builder.build());
                // even if we fail to request the PHY then we can still use the connection
                getConnection().setState(GattState.IDLE);
            }
        } else {
            Timber.i("[%s] Can't change the PHY on this version of Android, no-op", getDevice());
            getConnection().setState(GattState.IDLE);
        }
    }

    @Override
    public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
        builder.responseStatus(GattDisconnectReason.getReasonForCode(status).ordinal());
        if(status == BluetoothGatt.GATT_SUCCESS) {
            getConnection().setState(GattState.REQUEST_PHY_CHANGE_SUCCESS);
            builder.resultStatus(TransactionResult.TransactionResultStatus.SUCCESS)
                    .gattState(getConnection().getGattState())
                    .rxPhy(rxPhy)
                    .txPhy(txPhy);
            callCallbackWithTransactionResultAndRelease(callback, builder.build());
            getConnection().setState(GattState.IDLE);
        } else {
            getConnection().setState(GattState.REQUEST_PHY_CHANGE_FAILURE);
            builder.resultStatus(TransactionResult.TransactionResultStatus.FAILURE)
                    .gattState(getConnection().getGattState())
                    .txPhy(txPhy)
                    .rxPhy(rxPhy);
            callCallbackWithTransactionResultAndRelease(callback, builder.build());
            // even if we fail to request the PHY then we can still use the connection
            getConnection().setState(GattState.IDLE);
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}
