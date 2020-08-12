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
 * Will read the present physical layer read from the client, may or may not be allowed, but either way
 * will leave the connection in a usable state
 */

public class ReadGattClientPhyTransaction extends GattClientTransaction {
    public static final String NAME = "ReadGattClientPhyTransaction";

    public ReadGattClientPhyTransaction(GattConnection connection, GattState successEndState) {
        super(connection, successEndState);
    }

    public ReadGattClientPhyTransaction(GattConnection connection, GattState successEndState, long timeoutMillis) {
        super(connection, successEndState, timeoutMillis);
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        super.transaction(callback);
        getConnection().setState(GattState.READING_CURRENT_PHY);
        BluetoothGatt localGatt = getConnection().getGatt();
        if(FitbitGatt.atLeastSDK(O)) {
            // of course this would have no boolean result, why would it follow the same pattern as
            // mtu or connection priority? :sigh:
            if(localGatt != null) {
                localGatt.readPhy();
            } else {
                Timber.w("Couldn't read the phy because gatt was null");
                TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
                getConnection().setState(GattState.READ_CURRENT_PHY_FAILURE);
                builder.resultStatus(TransactionResult.TransactionResultStatus.FAILURE)
                    .gattState(getConnection().getGattState());
                callCallbackWithTransactionResultAndRelease(callback, builder.build());
                // even if we fail to request the PHY then we can still use the connection
                getConnection().setState(GattState.IDLE);
            }
        } else {
            Timber.i("[%s] Can't read the PHY on this version of Android, no-op", getDevice());
            getConnection().setState(GattState.IDLE);
        }
    }

    @Override
    public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
        Timber.i("[%s] The actual txPhy: %d, rxPhy: %d", getDevice(), txPhy, rxPhy);
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
        builder.responseStatus(GattDisconnectReason.getReasonForCode(status).ordinal());
        if(status == BluetoothGatt.GATT_SUCCESS) {
            getConnection().setState(GattState.READ_CURRENT_PHY_SUCCESS);
            builder.resultStatus(TransactionResult.TransactionResultStatus.SUCCESS)
                    .gattState(getConnection().getGattState())
                    .rxPhy(rxPhy)
                    .txPhy(txPhy);
            callCallbackWithTransactionResultAndRelease(callback, builder.build());
            getConnection().setState(GattState.IDLE);
        } else {
            getConnection().setState(GattState.READ_CURRENT_PHY_FAILURE);
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
