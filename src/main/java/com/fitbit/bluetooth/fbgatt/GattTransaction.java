/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.tx.GattConnectTransaction;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import timber.log.Timber;

/**
 * The base gatt transaction task, will encapsulate either a single operation, or allow multiple
 * operations to be performed atomically.  Users must subclass and implement the functionality they
 * desire against the gatt connection object passed in.
 * <p>
 * If using pre / post commits, the implementer should be aware that only the last transaction result
 * will be returned from the list if it is a successful run, if it fails, the chain will be halted
 * and the failing transaction will return.  There will be only one transaction result from the class
 * the name of the returned result may not match the name of the main transaction, for example, if you
 * add a {@link com.fitbit.bluetooth.fbgatt.tx.SubscribeToCharacteristicNotificationsTransaction} as
 * a pre-commit, then an {@link com.fitbit.bluetooth.fbgatt.tx.WriteGattDescriptorTransaction} as the main
 * followed by an {@link com.fitbit.bluetooth.fbgatt.tx.UnSubscribeToGattCharacteristicNotificationsTransaction}
 * the resulting success {@link com.fitbit.bluetooth.fbgatt.TransactionResult} will be for the
 * {@link com.fitbit.bluetooth.fbgatt.tx.UnSubscribeToGattCharacteristicNotificationsTransaction}, not
 * the {@link com.fitbit.bluetooth.fbgatt.tx.WriteGattDescriptorTransaction}
 * <p>
 * Created by iowens on 10/17/17.
 */
public abstract class GattTransaction<T extends GattTransaction<T>> {
    /*
     * We shouldn't allow any gatt transaction including
     */
    protected static final long DEFAULT_GATT_TRANSACTION_TIMEOUT = TimeUnit.SECONDS.toMillis(60);

    protected final Handler timeoutHandler;
    protected final Handler mainThreadHandler;

    private long timeout = DEFAULT_GATT_TRANSACTION_TIMEOUT;

    private GattState successEndState;

    protected final ArrayList<T> preCommitHooks;

    protected final ArrayList<T> postCommitHooks;

    protected Context appContext; //NOPMD
    public GattTransactionCallback callback;

    protected AtomicBoolean taskHasStarted = new AtomicBoolean(false);
    protected AtomicInteger executedTransactions = new AtomicInteger(0);
    protected boolean haltChain = false;
    private final Object hookLock = new Object();
    private final CountDownLatch cdl = new CountDownLatch(1);
    protected StrategyProvider strategyProvider = new StrategyProvider();

    public GattTransaction(GattState successEndState) {
        this.appContext = FitbitGatt.getInstance().getAppContext();
        if (this.appContext == null) {
            Timber.w("Bitgatt must not have been started, please start Bitgatt");
            throw new IllegalStateException("You must start Bitgatt before creating transactions");
        }
        // let's put the timeout on the main looper so that we don't end up
        // deadlocking ourselves over work
        this.timeoutHandler = new Handler(this.appContext.getMainLooper());
        // We will need the main thread handler for calling back on errors that occur prior to the
        // gatt request for consistency.
        this.mainThreadHandler = new Handler(this.appContext.getMainLooper());
        this.successEndState = successEndState;

        /*
         * Often this will have no entries
         */
        this.preCommitHooks = new ArrayList<>();
        /*
         * Often this will have no entries
         */
        this.postCommitHooks = new ArrayList<>();
    }

    protected void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    protected long getTimeout() {
        return this.timeout;
    }

    public abstract @Nullable
    FitbitBluetoothDevice getDevice();

    /**
     * Used to determine if this transaction has already been run
     *
     * @return Will return true if it has, false if it hasn't
     */

    public boolean hasStarted() {
        return taskHasStarted.get();
    }

    /**
     * Will execute the transaction's discreet operations, will return before executing all transactions
     * if a transaction status of fail or timeout is returned before the last transaction, transactions
     * will be executed in the order that they were added.
     * <p>
     * All transaction callbacks will occur on the fbgatt thread associated with the connection,
     * unless they are performing GATT operations which will result on the callback being delivered
     * on the main thread.  If the user needs to transition back to the main thread for a non-gatt
     * operation, they will have to make that transition themselves.
     */
    @VisibleForTesting( otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public void commit(GattTransactionCallback callback) {

        if (taskHasStarted.getAndSet(true)) {
            throw new IllegalStateException(String.format(Locale.ENGLISH, "[%s] This transaction was already started!", getDevice()));
        }
        // let's allocate the array to the proper size ( why let it grow and waste cycles )
        ArrayList<T> transactions = new ArrayList<>(preCommitHooks.size() + postCommitHooks.size() + 1);
        // if this is a composite transaction, we will want to make sure that while intermediate callbacks can be called back
        // we hold a reference to the parent tx and call it on tx complete
        synchronized (hookLock) {
            this.callback = callback;
            transactions.addAll(preCommitHooks);
            transactions.add((T) this);
            transactions.addAll(postCommitHooks);
        }
        // the entire transaction must complete in {@link timeout} time.
        scheduleTransactionTimeout((T) this, callback);
        while (!transactions.isEmpty()) {
            T tx = (T) transactions.remove(0);
            if (!areConditionsValidForExecution(tx)) {
                Timber.w("[%s] The transaction conditions are not met", getDevice());
                return;
            }
            if (this.haltChain) {
                Timber.w("[%s] We are aborting the chain because a transaction failed", getDevice());
                return;
            }
            executeTransaction(tx, callback);
            // in the case of the composite transaction, this can lead to the composite transaction
            // scheduler being interrupted which will throw on await.  If we are cancelled at this
            // point, we should quietly end.
            if(!Thread.currentThread().isAlive() || Thread.currentThread().isInterrupted()) {
                Timber.i("Already stopped");
                return;
            }
            try {
                cdl.await(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Timber.d("Transaction was interrupted while waiting for result, re-interrupting thread : %s", Thread.currentThread().getName());
                Thread.currentThread().interrupt();
                return;
            }
        }
        if (FitbitGatt.getInstance().isSlowLoggingEnabled()) {
            Timber.v("[%s] Completed running all pre-commit, post-commit, and main transactions", getDevice());
        }
    }

    /**
     * Will check to see if the entry conditions are valid for this transaction execution is to be
     * used in the loop of pre / post commit transactions
     *
     * @param tx The transaction to check
     * @return True if conditions are valid, false otherwise
     */
    protected abstract boolean areConditionsValidForExecution(T tx);

    /**
     * Will actually execute a given transaction
     *
     * @param tx The transaction to be executed
     */
    private void executeTransaction(T tx, GattTransactionCallback callback) {
        if (FitbitGatt.getInstance().isSlowLoggingEnabled()) {
            Timber.v("[%s] Running transaction: %s", getDevice(), tx.getName());
        }
        registerListener(tx);
        // it might be a pre / post commit hook so we'll need to set it here too on the tx
        tx.taskHasStarted.getAndSet(true);
        tx.transaction(getGattTransactionCallback(tx, callback));
    }

    protected abstract void unregisterListener(T tx);

    protected abstract void registerListener(T tx);

    protected int getExecutedTransactions() {
        return executedTransactions.get();
    }

    /**
     * Will likely need to be called from {@link com.fitbit.bluetooth.fbgatt.strategies.Strategy} subclasses,
     * public for this reason.
     * @param callback The gatt transaction callback
     * @param result The transaction result
     */
    @CallSuper
    public void callCallbackWithTransactionResultAndRelease(GattTransactionCallback callback, TransactionResult result) {
        // to deal with transaction changes after the callback is null
        if(callback != null) {
            callback.onTransactionComplete(result);
        } else {
            Timber.i("The callback was null, not delivering result: %s, but releasing", result);
            // fixing https://console.firebase.google.com/project/api-project-625585532877/crashlytics/app/android:com.fitbit.betabit.FitbitMobile.hockeyapp/issues/5ca51fcbf8b88c296348095d?time=last-seven-days&sessionId=5CAB9C94031500014AB702C03F806B64_DNE_8_v2
        }
        release();
    }

    private ParentGattTransactionCallback getGattTransactionCallback(T tx, GattTransactionCallback wrappedCallback) {
        return new ParentGattTransactionCallback() {
            @Override
            public void onTransactionComplete(@NonNull TransactionResult result) {
                int done = executedTransactions.addAndGet(1);
                int totalTx = preCommitHooks.size() + postCommitHooks.size() + 1; // the 1 is this
                if (FitbitGatt.getInstance().isSlowLoggingEnabled()) {
                    Timber.v("[%s] Transaction %s complete.", getDevice(), tx.getName());
                    Timber.v("[%s] Transactions done %d of %d", getDevice(), done, totalTx);
                }
                if (!result.resultStatus.equals(TransactionResult.TransactionResultStatus.SUCCESS)) {
                    Timber.w("[%s] The transaction %s failed, Result: %s", getDevice(), tx.getName(), result);
                    wrappedCallback.onTransactionComplete(result);
                    unregisterListener(tx);
                    Timber.w("[%s] Halting the execution chain because tx %s failed", getDevice(), tx.getName());
                    GattTransaction.this.haltChain = true;
                    // we will dispose of all timeouts now because none of the other runnables
                    // will complete
                    timeoutHandler.removeCallbacksAndMessages(null);
                } else {
                    if (FitbitGatt.getInstance().isSlowLoggingEnabled()) {
                        Timber.d("[%s] Transaction %s success, Result: %s", getDevice(), tx.getName(), result);
                    }
                    // the callbacks are always handled by the individual transactions, here we only
                    // want to manage the timeout
                    if (done == totalTx) {
                        // we only want to remove the timeout once the final tx completes
                        timeoutHandler.removeCallbacksAndMessages(null);
                        // only callback here if there is only a single transaction, otherwise
                        // let the internal callbacks handle it
                        if (totalTx == 1) {
                            wrappedCallback.onTransactionComplete(result);
                            release();
                        }
                    } else if (FitbitGatt.getInstance().isSlowLoggingEnabled()) {
                        Timber.v("[%s] Pre / Post commit tx : %s completed successfully", getDevice(), tx.getName());
                    }
                    unregisterListener(tx);
                }
            }
        };
    }

    private void scheduleTransactionTimeout(T tx, GattTransactionCallback callback) {
        timeoutHandler.postDelayed(() -> handleTimeout(tx, callback), timeout);
    }

    protected void handleTimeout(T tx, GattTransactionCallback callback) {
        // for the purpose of caching so that we don't leak in the instance of an illegal state
        // exception
        final GattTransactionCallback localCallback;
        localCallback = callback;
        this.callback = null;
        // on timeout we will need to halt the chain and cancel any remaining timeouts
        this.haltChain = true;
        // some things will have locked using the connection object
        timeoutHandler.removeCallbacksAndMessages(null);
        TransactionResult transactionResult = getTimeoutTransactionResult(tx);
        if (transactionResult == null) {
            transactionResult = new TransactionResult.Builder().transactionName(tx.getName())
                    .resultStatus(TransactionResult.TransactionResultStatus.INVALID_STATE).build();
            localCallback.onTransactionComplete(transactionResult);
            unregisterListener(tx);
            release();
            throw new IllegalStateException(String.format(Locale.ENGLISH, "[%s] Gatt server and gatt client can not both be null", getDevice()));
        }
        localCallback.onTransactionComplete(transactionResult);
        unregisterListener(tx);
        release();
        Timber.v("[%s] The transaction timed out and the callbacks have already been notified, going to idle state", getDevice());
    }

    protected abstract TransactionResult getTimeoutTransactionResult(T tx);

    @Deprecated
    public void addPreCommitHook(T preCommitHook) {
        if (taskHasStarted.get()) {
            throw new IllegalStateException(String.format(Locale.ENGLISH, "[%s] You can't add hooks after task has started", getDevice()));
        }
        if (preCommitHook instanceof GattConnectTransaction) {
            Timber.w("[%s] Gatt connect transactions can not be a pre-commit condition, you must connect explicitly", getDevice());
            return;
        }
        synchronized (hookLock) {
            this.preCommitHooks.add(preCommitHook);
        }
    }

    @Deprecated
    public void removePreCommitHook(T preCommitHook) {
        if (taskHasStarted.get()) {
            throw new IllegalStateException(String.format(Locale.ENGLISH, "[%s] You can't remove hooks after task has started", getDevice()));
        }
        synchronized (hookLock) {
            this.preCommitHooks.remove(preCommitHook);
        }
    }

    @Deprecated
    public void addPostCommitHook(T postCommitHook) {
        if (taskHasStarted.get()) {
            throw new IllegalStateException(String.format(Locale.ENGLISH, "[%s] You can't add hooks after task has started", getDevice()));
        }
        synchronized (hookLock) {
            this.postCommitHooks.add(postCommitHook);
        }
    }

    @Deprecated
    public void removePostCommitHook(T postCommitHook) {
        if (taskHasStarted.get()) {
            throw new IllegalStateException(String.format(Locale.ENGLISH, "[%s] You can't remove hooks after task has started", getDevice()));
        }
        synchronized (hookLock) {
            this.postCommitHooks.remove(postCommitHook);
        }
    }

    @VisibleForTesting
    @SuppressWarnings("unused") // API Method
    public ArrayList<T> getPreCommitHooks() {
        return preCommitHooks;
    }

    @VisibleForTesting
    @SuppressWarnings("unused") // API Method
    public ArrayList<T> getPostCommitHooks() {
        return postCommitHooks;
    }

    @CallSuper
    protected void release(){
        cdl.countDown();
    }

    protected void transaction(GattTransactionCallback callback) {
        this.callback = callback;
    }

    public abstract String getName();

    protected void onGattClientTransactionTimeout(GattConnection connection) {
        Timber.v("[%s] onTimeout not handled in tx: %s", connection.getDevice(), getName());
    }

    @SuppressWarnings("WeakerAccess") // API Method
    protected void onGattServerTransactionTimeout(GattServerConnection connection) {
        Timber.v("[%s] onTimeout for server connection %s not handled in tx: %s", Build.MODEL, connection, getName());
    }

    protected GattState getSuccessState() {
        return successEndState;
    }

    /**
     * @return a String suitable for logging that contains a dump of the state of this transaction
     */
    @SuppressWarnings("unused") // API Method
    public String getStateDump() {
        return callback +
                " taskHasStarted: " +
                taskHasStarted.get() +
                " haltChain: " +
                haltChain +
                " executedTransactions: " +
                executedTransactions.get();
    }

    private static class ParentGattTransactionCallback implements GattTransactionCallback {

        @Override
        public void onTransactionComplete(@NonNull TransactionResult result) {

        }
    }
}
