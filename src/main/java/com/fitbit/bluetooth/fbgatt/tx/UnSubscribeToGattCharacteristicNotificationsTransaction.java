/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx;

import com.fitbit.bluetooth.fbgatt.AndroidDevice;
import com.fitbit.bluetooth.fbgatt.GattClientTransaction;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransaction;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.Situation;
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.strategies.Strategy;
import com.fitbit.bluetooth.fbgatt.util.GattDisconnectReason;
import com.fitbit.bluetooth.fbgatt.util.GattStatus;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import androidx.annotation.Nullable;

import timber.log.Timber;

/**
 * Will unsubscribe from notifications on a remote gatt characteristic
 * <p>
 * Created by iowens on 12/7/17.
 */

public class UnSubscribeToGattCharacteristicNotificationsTransaction extends GattClientTransaction {
    public static final String NAME = "UnSubscribeToGattCharacteristicNotificationsTransaction";
    private BluetoothGattCharacteristic characteristic;

    public UnSubscribeToGattCharacteristicNotificationsTransaction(@Nullable GattConnection connection, GattState successEndState, BluetoothGattCharacteristic characteristic) {
        super(connection, successEndState);
        this.characteristic = characteristic;
    }

    public UnSubscribeToGattCharacteristicNotificationsTransaction(@Nullable GattConnection connection, GattState successEndState, BluetoothGattCharacteristic characteristic, long timeoutMillis) {
        super(connection, successEndState, timeoutMillis);
        this.characteristic = characteristic;
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        super.transaction(callback);
        getConnection().setState(GattState.DISABLING_CHARACTERISTIC_NOTIFICATION);
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
        boolean success = false;
        BluetoothGatt localGatt = getConnection().getGatt();
        if(localGatt != null) {
            try {
                success = localGatt.setCharacteristicNotification(characteristic, false);
            } catch (NullPointerException ex) {
                Timber.w(ex, "[%s] We are going to fail this tx due to the stack NPE, this is probably poor peripheral behavior, this should become a FW bug.", getDevice());
                // we want to apply this strategy to every phone, so we will provide an empty target android
                // device
                Strategy strategy = strategyProvider.
                    getStrategyForPhoneAndGattConnection(null, getConnection(),
                        Situation.TRACKER_WENT_AWAY_DURING_GATT_OPERATION);
                if (strategy != null) {
                    strategy.applyStrategy();
                }
            }
        } else {
            Timber.w("Could not unsubscribe, the gatt was null");
        }
        if (success) {
            /*
             * this will need to be delayed in its response because if a developer chains a descriptor
             * write to this operation at least on the Pixel 3 with Antares, it can lead to a 133
             * which is likely some sort of Android bug as the descriptor write should be entirely
             * unrelated to the subscription.
             *
             * For now we are going to leave this as an un-matching device so that we can test further
             * but I.O. believes that we will inevitably require a delay, and the strategyDevice will
             * need to be null to match all phones.
             */
            AndroidDevice strategyDevice = strategyProvider.getUnmatchableDevice();
            Strategy strategy = strategyProvider.
                    getStrategyForPhoneAndGattConnection(strategyDevice, getConnection(),
                            Situation.DELAY_ANDROID_SUBSCRIPTION_EVENT);
            if(strategy == null) {
                mainThreadHandler.post(() -> {
                    if(characteristic.getUuid().equals(this.characteristic.getUuid())) {
                        getConnection().setState(GattState.DISABLE_CHARACTERISTIC_NOTIFICATION_SUCCESS);
                        Timber.v("[%s] Your characteristic had notifications disabled", getDevice());
                        builder.characteristicUuid(characteristic.getUuid())
                                .data(characteristic.getValue())
                                .gattState(getConnection().getGattState())
                                .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
                        callCallbackWithTransactionResultAndRelease(callback, builder.build());
                    }
                    getConnection().setState(GattState.IDLE);
                });
            } else {
                builder.characteristicUuid(characteristic.getUuid())
                        .data(characteristic.getValue())
                        .gattState(getConnection().getGattState())
                        .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
                strategy.applyStrategy(this, builder.build(), callback);
            }
        } else {
            // no need for strategy, it failed.
            getConnection().setState(GattState.DISABLE_CHARACTERISTIC_NOTIFICATION_FAILURE);
            builder.responseStatus(GattDisconnectReason.getReasonForCode(GattStatus.GATT_UNKNOWN.getCode()).ordinal())
                    .characteristicUuid(characteristic.getUuid())
                    .resultStatus(TransactionResult.TransactionResultStatus.FAILURE)
                    .gattState(getConnection().getGattState())
                    .responseStatus(GattStatus.GATT_UNKNOWN.ordinal())
                    .data(characteristic.getValue())
                    .serviceUuid(characteristic.getService().getUuid());
            mainThreadHandler.post(() -> {
                callCallbackWithTransactionResultAndRelease(callback, builder.build());
            });
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}
