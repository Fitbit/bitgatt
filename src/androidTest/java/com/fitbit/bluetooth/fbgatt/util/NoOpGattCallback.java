/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.util;

import com.fitbit.bluetooth.fbgatt.FitbitGatt;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattServerConnection;
import com.fitbit.bluetooth.fbgatt.exception.AlreadyStartedException;
import com.fitbit.bluetooth.fbgatt.exception.BitGattStartException;

import static org.junit.Assert.*;

/**
 * Used by tests
 * <p>
 * Created by ilepadatescu on 09/20/2019
 */
public class NoOpGattCallback implements FitbitGatt.FitbitGattCallback {

    @Override
    public void onBluetoothPeripheralDiscovered(GattConnection connection) {
        //no-op
    }

    @Override
    public void onBluetoothPeripheralDisconnected(GattConnection connection) {
        //no-op
    }

    @Override
    public void onScanStarted() {
        //no-op
    }

    @Override
    public void onScanStopped() {
        //no-op
    }

    @Override
    public void onScannerInitError(BitGattStartException error) {
        fail("Gatt Scanner Start error " + error.getMessage());
    }

    @Override
    public void onPendingIntentScanStopped() {
        //no-op
    }

    @Override
    public void onPendingIntentScanStarted() {
        //no-op
    }

    @Override
    public void onBluetoothOff() {
        //no-op
    }

    @Override
    public void onBluetoothOn() {
        //no-op
    }

    @Override
    public void onBluetoothTurningOn() {
        //no-op
    }

    @Override
    public void onBluetoothTurningOff() {
        //no-op
    }

    @Override
    public void onGattServerStarted(GattServerConnection serverConnection) {
        //no-op
    }

    @Override
    public void onGattServerStartError(BitGattStartException error) {
        if(!(error instanceof AlreadyStartedException)) {
            fail("Gatt Server Start error " + error.getMessage());
        }
    }

    @Override
    public void onGattClientStarted() {
        //no-op
    }

    @Override
    public void onGattClientStartError(BitGattStartException error) {
        fail("Gatt Client Start error " + error.getMessage());
    }
}
