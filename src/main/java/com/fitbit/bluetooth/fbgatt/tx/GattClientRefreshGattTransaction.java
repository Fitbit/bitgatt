/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tx;

import com.fitbit.bluetooth.fbgatt.GattClientTransaction;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransaction;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.TransactionResult;

import android.bluetooth.BluetoothGatt;
import androidx.annotation.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;

import timber.log.Timber;

/**
 * This transaction should only reluctantly be used by upstream strategies to resolve connection
 * issues after entirely understanding the problem and after working to resolve it with other
 * conventional solutions.  This call is likely to not work at some point and it's use is at your
 * own risk.  In the past it has caused some phones to have inconsistencies between their gatt cache
 * and what was in the filesystem ( db ) among other issues, please only use this if you know exactly
 * why you are doing it and in the narrowest of circumstances when you know that it will help.
 */

public class GattClientRefreshGattTransaction extends GattClientTransaction {

    public static final String NAME = "GattClientRefreshGattTransaction";
    private final CountDownLatch cdl = new CountDownLatch(2);

    /**
     * The successful end state here will be {@link GattState#REFRESH_GATT_SUCCESS}
     *
     * @param connection      The {@link GattConnection} upon which you wish to try refresh
     * @param successEndState The success end state of this transaction
     */

    public GattClientRefreshGattTransaction(@Nullable GattConnection connection, GattState successEndState) {
        super(connection, successEndState);
    }

    public GattClientRefreshGattTransaction(@Nullable GattConnection connection, GattState successEndState, long timeoutMillis) {
        super(connection, successEndState, timeoutMillis);
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        super.transaction(callback);
        getConnection().setState(GattState.REFRESH_GATT_IN_PROGRESS);
        TransactionResult.Builder resultBuilder = new TransactionResult.Builder().transactionName(NAME);
        boolean success = doRefresh();
        if(success) {
            // if we are still connected, don't do anything, this probably didn't work or this phone is weird
            // This call may or may not have worked, this phone doesn't disconnect after
            // refresh which is a bit odd, so YMMV
            Timber.v("[%s] The phone didn't disconnect so our state remains connected, the current connection status is ambiguous", getDevice());
            resultBuilder.resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
            getConnection().setState(GattState.REFRESH_GATT_SUCCESS);
            resultBuilder.gattState(getConnection().getGattState());
            callCallbackWithTransactionResultAndRelease(callback, resultBuilder.build());
            getConnection().setState(GattState.IDLE);
        } else {
            // if this failed, we probably will have done nothing which in this case is a success
            Timber.v("[%s] The refresh call didn't succeed the method might be more protected or different than we expected in some way", getDevice());
            resultBuilder.resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
            getConnection().setState(GattState.REFRESH_GATT_FAILURE);
            resultBuilder.gattState(getConnection().getGattState());
            callCallbackWithTransactionResultAndRelease(callback, resultBuilder.build());
            getConnection().setState(GattState.IDLE);
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    private boolean doRefresh(){
        boolean refreshSucceeded = false;
        BluetoothGatt localGatt = getConnection().getGatt();
        if(localGatt == null) {
            return false;
        }
        try {
            //@SuppressLint("PrivateApi") Method refreshMethod = BluetoothGatt.class.getDeclaredMethod("refresh", (Class<?>) null);
            //refreshSucceeded = (Boolean) refreshMethod.invoke(localGatt, (Object) null);
            //if(!refreshSucceeded) {
                // let's try another way, the above doesn't work, this does.
                Method localMethod = localGatt.getClass().getMethod("refresh");
                refreshSucceeded = (Boolean) localMethod.invoke(localGatt);
           // }
            /*
             * If this method works, the device will be disconnected, and we have no idea of
             * what the state of the connection will be as this is private, it could differ
             * between different devices so we should release whatever resources we are holding
             * and pretend it is a new connection.
             */
            Timber.v("[%s] refreshGatt: %s", getDevice(), Boolean.toString(refreshSucceeded));
        } catch (NullPointerException e) {
            Timber.w(e, "[%s] Catching null receiver exception", getDevice());
            /*
             * We only want to log this one to Crashlytics because it is the only one we might
             * be able to do anything about, it will be a crash in close if the client_if is
             * gone.
             */
            Timber.e(e);
            // https://www.fabric.io/fitbit7/android/apps/com.fitbit.fitbitmobile/issues/58e911a00aeb16625b6ab2f5?time=last-seven-days
        } catch (NoSuchMethodException e) {
            Timber.w(e, "[%s] BluetoothGatt.refresh() method not found!", getDevice());
        } catch (InvocationTargetException e) {
            Timber.w(e, "[%s] Invoking BluetoothGatt.refresh() method not failed!", getDevice());
        } catch (IllegalAccessException e) {
            Timber.w(e, "[%s] Illegal access to BluetoothGatt.refresh()!", getDevice());
        } catch (ClassCastException e) {
            Timber.w(e, "[%s] BluetoothGatt.refresh() does not return a Boolean!", getDevice());
        } catch (SecurityException e) {
            Timber.w(e, "[%s] BluetoothGatt.refresh() threw a security exception!", getDevice());
        } finally {
            if(refreshSucceeded) {
                Timber.d("[%s] Calling refresh, you will need to discover services again at least, and disconnect and reconnect at most.", getDevice());
            }
        }
        return refreshSucceeded;
    }
}
