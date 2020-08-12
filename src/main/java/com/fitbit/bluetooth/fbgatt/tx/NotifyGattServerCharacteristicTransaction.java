/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx;

import com.fitbit.bluetooth.fbgatt.FitbitBluetoothDevice;
import com.fitbit.bluetooth.fbgatt.GattServerConnection;
import com.fitbit.bluetooth.fbgatt.GattServerTransaction;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.Situation;
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.strategies.Strategy;
import com.fitbit.bluetooth.fbgatt.util.GattDisconnectReason;
import com.fitbit.bluetooth.fbgatt.util.GattStatus;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import timber.log.Timber;

/**
 * Notify characteristic on the gatt server
 *
 * Created by iowens on 12/17/17.
 */

public class NotifyGattServerCharacteristicTransaction extends GattServerTransaction {
    private static final String NAME = "NotifyGattServerCharacteristicTransaction";
    protected final BluetoothGattCharacteristic characteristic;
    private final boolean confirm;
    private final FitbitBluetoothDevice device;

    public NotifyGattServerCharacteristicTransaction(GattServerConnection connection, FitbitBluetoothDevice device, GattState successEndState, BluetoothGattCharacteristic characteristic, boolean confirm) {
        super(connection, successEndState);
        this.characteristic = characteristic;
        this.confirm = confirm;
        this.device = device;
    }

    public NotifyGattServerCharacteristicTransaction(GattServerConnection connection, FitbitBluetoothDevice device, GattState successEndState, BluetoothGattCharacteristic characteristic, boolean confirm, long timeoutMillis) {
        super(connection, successEndState, timeoutMillis);
        this.characteristic = characteristic;
        this.confirm = confirm;
        this.device = device;
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        super.transaction(callback);
        getGattServer().setState(GattState.NOTIFYING_CHARACTERISTIC);
        boolean success;
        try {
            success = getGattServer().getServer().notifyCharacteristicChanged(device.getBtDevice(), this.characteristic, this.confirm);
        } catch (NullPointerException ex) {
            Timber.w(ex,"[%s] We are going to fail this tx due to the stack NPE, this is probably poor peripheral behavior, this should become a FW bug.", getDevice());
            Timber.w("[%s] btDevice %s characteristic %s confirm %s", device, device.getBtDevice(), this.characteristic, this.confirm);
            // Ensure that the flag is set to false, and that is is
            // impossible to be anything else stepping through after
            // this ... strategy time
            success = false;
        }
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
        if(!success) {
            getGattServer().setState(GattState.NOTIFY_CHARACTERISTIC_FAILURE);
            builder.resultStatus(TransactionResult.TransactionResultStatus.FAILURE)
                    .gattState(getGattServer().getGattState());
            mainThreadHandler.post(() -> {
                callCallbackWithTransactionResultAndRelease(callback, builder.build());
                getGattServer().setState(GattState.IDLE);
                // we want to apply this strategy to every phone, so we will provide an empty target android
                // device
                Strategy strategy = strategyProvider.
                        getStrategyForPhoneAndGattConnection(null, null,
                                Situation.TRACKER_WENT_AWAY_DURING_GATT_OPERATION);
                if(strategy != null) {
                    strategy.applyStrategy();
                }
            });
        }
    }

    @Override
    public void onServerNotificationSent(BluetoothDevice device, int status) {
        Timber.d("[%s] Notification sent response with status: %s", getDevice(), GattStatus.values()[status].name());
        TransactionResult.Builder builder = new TransactionResult.Builder().transactionName(getName());
        builder.responseStatus(GattDisconnectReason.getReasonForCode(status).ordinal());
        if(this.characteristic != null) {
            builder.characteristicUuid(this.characteristic.getUuid());
            builder.data(this.characteristic.getValue());
        }
        if(status == BluetoothGatt.GATT_SUCCESS) {
            getGattServer().setState(GattState.NOTIFY_CHARACTERISTIC_SUCCESS);
            builder.resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
            callCallbackWithTransactionResultAndRelease(callback, builder.build());
            getGattServer().setState(GattState.IDLE);
        } else {
            getGattServer().setState(GattState.NOTIFY_CHARACTERISTIC_FAILURE);
            builder.resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
            callCallbackWithTransactionResultAndRelease(callback, builder.build());
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}
