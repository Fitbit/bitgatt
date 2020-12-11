/*
 * Copyright 2020 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.btcopies.BluetoothGattCharacteristicCopy;
import com.fitbit.bluetooth.fbgatt.btcopies.BluetoothGattDescriptorCopy;
import com.fitbit.bluetooth.fbgatt.util.GattUtils;

import android.bluetooth.BluetoothGatt;
import android.os.Looper;

import java.util.Locale;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import timber.log.Timber;

/**
 * This transaction represents the base class for all bluetooth client related transactions
 *
 * It handles failing transaction when conditions are not met such as the connecting going away and
 * {@link GattClientTransaction} registration lifecycle
 */
public class GattClientTransaction extends GattTransaction<GattClientTransaction> implements GattClientListener {
    private final GattUtils utils = new GattUtils();
    private final GattConnection connection;

    public GattClientTransaction(@Nullable GattConnection connection, GattState successEndState) {
        super(successEndState);
        this.connection = connection;
    }

    public GattClientTransaction(@Nullable GattConnection connection, GattState successEndState, long timeoutMillis) {
        super(successEndState);
        this.connection = connection;
        setTimeout(timeoutMillis);
    }

    public GattConnection getConnection() {
        return connection;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public void commit(GattTransactionCallback callback) {
        // get main looper would only be null in a test, unless mocked
        // we have to do all of this null checking for connection because of a contract with fbairlink
        // that makes the transaction connection constructor accept null, we should review this
        // contract after the launch of Golden Gate.
        Looper mainLooper = this.appContext.getMainLooper();
        if (getConnection() != null && getConnection().getClientTransactionQueueController() != null) {
            if (mainLooper != null &&
                    (Thread.currentThread().equals(mainLooper.getThread()))) {
                throw new IllegalStateException(String.format(Locale.ENGLISH,
                        "[%s] This transaction %s is not allowed to run on the %s thread",
                        getDevice(),
                        getName(),
                        mainLooper.getThread()));
            }
        } else {
            throw new IllegalStateException(String.format(Locale.ENGLISH, "[%s] This transaction can not be run because there is no connection to support it", getDevice()));
        }
        super.commit(callback);
    }

    @Override
    protected boolean areConditionsValidForExecution(GattClientTransaction tx) {
        GattConnection txConnection = tx.getConnection();
        // check if transaction entry conditions are valid
        if (txConnection != null) {
            if (txConnection.checkTransaction(tx).equals(GattStateTransitionValidator.GuardState.INVALID_TARGET_STATE)) {
                TransactionResult transactionResult = new TransactionResult.Builder().transactionName(tx.getName())
                        .gattState(tx.getConnection().getGattState())
                        .resultStatus(TransactionResult.TransactionResultStatus.INVALID_STATE).build();
                mainThreadHandler.post(() -> callback.onTransactionComplete(transactionResult));
                release();
                // we will dispose of all timeouts now because none of the other runnables
                // will complete
                timeoutHandler.removeCallbacksAndMessages(null);
                return false;
            }
        } else {
            throw new IllegalStateException(String.format(Locale.ENGLISH, "[%s] No connection for this transaction", getDevice()));
        }
        return true;
    }


    @Nullable
    @Override
    public FitbitBluetoothDevice getDevice() {
        return (connection != null) ? getConnection().getDevice() : null;
    }

    @Override
    protected void unregisterListener(GattClientTransaction tx) {
        GattClientCallback gattClientCallback = FitbitGatt.getInstance().getClientCallback();
        if (gattClientCallback != null) {
            gattClientCallback.removeListener(tx);
        }
    }

    @Override
    protected void registerListener(GattClientTransaction tx) {
        GattClientCallback gattClientCallback = FitbitGatt.getInstance().getClientCallback();
        if (gattClientCallback != null) {
            gattClientCallback.addListener(tx);
        }
    }

    @Override
    protected TransactionResult getTimeoutTransactionResult(GattClientTransaction tx) {
        GattConnection txConnection = tx.getConnection();
        TransactionResult transactionResult;
        if (txConnection != null) {
            // tell the transaction that it timed out, so that in the case that it is blocking
            // the thread, it knows to release
            tx.onGattClientTransactionTimeout(txConnection);
            transactionResult = new TransactionResult.Builder().transactionName(tx.getName())
                    .gattState(txConnection.getGattState())
                    .resultStatus(TransactionResult.TransactionResultStatus.TIMEOUT).build();
        } else {
            transactionResult = new TransactionResult.Builder().transactionName(tx.getName())
                    .resultStatus(TransactionResult.TransactionResultStatus.INVALID_STATE).build();
        }
        return transactionResult;
    }

    @Override
    public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
        Timber.v("[%s] onPhyUpdate not handled in tx: %s", utils.debugSafeGetBtDeviceName(gatt), getName());
    }

    @Override
    public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
        Timber.v("[%s] onPhyRead not handled in tx: %s", utils.debugSafeGetBtDeviceName(gatt), getName());
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        Timber.v("[%s] onPhyRead not handled in tx: %s", utils.debugSafeGetBtDeviceName(gatt), getName());
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        Timber.v("[%s] onServicesDiscovered not handled in tx: %s", utils.debugSafeGetBtDeviceName(gatt), getName());
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristicCopy characteristic, int status) {
        if (FitbitGatt.getInstance().isSlowLoggingEnabled()) {
            Timber.v("[%s] onCharacteristicRead not handled in tx: %s", utils.debugSafeGetBtDeviceName(gatt), getName());
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristicCopy characteristic, int status) {
        if (FitbitGatt.getInstance().isSlowLoggingEnabled()) {
            Timber.v("[%s] onCharacteristicWrite not handled in tx: %s", utils.debugSafeGetBtDeviceName(gatt), getName());
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristicCopy characteristic) {
        if (FitbitGatt.getInstance().isSlowLoggingEnabled()) {
            Timber.v("[%s] onCharacteristicChanged not handled in tx: %s", utils.debugSafeGetBtDeviceName(gatt), getName());
        }
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptorCopy descriptor, int status) {
        Timber.v("[%s] onDescriptorRead not handled in tx: %s", utils.debugSafeGetBtDeviceName(gatt), getName());
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptorCopy descriptor, int status) {
        Timber.v("[%s] onDescriptorWrite not handled in tx: %s", utils.debugSafeGetBtDeviceName(gatt), getName());
    }

    @Override
    public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
        Timber.v("[%s] onReliableWriteCompleted not handled in tx: %s", utils.debugSafeGetBtDeviceName(gatt), getName());
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        Timber.v("[%s] onReadRemoteRssi not handled in tx: %s", utils.debugSafeGetBtDeviceName(gatt), getName());
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        Timber.v("[%s] onMtuChanged not handled in tx: %s", utils.debugSafeGetBtDeviceName(gatt), getName());
    }


    @Override
    public String getName() {
        return null;
    }
}
