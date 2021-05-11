/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.stubbing.Answer;
import androidx.annotation.NonNull;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Evaluate gatt listener for transaction events
 *
 * Created by iowens on 12/2/17.
 */
@RunWith(JUnit4.class)
public class GattRegisterEventListenerTest {

    private static final String MOCK_ADDRESS = "02:00:00:00:00:00";
    private GattConnection conn;

    @Before
    public void before() {
        // Context
        Context appContext = mock(Context.class);
        when(appContext.getSystemService(any(String.class))).thenReturn(null);
        when(appContext.getApplicationContext()).thenReturn(appContext);
        // started
        FitbitGatt.getInstance().setStarted();
        Handler mockHandler = mock(Handler.class);
        Looper mockLooper = mock(Looper.class);
        Thread mockThread = mock(Thread.class);
        when(mockThread.getName()).thenReturn("Irvin's mock thread");
        when(mockLooper.getThread()).thenReturn(mockThread);
        when(mockHandler.getLooper()).thenReturn(mockLooper);
        when(mockHandler.postDelayed(any(Runnable.class), anyLong())).thenAnswer((Answer) invocation -> {
            Runnable msg = invocation.getArgument(0);
            msg.run();
            return null;
        });
        when(mockHandler.post(any(Runnable.class))).thenAnswer((Answer) invocation -> {
            Runnable msg = invocation.getArgument(0);
            msg.run();
            return null;
        });
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(MOCK_ADDRESS, "fooDevice", mock(BluetoothDevice.class));
        conn = spy(new GattConnection(device, mockLooper));
        conn.setMockMode(true);
        when(conn.getMainHandler()).thenReturn(mockHandler);
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
