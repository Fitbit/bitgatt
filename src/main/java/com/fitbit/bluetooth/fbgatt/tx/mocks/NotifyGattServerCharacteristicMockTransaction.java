/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx.mocks;

import com.fitbit.bluetooth.fbgatt.FitbitBluetoothDevice;
import com.fitbit.bluetooth.fbgatt.GattServerConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.Situation;
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.strategies.Strategy;
import com.fitbit.bluetooth.fbgatt.tx.NotifyGattServerCharacteristicTransaction;
import com.fitbit.bluetooth.fbgatt.util.GattDisconnectReason;
import com.fitbit.bluetooth.fbgatt.util.GattStatus;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;

/**
 * Notify gatt server characteristic mock class
 *
 * Created by iowens on 12/17/17.
 */

public class NotifyGattServerCharacteristicMockTransaction extends NotifyGattServerCharacteristicTransaction {
    private static final int REASONABLE_AMOUNT_OF_TIME_FOR_NOTIFY = 250;
    private boolean shouldFail = false;
    private final Handler mainHandler;
    private boolean shouldThrow;

    public NotifyGattServerCharacteristicMockTransaction(GattServerConnection connection, FitbitBluetoothDevice device, GattState successEndState, BluetoothGattCharacteristic characteristic, boolean confirm, boolean shouldFail) {
        super(connection, device, successEndState, characteristic, confirm);
        this.shouldFail = shouldFail;
        this.mainHandler = getGattServer().getMainHandler();
    }

    public void setShouldThrow(boolean shouldThrow) {
        this.shouldThrow = shouldThrow;
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        this.callback = callback;
        getGattServer().setState(GattState.NOTIFYING_CHARACTERISTIC);
        mainHandler.postDelayed(() -> {
            if(shouldFail) {
                if(shouldThrow) {
                    // we want to apply this strategy to every phone, so we will provide an empty target android
                    // device
                    TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
                    builder.responseStatus(GattDisconnectReason.getReasonForCode(GattStatus.GATT_INTERNAL_ERROR.getCode()).ordinal());
                    if(this.characteristic != null) {
                        builder.characteristicUuid(this.characteristic.getUuid());
                        builder.data(this.characteristic.getValue());
                    }
                    getGattServer().setState(GattState.NOTIFY_CHARACTERISTIC_FAILURE);
                    builder.resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
                    getGattServer().setState(GattState.IDLE);
                    Strategy strategy = strategyProvider.
                            getStrategyForPhoneAndGattConnection(null, null,
                                    Situation.TRACKER_WENT_AWAY_DURING_GATT_OPERATION);
                    if(strategy != null) {
                        strategy.applyStrategy();
                    }
                    callCallbackWithTransactionResultAndRelease(callback, builder.build());
                } else {
                    onServerNotificationSent(null, GattStatus.GATT_INTERNAL_ERROR.ordinal());
                }
            } else {
                onServerNotificationSent(null, BluetoothGatt.GATT_SUCCESS);
            }
        }, REASONABLE_AMOUNT_OF_TIME_FOR_NOTIFY);
    }
}
