/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx;

import com.fitbit.bluetooth.fbgatt.FitbitBluetoothDevice;
import com.fitbit.bluetooth.fbgatt.FitbitGatt;
import com.fitbit.bluetooth.fbgatt.GattClientTransaction;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.receivers.CreateBondTransactionBroadcastReceiver;
import com.fitbit.bluetooth.fbgatt.util.GattStatus;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import timber.log.Timber;

/**
 * Will create a bluetooth bond.  The timeout will be extended to BOND_TIMEOUT value for this
 * transaction as sometimes a bond takes a while.  While this bond task is executing the transaction
 * queue for the device in question will be blocked, this does not stop the other devices from performing
 * operations.  Note: This implementation does not deal with devices that disconnect after bond,
 * that is business logic that is outside of the scope of this implementation.
 * <p>
 * Created by iowens on 8/28/18.
 */
public class CreateBondTransaction extends GattClientTransaction implements CreateBondTransactionInterface {
    /**
     * The transaction name
     */
    public static final String NAME = "CreateBondTransaction";
    /**
     * The timeout override value to be used for the bond transaction
     */
    private static final long BOND_TIMEOUT = TimeUnit.MINUTES.toMillis(2);

    private Context context;

    /**
     * Receiver for {@link BluetoothDevice#ACTION_BOND_STATE_CHANGED}
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    private final BroadcastReceiver receiver = new CreateBondTransactionBroadcastReceiver(this);

    /**
     * The transaction builder
     */
    private final TransactionResult.Builder builder;

    /**
     * Constructor
     *
     * @param connection      The {@link GattConnection} connection to attempt a bond
     * @param successEndState The success state for this transaction
     */
    public CreateBondTransaction(@Nullable GattConnection connection, GattState successEndState) {
        super(connection, successEndState);
        builder = new TransactionResult.Builder();
        // we're going to want a longer timeout for the bond
        setTimeout(BOND_TIMEOUT);
    }

    public CreateBondTransaction(@Nullable GattConnection connection, GattState successEndState, long timeoutMillis) {
        super(connection, successEndState);
        builder = new TransactionResult.Builder();
        // we're going to want a longer timeout for the bond
        setTimeout(timeoutMillis);
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        super.transaction(callback);
        createBond();
    }

    private void createBond() {
        getConnection().setState(GattState.CREATING_BOND);
        Context context = FitbitGatt.getInstance().getAppContext();
        if (context == null) {
            Timber.w("[%s] Couldn't create the bond because the context was null", getDevice());
            bondFailure();
        } else {
            createBond(context);
        }
    }

    private void createBond(Context context) {
        FitbitBluetoothDevice device = getDevice();
        if (device == null) {
            Timber.w("Couldn't create the bond because device was null");
            bondFailure();
        } else {
            createBond(context, device);
            // let's go ahead and wait until the bond timeout
            synchronized (NAME) {
                try {
                    NAME.wait(BOND_TIMEOUT);
                } catch (InterruptedException e) {
                    Timber.e(e, "[%s] Well, the thread was interrupted, we will just let it go", device);
                }
            }
        }
    }

    private void createBond(Context context, FitbitBluetoothDevice device) {
        this.context = context;
        context.registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        device.getBtDevice().createBond();
    }

    @Override
    @VisibleForTesting
    public void bondSuccess() {
        Timber.v("[%s] The bond attempt succeeded", getDevice());
        getConnection().setState(GattState.CREATE_BOND_SUCCESS);
        builder.transactionName(NAME)
            .gattState(getConnection().getGattState())
            .responseStatus(GattStatus.GATT_SUCCESS.ordinal())
            .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
        if (callback != null) {
            callCallbackWithTransactionResultAndRelease(callback, builder.build());
        }
        getConnection().setState(GattState.IDLE);
        if (context != null) {
            context.unregisterReceiver(receiver);
        }
        synchronized (NAME) {
            NAME.notify();
        }

    }

    @Override
    public void bondFailure() {
        Timber.v("[%s] The bond attempt failed", getDevice());
        getConnection().setState(GattState.CREATE_BOND_FAILURE);
        builder.transactionName(NAME)
            .gattState(getConnection().getGattState())
            .responseStatus(GattStatus.GATT_INTERNAL_ERROR.ordinal())
            .resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
        if (callback != null) {
            callCallbackWithTransactionResultAndRelease(callback, builder.build());
        }
        getConnection().setState(GattState.IDLE);
        if (context != null) {
            context.unregisterReceiver(receiver);
        }
        synchronized (NAME) {
            NAME.notify();
        }
    }

    @Override
    protected void onGattClientTransactionTimeout(GattConnection connection) {
        super.onGattClientTransactionTimeout(connection);
        // there is no reason to return anything, this is just so that we can
        // unlock the monitor on the connection for timeout
        synchronized (NAME) {
            NAME.notify();
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}
