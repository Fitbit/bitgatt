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
 * Will subscribe to a characteristic for notifications
 * <p>
 * Created by iowens on 12/7/17.
 */

public class SubscribeToCharacteristicNotificationsTransaction extends GattClientTransaction {
    public static final String NAME = "SubscribeToCharacteristicNotificationsTransaction";
    private final BluetoothGattCharacteristic characteristic;

    public SubscribeToCharacteristicNotificationsTransaction(@Nullable GattConnection connection, GattState successEndState, BluetoothGattCharacteristic characteristic) {
        super(connection, successEndState);
        this.characteristic = characteristic;
    }

    public SubscribeToCharacteristicNotificationsTransaction(@Nullable GattConnection connection, GattState successEndState, BluetoothGattCharacteristic characteristic, long timeoutMillis) {
        super(connection, successEndState, timeoutMillis);
        this.characteristic = characteristic;
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        super.transaction(callback);
        getConnection().setState(GattState.ENABLING_CHARACTERISTIC_NOTIFICATION);
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
        BluetoothGatt localGatt = getConnection().getGatt();
        if(localGatt == null) {
            Timber.w("Couldn't subscribe because gatt was null");
            getConnection().setState(GattState.ENABLE_CHARACTERISTIC_NOTIFICATION_FAILURE);
            builder.characteristicUuid(characteristic.getUuid())
                .gattState(getConnection().getGattState())
                .responseStatus(GattStatus.GATT_UNKNOWN.ordinal())
                .resultStatus(TransactionResult.TransactionResultStatus.FAILURE)
                .data(characteristic.getValue())
                .serviceUuid(characteristic.getService().getUuid());
            return;
        }
        try {
            if (localGatt.setCharacteristicNotification(this.characteristic, true)) {
                getConnection().setState(GattState.ENABLE_CHARACTERISTIC_NOTIFICATION_SUCCESS);
                Timber.v("[%s] Notification success on %s", getDevice(), this.characteristic.getUuid());
                builder.characteristicUuid(characteristic.getUuid())
                        .gattState(getConnection().getGattState())
                        .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS)
                        .responseStatus(GattStatus.GATT_SUCCESS.ordinal())
                        .data(characteristic.getValue())
                        .serviceUuid(characteristic.getService().getUuid());
            } else {
                getConnection().setState(GattState.ENABLE_CHARACTERISTIC_NOTIFICATION_FAILURE);
                builder.characteristicUuid(characteristic.getUuid())
                        .gattState(getConnection().getGattState())
                        .responseStatus(GattStatus.GATT_UNKNOWN.ordinal())
                        .resultStatus(TransactionResult.TransactionResultStatus.FAILURE)
                        .data(characteristic.getValue())
                        .serviceUuid(characteristic.getService().getUuid());
            }
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
            if (strategy == null) {
                mainThreadHandler.post(() -> {
                    callCallbackWithTransactionResultAndRelease(callback, builder.build());
                    // this can fail, but we are still idle because we can use the connection
                    getConnection().setState(GattState.IDLE);
                });
            } else {
                strategy.applyStrategy(this, builder.build(), callback);
            }
        } catch (NullPointerException ex) {
            Timber.w(ex, "[%s] We are going to fail this tx due to the stack NPE, this is probably poor peripheral behavior, this should become a FW bug.", getDevice());
            // we want to apply this strategy to every phone, so we will provide an empty target android
            // device
            getConnection().setState(GattState.ENABLE_CHARACTERISTIC_NOTIFICATION_FAILURE);
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
            Strategy strategy = strategyProvider.
                    getStrategyForPhoneAndGattConnection(null, getConnection(),
                            Situation.TRACKER_WENT_AWAY_DURING_GATT_OPERATION);
            if (strategy != null) {
                strategy.applyStrategy();
            }
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}
