/*
 *
 *  Copyright 2021 Fitbit, Inc. All rights reserved.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package com.fitbit.bluetooth.fbgatt.commands;

/**
 * Wrapper of the GattTransaction. This will provide necessary states and messages to the executors.
 *
 * @param <GattTransaction> the GattTransaction type to be used
 */
public class CommandRequest<GattTransaction> {
    private final GattTransaction transaction;
    private final RequestState state;
    private final String message;
    private final Exception exception;

    public CommandRequest(String message, RequestState state) {
        this(null, state, message, null);
    }

    public CommandRequest(GattTransaction transaction, RequestState state) {
        this(transaction, state, null, null);
    }

    public CommandRequest(Exception exception) {
        this(null, RequestState.FAILURE, null, exception);
    }

    public CommandRequest(GattTransaction transaction, RequestState state, String message, Exception exception) {
        this.transaction = transaction;
        this.state = state;
        this.message = message;
        this.exception = exception;
    }

    public GattTransaction getTransaction() {
        return transaction;
    }

    public RequestState getState() {
        return state;
    }

    public String getMessage() {
        return message;
    }

    public Exception getException() {
        return exception;
    }

    public enum RequestState {
        SUCCESS, FAILURE
    }
}
