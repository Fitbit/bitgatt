/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.stubbing.Answer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Will test the status changes for the BT adapter
 *
 * Created by iowens on 8/27/18.
 */
@RunWith(JUnit4.class)
public class BluetoothAdapterStatusTests {

    BluetoothRadioStatusListener mockStatusListener;
    private Context appContext;

    @Before
    public void setup(){
        // Context
        appContext = mock(Context.class);
        when(appContext.getSystemService(any(String.class))).thenReturn(null);
        when(appContext.getApplicationContext()).thenReturn(appContext);
        // started
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
        FitbitGatt.getInstance().setStarted(true);
        FitbitGatt.getInstance().setAppContext(appContext);
    }

    @After
    public void after() {
        FitbitGatt.setInstance(null);
    }

    @Test
    public void ensureBluetoothStatusOffWhenTurningOn(){
        BroadcastReceiver mockBroadcastReceiver = mock(BroadcastReceiver.class);
        mockStatusListener = new BluetoothRadioStatusListener(appContext, false);
        mockStatusListener.receiver = mockBroadcastReceiver;
        FitbitGatt.getInstance().setBluetoothListener(mockStatusListener);
        mockStatusListener.listener = FitbitGatt.getInstance();
        mockStatusListener.listener.bluetoothTurningOn();
        Assert.assertFalse(FitbitGatt.getInstance().isBluetoothOn);
    }

    @Test
    public void ensureBluetoothStatusOffWhenTurningOff(){
        BroadcastReceiver mockBroadcastReceiver = mock(BroadcastReceiver.class);
        mockStatusListener = new BluetoothRadioStatusListener(appContext, false);
        mockStatusListener.receiver = mockBroadcastReceiver;
        FitbitGatt.getInstance().setBluetoothListener(mockStatusListener);
        mockStatusListener.listener = FitbitGatt.getInstance();
        mockStatusListener.listener.bluetoothTurningOff();
        Assert.assertFalse(FitbitGatt.getInstance().isBluetoothOn);
    }

    @Test
    public void ensureBluetoothStatusOnWhenOn(){
        BroadcastReceiver mockBroadcastReceiver = mock(BroadcastReceiver.class);
        mockStatusListener = new BluetoothRadioStatusListener(appContext, false);
        mockStatusListener.receiver = mockBroadcastReceiver;
        FitbitGatt.getInstance().setBluetoothListener(mockStatusListener);
        mockStatusListener.listener = FitbitGatt.getInstance();
        mockStatusListener.listener.bluetoothOn();
        Assert.assertTrue(FitbitGatt.getInstance().isBluetoothOn);
    }

    @Test
    public void ensureBluetoothStatusOffWhenOff(){
        BroadcastReceiver mockBroadcastReceiver = mock(BroadcastReceiver.class);
        mockStatusListener = new BluetoothRadioStatusListener(appContext, false);
        mockStatusListener.receiver = mockBroadcastReceiver;
        FitbitGatt.getInstance().setBluetoothListener(mockStatusListener);
        mockStatusListener.listener = FitbitGatt.getInstance();
        mockStatusListener.listener.bluetoothOff();
        Assert.assertFalse(FitbitGatt.getInstance().isBluetoothOn);
    }
    @Test
    public void ensureBluetoothStatusCallbackOnWhenOn(){
        BroadcastReceiver mockBroadcastReceiver = mock(BroadcastReceiver.class);
        mockStatusListener = new BluetoothRadioStatusListener(appContext, false);
        mockStatusListener.receiver = mockBroadcastReceiver;
        FitbitGatt.getInstance().setBluetoothListener(mockStatusListener);
        FitbitGatt.FitbitGattCallback cb = spy(new NoOpGattCallback());
        FitbitGatt.getInstance().registerGattEventListener(cb);
        mockStatusListener.listener = FitbitGatt.getInstance();
        mockStatusListener.listener.bluetoothOn();
        Assert.assertTrue(FitbitGatt.getInstance().isBluetoothOn);
        verify(cb, atLeastOnce()).onBluetoothOn();
    }

    @Test
    public void ensureBluetoothStatusCallbackOffWhenOff(){
        BroadcastReceiver mockBroadcastReceiver = mock(BroadcastReceiver.class);
        mockStatusListener = new BluetoothRadioStatusListener(appContext, false);
        mockStatusListener.receiver = mockBroadcastReceiver;
        FitbitGatt.getInstance().setBluetoothListener(mockStatusListener);
        FitbitGatt.FitbitGattCallback cb = spy(new NoOpGattCallback());
        FitbitGatt.getInstance().registerGattEventListener(cb);
        mockStatusListener.listener = FitbitGatt.getInstance();
        mockStatusListener.listener.bluetoothOff();
        Assert.assertFalse(FitbitGatt.getInstance().isBluetoothOn);
        verify(cb, atLeastOnce()).onBluetoothOff();
    }
}
