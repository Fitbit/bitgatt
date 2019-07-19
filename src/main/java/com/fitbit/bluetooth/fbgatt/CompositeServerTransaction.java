/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import timber.log.Timber;

/**
 * Composite transaction that will run all transactions delivered to it.  In this case
 * the success end state is irrelevant, it will be looking to be idle.  If a transaction in the
 * stack fails, the execution will be halted with a transaction result that indicates failure
 * then will return all execution results that have happened so far.
 *
 * Created by iowens on 04/25/19.
 */

public class CompositeServerTransaction extends GattTransaction {
    public static final String NAME = "CompositeServerTransaction";
    private List<GattTransaction> transactionList;
    private GattTransactionCallback finalCallback;
    private ArrayList<TransactionResult> results = new ArrayList<>();
    private AtomicInteger transactionIndex = new AtomicInteger(0);
    private TransactionQueueController compositeServerQueueController;

    public CompositeServerTransaction(@Nullable GattServerConnection connection, @NonNull List<GattTransaction> transactionList) {
        super(connection, GattState.IDLE);
        this.transactionList = transactionList;
        // setting the transaction timeout to the aggregate
        if (!transactionList.isEmpty()) {
            setTimeout(DEFAULT_GATT_TRANSACTION_TIMEOUT * transactionList.size());
        }
        // The composite server transaction will need it's own queue controller to run
        // child transactions while still blocking the main connection queue controller
        compositeServerQueueController = new TransactionQueueController(NAME);
        compositeServerQueueController.start();
    }

    @Override
    protected void transaction(GattTransactionCallback callback) {
        super.transaction(callback);
        this.finalCallback = callback;
        // at this point the CDL for the transaction has already been latched
        executeTransaction();
    }

    private void executeTransaction() {
        if (!transactionList.isEmpty()) {
            transactionList.get(transactionIndex.get()).commit(result -> {
                // now result is going to typically be on the main thread and this is a problem, so
                // in actuality we need to requeue the next tx run, the guarantee is that the provided
                // tx will run in order and this does not violate that even if other tx jump in-between
                results.add(result);
                int newValue = transactionIndex.incrementAndGet();
                Timber.v("[%s] Transaction result: %s", getDevice(), result);
                if (result.getResultStatus().equals(TransactionResult.TransactionResultStatus.SUCCESS)) {
                    if (newValue <= (transactionList.size() - 1)) {
                        // added the result on the main thread, but now we will queue the next on
                        // the main thread like normal but it will actually run on the queue
                        // controller thread
                        compositeServerQueueController.queueTransaction(() -> {
                            executeTransaction();
                            Timber.v("[%s] Transaction %s was successful, moving on to index: %d", getDevice(), result, newValue);
                        });
                    } else {
                        Timber.d("[%s] Finished running all transactions successfully, last result: %s", getDevice(), result);
                        TransactionResult.Builder builder = new TransactionResult.Builder();
                        builder.resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
                        builder.transactionName(NAME);
                        callCallbackWithTransactionResultAndRelease(finalCallback, builder.addTransactionResults(results).build());
                        compositeServerQueueController.stop();
                        compositeServerQueueController = null;
                    }
                } else {
                    Timber.w("[%s] Transaction %s failed with result: %s, aborting chain", getDevice(), result.getTransactionName(), result);
                    TransactionResult.Builder builder = new TransactionResult.Builder();
                    builder.resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
                    builder.transactionName(NAME).addTransactionResults(results);
                    callCallbackWithTransactionResultAndRelease(finalCallback, builder.build());
                    compositeServerQueueController.stop();
                    compositeServerQueueController = null;
                }
            });
        } else {
            Timber.w("[%s] Transaction list was empty", getDevice());
            TransactionResult.Builder builder = new TransactionResult.Builder();
            builder.resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
            builder.transactionName(NAME).addTransactionResults(results);
            callCallbackWithTransactionResultAndRelease(finalCallback, builder.build());
            compositeServerQueueController.stop();
            compositeServerQueueController = null;
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}
