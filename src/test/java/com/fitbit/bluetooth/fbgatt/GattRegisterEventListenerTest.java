/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import androidx.test.core.app.ApplicationProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import androidx.annotation.NonNull;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Evaluate gatt listener for transaction events
 *
 * Created by iowens on 12/2/17.
 */
@RunWith(RobolectricTestRunner.class)
public class GattRegisterEventListenerTest {

    private static final String MOCK_ADDRESS = "02:00:00:00:00:00";
    private GattConnection conn;

    @Before
    public void before() {
        // started
        FitbitGatt.getInstance().setStarted();
        FitbitBluetoothDevice device = mock(FitbitBluetoothDevice.class);
        conn = spy(new GattConnection(device, ApplicationProvider.getApplicationContext().getMainLooper()));
        conn.setMockMode(true);
        FitbitGatt.getInstance().putConnectionIntoDevices(device, conn);
    }

    @After
    public void after() {
        FitbitGatt.getInstance().shutdown();
        FitbitGatt.setInstance(null);
    }

    @Test
    public void makeSureCanNotRegisterListenerTwice() {
        assertTrue(FitbitGatt.getInstance().isInitialized());
        ConnectionEventListener eventListener = new ConnectionEventListener() {
            @Override
            public void onClientCharacteristicChanged(@NonNull TransactionResult result, @NonNull GattConnection connection) {

            }

            @Override
            public void onClientConnectionStateChanged(@NonNull TransactionResult result, @NonNull GattConnection connection) {

            }

            @Override
            public void onServicesDiscovered(@NonNull TransactionResult result, @NonNull GattConnection connection) {

            }

            @Override
            public void onMtuChanged(@NonNull TransactionResult result, @NonNull GattConnection connection) {

            }

            @Override
            public void onPhyChanged(@NonNull TransactionResult result, @NonNull GattConnection connection) {

            }


        };
        conn.registerConnectionEventListener(eventListener);
        conn.registerConnectionEventListener(eventListener);
        assertEquals(conn.numberOfEventListeners(), 1);
    }
}
