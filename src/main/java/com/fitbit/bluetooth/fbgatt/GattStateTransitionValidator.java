/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.tx.SetClientConnectionStateTransaction;
import com.fitbit.bluetooth.fbgatt.tx.SetServerConnectionStateTransaction;

import androidx.annotation.NonNull;
import timber.log.Timber;

import static com.fitbit.bluetooth.fbgatt.GattStateTransitionValidator.GuardState.INVALID_TARGET_STATE;
import static com.fitbit.bluetooth.fbgatt.GattStateTransitionValidator.GuardState.OK;

/**
 * The purpose of this class is to evaluate the potential transition from the present state of a
 * gatt connection to the desired end state within a given transaction, this class will encapsulate
 * the rules required by the gatt for stable operation.
 *
 * Created by iowens on 11/12/17.
 */

class GattStateTransitionValidator<T extends GattTransaction<T>>  {

    enum GuardState {
        OK,
        INVALID_TARGET_STATE
    }

    /**
     * Validate transaction against allowable states
     *
     * @param currentState The present state of the gatt connection
     * @param transaction  The transaction to be evaluated
     * @return OK if the transition will be allowed, invalid state if it is not
     */

    @NonNull
    GuardState checkTransaction(GattState currentState, T transaction) {
        GuardState state;
        GattState successState = transaction.getSuccessState();
        if(currentState.equals(GattState.BT_OFF)) {
            Timber.w("[%s] BT is off we cannot perform any transactions", transaction.getDevice());
            return INVALID_TARGET_STATE;
        }
        if(transaction instanceof SetClientConnectionStateTransaction || transaction instanceof SetServerConnectionStateTransaction) {
            if (FitbitGatt.getInstance().isSlowLoggingEnabled()) {
                Timber.v("[%s] Not checking because we are manually resetting.  Current state %s, destination state %s",
                        transaction.getDevice(), currentState.name(), successState.name());
            }
            return OK;
        }
        Timber.v("[%s] Current State %s, Success State %s", transaction.getDevice(), currentState.name(), successState.name());
        state = checkIsConnectionAttemptWhileConnected(currentState, successState);
        if (state.equals(INVALID_TARGET_STATE)) {
            if (FitbitGatt.getInstance().isSlowLoggingEnabled()) {
                Timber.v("[%s] %s Entry state invalid, connecting while connected", transaction.getDevice(), transaction.getName());
            }
            return INVALID_TARGET_STATE;
        }
        state = checkIsDisconnectAttemptWhileDisconnected(currentState, successState);
        if (state.equals(INVALID_TARGET_STATE)) {
            if (FitbitGatt.getInstance().isSlowLoggingEnabled()) {
                Timber.v("[%s] %s Entry state invalid, disconnecting while disconnected", transaction.getDevice(), transaction.getName());
            }
            return INVALID_TARGET_STATE;
        }
        state = checkIsTransactionWhileDisconnected(currentState, successState);
        if (state.equals(INVALID_TARGET_STATE)) {
            if (FitbitGatt.getInstance().isSlowLoggingEnabled()) {
                Timber.v("[%s] %s Entry state invalid, you can't do something else while disconnected", transaction.getDevice(), transaction.getName());
            }
            return INVALID_TARGET_STATE;
        }
        state = checkStateIsValidForGattOperations(currentState, successState);
        if (state.equals(INVALID_TARGET_STATE)) {
            if (FitbitGatt.getInstance().isSlowLoggingEnabled()) {
                Timber.v("[%s] %s Entry state invalid, can't read, write, notify or indicate, until idle or connected", transaction.getDevice(), transaction.getName());
            }
            return INVALID_TARGET_STATE;
        }
        return OK;
    }

    /**
     * Will only allow transactions to pass that have a goal state of connected, if this is any
     * other goal type, then the transaction will be in illegal state
     * @param currentState The present gatt state
     * @param successState The desired target state
     * @return The resulting guard state determining if the transition is allowed
     */

    private GuardState checkIsTransactionWhileDisconnected(GattState currentState, GattState successState) {
        if(!GattState.DISCONNECTED.equals(currentState) && !GattState.DISCONNECTING.equals(currentState)) {
            return OK;
        }
        if(GattState.CONNECTED.equals(successState)) {
            return OK;
        }
        return INVALID_TARGET_STATE;
    }

    /**
     * If the intent is to connect, we will need to ensure that we are not in
     * GattState.CONNECTED or GattState.DISCONNECTED
     * @param currentState The present gatt state
     * @param successState The desired target state
     * @return The resulting guard state determining if the transition is allowed
     */

    private GuardState checkIsConnectionAttemptWhileConnected(GattState currentState, GattState successState) {
        // we only care about evaluating the state if the success state is connected
        if (!GattState.CONNECTED.equals(successState)) {
            return OK;
        }
        if (GattState.DISCONNECTED.equals(currentState) || GattState.CONNECTING.equals(currentState)) {
            return OK;
        }
        return INVALID_TARGET_STATE;
    }

    /**
     * Similar to the check for connection, we need to make sure we aren't disconnecting while disconnected
     * @param currentState The present gatt state
     * @param successState The desired gatt target state
     * @return The resulting guard state for the check
     */

    private GuardState checkIsDisconnectAttemptWhileDisconnected(GattState currentState, GattState successState) {
        if (!GattState.DISCONNECTED.equals(successState)) {
            return OK;
        }
        if (GattState.IDLE.equals(currentState) || GattState.CONNECTED.equals(currentState)) {
            return OK;
        }
        return INVALID_TARGET_STATE;
    }

    /**
     * For this guard we if target is any idle state, and the current state is idle or in progress,
     * we will proceed.
     * @param currentState The current state
     * @param successState The desired gatt target state
     * @return The guard state result
     */
    private GuardState checkStateIsValidForGattOperations(GattState currentState, GattState successState) {
        if ((StateType.IDLE.equals(currentState.getStateType()) ||
                StateType.IN_PROGRESS.equals(currentState.getStateType())) && (StateType.IDLE.equals(successState.getStateType()))) {
            return OK;
        }
        return INVALID_TARGET_STATE;
    }

    /**
     * We must check that adding services is allowed from the present state
     * @param currentState The present gatt state
     * @param successState The desired target state
     * @return Will check to ensure that we are in a state from which we can add services
     */
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private GuardState checkStateIsValidForAddingServices(GattState currentState, GattState successState) {
        if (!GattState.ADD_SERVICE_SUCCESS.equals(successState)) {
            return OK;
        }
        if (GattState.IDLE.equals(currentState)) {
            return OK;
        }
        return INVALID_TARGET_STATE;
    }
}
