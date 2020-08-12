/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.btcopies.BluetoothGattCharacteristicCopy;
import com.fitbit.bluetooth.fbgatt.btcopies.BluetoothGattDescriptorCopy;
import com.fitbit.bluetooth.fbgatt.util.GattUtils;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;
import android.os.Build;
import android.os.Looper;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import androidx.annotation.Nullable;
import timber.log.Timber;

/**
 * This transaction represents the base class for all bluetooth server related transactions
 *
 * It handles failing transaction when conditions are not met such as the server not being created and
 * {@link GattServerCallback} registration lifecycle
 */
public class GattServerTransaction extends GattTransaction<GattServerTransaction> implements GattServerListener {
    private GattUtils utils = new GattUtils();
    private GattServerConnection gattServer;

    public GattServerTransaction(GattServerConnection server, GattState successEndState) {
        super(successEndState);
        this.gattServer = server;
    }

    public GattServerTransaction(GattServerConnection server, GattState successEndState, long timeoutMillis) {
        this(server, successEndState);
        setTimeout(timeoutMillis);
    }

    protected GattServerConnection getGattServer() {
        return this.gattServer;
    }

    @Override
    public void commit(GattTransactionCallback callback) {
        // get main looper would only be null in a test, unless mocked
        // we have to do all of this null checking for connection because of a contract with fbairlink
        // that makes the transaction connection constructor accept null, we should review this
        // contract after the launch of Golden Gate.
        Looper mainLooper = this.appContext.getMainLooper();
        if (getGattServer() != null && getGattServer().getServerTransactionQueueController() != null) {
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
    protected TransactionResult getTimeoutTransactionResult(GattServerTransaction tx) {
        GattServerConnection txGattServer = tx.getGattServer();
        TransactionResult transactionResult = null;
        if (txGattServer != null) {
            // tell the transaction that it timed out, so that in the case that it is blocking
            // the thread, it knows to release
            tx.onGattServerTransactionTimeout(txGattServer);
            transactionResult = new TransactionResult.Builder().transactionName(tx.getName())

                    .gattState(getGattServer().getGattState())
                    .resultStatus(TransactionResult.TransactionResultStatus.TIMEOUT).build();
        }
        return transactionResult;
    }

    protected boolean areConditionsValidForExecution(GattServerTransaction tx) {
        GattServerConnection txGattServer = tx.getGattServer();
        // check if transaction entry conditions are valid
        if (txGattServer != null) {
            if (txGattServer.checkTransaction(tx).equals(GattStateTransitionValidator.GuardState.INVALID_TARGET_STATE)) {
                TransactionResult transactionResult = new TransactionResult.Builder().transactionName(tx.getName())
                        .gattState(tx.getGattServer().getGattState())
                        .resultStatus(TransactionResult.TransactionResultStatus.INVALID_STATE).build();
                mainThreadHandler.post(() -> callback.onTransactionComplete(transactionResult));
                release();
                // we will dispose of all timeouts now because none of the other runnables
                // will complete
                timeoutHandler.removeCallbacksAndMessages(null);
                return false;
            }
        } else {
            throw new IllegalStateException(String.format(Locale.ENGLISH, "[%s] No gatt server for this transaction", getDevice()));
        }
        return true;
    }

    @Nullable
    @Override
    public FitbitBluetoothDevice getDevice() {
        return null;
    }

    @Override
    protected void unregisterListener(GattServerTransaction tx) {
        GattServerCallback serverCallback = FitbitGatt.getInstance().getServerCallback();
        if (serverCallback != null) {
            serverCallback.removeListener(tx);
        }
    }

    @Override
    protected void registerListener(GattServerTransaction tx) {
        GattServerCallback serverCallback = FitbitGatt.getInstance().getServerCallback();
        if (serverCallback != null) {
            serverCallback.addListener(tx);
        }
    }

    @Override
    public void onServerConnectionStateChange(BluetoothDevice device, int status, int newState) {
        Timber.v("[%s] onServerConnectionStateChange not handled in tx: %s", utils.debugSafeGetBtDeviceName(device), getName());
    }

    @Override
    public void onServerServiceAdded(int status, BluetoothGattService service) {
        Timber.v("[%s] onServerServiceAdded not handled in tx: %s", Build.MODEL, getName());
    }

    @Override
    public void onServerCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristicCopy characteristic) {
        Timber.v("[%s] onServerCharacteristicReadRequest not handled in tx: %s", utils.debugSafeGetBtDeviceName(device), getName());
    }

    @Override
    public void onServerCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristicCopy characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
        Timber.v("[%s] onServerCharacteristicWriteRequest not handled in tx: %s", utils.debugSafeGetBtDeviceName(device), getName());
    }

    @Override
    public void onServerDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptorCopy descriptor) {
        Timber.v("[%s] onServerDescriptorReadRequest not handled in tx: %s", utils.debugSafeGetBtDeviceName(device), getName());
    }

    @Override
    public void onServerDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptorCopy descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
        Timber.v("[%s] onServerDescriptorWriteRequest not handled in tx: %s", utils.debugSafeGetBtDeviceName(device), getName());
    }

    @Override
    public void onServerExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
        Timber.v("[%s] onServerExecuteWrite not handled in tx: %s", utils.debugSafeGetBtDeviceName(device), getName());
    }

    @Override
    public void onServerNotificationSent(BluetoothDevice device, int status) {
        Timber.v("[%s] onServerNotificationSent not handled in tx: %s", utils.debugSafeGetBtDeviceName(device), getName());
    }

    @Override
    public void onServerMtuChanged(BluetoothDevice device, int mtu) {
        Timber.v("[%s] onServerMtuChanged not handled in tx: %s", utils.debugSafeGetBtDeviceName(device), getName());
    }

    @Override
    public void onServerPhyUpdate(BluetoothDevice device, int txPhy, int rxPhy, int status) {
        Timber.v("[%s] onServerPhyUpdate not handled in tx: %s", utils.debugSafeGetBtDeviceName(device), getName());
    }

    @Override
    public void onServerPhyRead(BluetoothDevice device, int txPhy, int rxPhy, int status) {
        Timber.v("[%s] onServerPhyRead not handled in tx: %s", utils.debugSafeGetBtDeviceName(device), getName());
    }

    @Override
    public String getName() {
        return null;
    }
}
