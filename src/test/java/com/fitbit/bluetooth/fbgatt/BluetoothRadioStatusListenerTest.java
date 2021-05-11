/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.stubbing.Answer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class BluetoothRadioStatusListenerTest {

    private BluetoothRadioStatusListener statusListener;
    private ScheduledExecutorService singleThreadExecutor = Executors.newSingleThreadScheduledExecutor();
    private Context ctx;
    private Handler mockHandler;
    @SuppressWarnings("FutureReturnValueIgnored")
    private Answer<Boolean> handlerPostAnswer = invocation -> {
        Long delay = 0L;
        if (invocation.getArguments().length > 1) {
            delay = invocation.getArgument(1);
        }
        Runnable msg = invocation.getArgument(0);
        if (msg != null) {
            singleThreadExecutor.schedule(msg, delay, TimeUnit.MILLISECONDS);
        }
        return true;
    };

    @Before
    public void before(){
        Looper mockMainThreadLooper = mock(Looper.class);
        Thread mockMainThread = mock(Thread.class);
        when(mockMainThread.getName()).thenReturn("Irvin's mock thread");
        when(mockMainThreadLooper.getThread()).thenReturn(mockMainThread);
        ctx = mock(Context.class);
        when(ctx.getApplicationContext()).thenReturn(ctx);
        when(ctx.getMainLooper()).thenReturn(mockMainThreadLooper);
        mockHandler = mock(Handler.class);
        doAnswer(handlerPostAnswer).when(mockHandler).post(any(Runnable.class));
        doAnswer(handlerPostAnswer).when(mockHandler).postDelayed(any(Runnable.class), anyLong());
        when(mockHandler.getLooper()).thenReturn(mockMainThreadLooper);
        statusListener = new BluetoothRadioStatusListener(ctx, false, mockMainThreadLooper);
    }

    @After
    public void after() {
        FitbitGatt.setInstance(null);
    }

    // simple case, if state is off and it goes to on, should return true
    @Test
    public void testShouldScheduleCallbackBtOffToBtOn(){
        // assume not flapping
        statusListener.setLastEvent(SystemClock.elapsedRealtimeNanos() - BluetoothRadioStatusListener.MIN_TURNING_ON_CALLBACK_DELAY);
        boolean result = statusListener.shouldScheduleCallback(BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_ON);
        assertTrue(result);
    }

    // simple case, if state is on and it goes to off, should return true
    @Test
    public void testShouldScheduleCallbackBtOnToBtOff(){
        // assume not flapping
        statusListener.setLastEvent(SystemClock.elapsedRealtimeNanos() - BluetoothRadioStatusListener.MIN_TURNING_OFF_CALLBACK_DELAY);
        boolean result = statusListener.shouldScheduleCallback(BluetoothAdapter.STATE_ON, BluetoothAdapter.STATE_OFF);
        assertTrue(result);
    }

    @Test
    public void testShouldCallbackForBluetoothTurningOnToTurningOnSomehow(){
        statusListener.setLastEvent(SystemClock.elapsedRealtimeNanos() - BluetoothRadioStatusListener.MIN_TURNING_ON_CALLBACK_DELAY);
        boolean result = statusListener.shouldScheduleCallback(BluetoothAdapter.STATE_ON, BluetoothAdapter.STATE_ON);
        assertFalse(result);
    }

    @Test
    public void testShouldCallbackForBluetoothTurningOffToTurningOffSomehow(){
        statusListener.setLastEvent(SystemClock.elapsedRealtimeNanos() - BluetoothRadioStatusListener.MIN_TURNING_OFF_CALLBACK_DELAY);
        boolean result = statusListener.shouldScheduleCallback(BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_OFF);
        assertFalse(result);
    }

    @Test
    public void flappingTestToggleThreeTimesInLessThanASecondFromOnToOff(){
        boolean result;
        boolean onOrOff = false;
        for(int i=0; i <= 4; i++) {
            result = statusListener.shouldScheduleCallback(onOrOff ? BluetoothAdapter.STATE_ON : BluetoothAdapter.STATE_OFF,
                    onOrOff ? BluetoothAdapter.STATE_OFF : BluetoothAdapter.STATE_ON);
            assertFalse(result);
            onOrOff = !onOrOff;
        }
    }

    @Test
    public void flappingTestToggleTwiceInMoreThanASecondFromOffToOn() {
        boolean result = false;
        // assume not flapping at start
        statusListener.setLastEvent(SystemClock.elapsedRealtimeNanos() - BluetoothRadioStatusListener.MIN_TURNING_OFF_CALLBACK_DELAY);
        boolean onOrOff = true;
        for(int i=0; i <= 2; i++) {
            result = statusListener.shouldScheduleCallback(onOrOff ? BluetoothAdapter.STATE_ON : BluetoothAdapter.STATE_OFF,
                    onOrOff ? BluetoothAdapter.STATE_OFF : BluetoothAdapter.STATE_ON);
            statusListener.setLastEvent(SystemClock.elapsedRealtimeNanos() - BluetoothRadioStatusListener.MIN_TURNING_ON_CALLBACK_DELAY);
            onOrOff = !onOrOff;
        }
        assertTrue(result);
    }

    @Test
    public void flappingTestToggleTwiceInLessThanASecondFromOffToOn() {
        boolean result = false;
        // assume not flapping at start
        statusListener.setLastEvent(SystemClock.elapsedRealtimeNanos() - BluetoothRadioStatusListener.MIN_TURNING_ON_CALLBACK_DELAY);
        boolean onOrOff = true;
        for(int i=0; i <= 2; i++) {
            result = statusListener.shouldScheduleCallback(onOrOff ? BluetoothAdapter.STATE_ON : BluetoothAdapter.STATE_OFF,
                    onOrOff ? BluetoothAdapter.STATE_OFF : BluetoothAdapter.STATE_ON);
            statusListener.setLastEvent(SystemClock.elapsedRealtimeNanos() - (BluetoothRadioStatusListener.MIN_TURNING_OFF_CALLBACK_DELAY - (BluetoothRadioStatusListener.MIN_TURNING_OFF_CALLBACK_DELAY / 2)));
            onOrOff = !onOrOff;
        }
        assertFalse(result);
    }

    /**
     * Test typical case, bluetooth was off when app was started, user turns it on
     */
    @Test
    public void testBluetoothOnCallbackShouldHappenAfterDelay() throws InterruptedException {
        // assume not flapping at start
        // statusListener.setLastEvent(SystemClock.elapsedRealtimeNanos() - BluetoothRadioStatusListener.MIN_CALLBACK_DELAY);
        // assume BT was off
        statusListener.setCurrentState(BluetoothAdapter.STATE_OFF);
        final long startTime = SystemClock.elapsedRealtime();
        CountDownLatch cdl = new CountDownLatch(1);
        statusListener.setListener(new BluetoothRadioStatusListener.BluetoothOnListener() {
            @Override
            public void bluetoothOff() {
                fail();
            }

            @Override
            public void bluetoothOn() {
                long endTime = SystemClock.elapsedRealtime();
                assertTrue(endTime - startTime >= BluetoothRadioStatusListener.MIN_TURNING_ON_CALLBACK_DELAY);
                cdl.countDown();
            }

            @Override
            public void bluetoothTurningOff() {
                fail();
            }

            @Override
            public void bluetoothTurningOn() {
                fail();
            }
        });
        // bluetooth on intent
        Intent i = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        i.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON);
        statusListener.receiver.onReceive(ctx, i);
        cdl.await(2, TimeUnit.SECONDS);
        statusListener.removeListener();
    }

    /**
     * Test typical case, bluetooth was on when app was started, user turns it off
     */
    @Test
    public void testBluetoothOffCallbackShouldHappenWithNoDelay() throws InterruptedException {
        // assume BT was off
        statusListener.setCurrentState(BluetoothAdapter.STATE_ON);
        final long startTime = SystemClock.elapsedRealtime();
        CountDownLatch cdl = new CountDownLatch(1);
        statusListener.setListener(new BluetoothRadioStatusListener.BluetoothOnListener() {
            @Override
            public void bluetoothOff() {
                long endTime = SystemClock.elapsedRealtime();
                // should happen basically immediately
                assertTrue(endTime - startTime <= 50);
                cdl.countDown();
            }

            @Override
            public void bluetoothOn() {
                fail();
            }

            @Override
            public void bluetoothTurningOff() {
                fail();
            }

            @Override
            public void bluetoothTurningOn() {
                fail();
            }
        });
        // bluetooth on intent
        Intent i = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        i.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
        statusListener.receiver.onReceive(ctx, i);
        cdl.await(2, TimeUnit.SECONDS);
        statusListener.removeListener();
    }

    /**
     * Test typical case, bluetooth was on when app was started, user turns it off
     */
    @Test
    public void testBluetoothTurningOffToOffCallbackShouldHappenWithNoDelay() throws InterruptedException {
        // assume BT was off
        statusListener.setCurrentState(BluetoothAdapter.STATE_ON);
        final long startTime = SystemClock.elapsedRealtime();
        CountDownLatch cdl = new CountDownLatch(2);
        statusListener.setListener(new BluetoothRadioStatusListener.BluetoothOnListener() {
            @Override
            public void bluetoothOff() {
                long endTime = SystemClock.elapsedRealtime();
                // should happen basically immediately
                assertTrue(endTime - startTime <= 50);
                cdl.countDown();
            }

            @Override
            public void bluetoothOn() {
                fail();
            }

            @Override
            public void bluetoothTurningOff() {
                long endTime = SystemClock.elapsedRealtime();
                // should happen basically immediately
                assertTrue(endTime - startTime <= 50);
                cdl.countDown();
            }

            @Override
            public void bluetoothTurningOn() {
                fail();
            }
        });
        // bluetooth on intent
        Intent i = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        i.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_TURNING_OFF);
        statusListener.receiver.onReceive(ctx, i);
        i.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
        statusListener.receiver.onReceive(ctx, i);
        cdl.await(2, TimeUnit.SECONDS);
        statusListener.removeListener();
    }

    /**
     * Test typical case, bluetooth was on when app was started, user turns it off
     */
    @Test
    public void testBluetoothOffOnOffCallbackShouldHappenAfterDelay() throws InterruptedException {
        // assume BT was off
        statusListener.setCurrentState(BluetoothAdapter.STATE_ON);
        final long startTime = SystemClock.elapsedRealtime();
        CountDownLatch cdl = new CountDownLatch(1);
        statusListener.setListener(new BluetoothRadioStatusListener.BluetoothOnListener() {
            @Override
            public void bluetoothOff() {
                long endTime = SystemClock.elapsedRealtime();
                long diff = endTime - startTime;
                assertTrue(diff >= BluetoothRadioStatusListener.MIN_TURNING_OFF_CALLBACK_DELAY && diff <= (BluetoothRadioStatusListener.MIN_TURNING_OFF_CALLBACK_DELAY * 2));
                cdl.countDown();
            }

            @Override
            public void bluetoothOn() {
                fail();
            }

            @Override
            public void bluetoothTurningOff() {
                fail();
            }

            @Override
            public void bluetoothTurningOn() {
                fail();
            }
        });
        // bluetooth on intent
        Intent i = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        i.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
        statusListener.receiver.onReceive(ctx, i);
        i = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        i.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON);
        statusListener.receiver.onReceive(ctx, i);
        i = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        i.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
        statusListener.receiver.onReceive(ctx, i);
        cdl.await(2, TimeUnit.SECONDS);
        statusListener.removeListener();
    }

    /**
     * Test typical case, bluetooth was on when app was started, user turns it off
     */
    @Test
    public void testBluetoothOnOffOnOffOnCallbackShouldHappenAfterDelay() throws InterruptedException {
        // assume BT was off
        statusListener.setCurrentState(BluetoothAdapter.STATE_OFF);
        final long startTime = SystemClock.elapsedRealtime();
        CountDownLatch cdl = new CountDownLatch(1);
        final int[] onCallCount = new int[]{1};
        statusListener.setListener(new BluetoothRadioStatusListener.BluetoothOnListener() {
            @Override
            public void bluetoothOff() {
                fail();
            }

            @Override
            public void bluetoothOn() {
                long endTime = SystemClock.elapsedRealtime();
                long diff = endTime - startTime;
                assertTrue(diff >= BluetoothRadioStatusListener.MIN_TURNING_ON_CALLBACK_DELAY && diff <= (BluetoothRadioStatusListener.MIN_TURNING_ON_CALLBACK_DELAY * 2));
                onCallCount[0] = onCallCount[0]++;
                // not entirely sure if this works
                if(onCallCount[0] > 2) {
                    fail();
                } else {
                    cdl.countDown();
                }
            }

            @Override
            public void bluetoothTurningOff() {
                fail();
            }

            @Override
            public void bluetoothTurningOn() {
                fail();
            }
        });
        // bluetooth on intent
        Intent i = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        i.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON);
        statusListener.receiver.onReceive(ctx, i);
        i = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        i.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
        statusListener.receiver.onReceive(ctx, i);
        i = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        i.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON);
        statusListener.receiver.onReceive(ctx, i);
        i = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        i.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
        statusListener.receiver.onReceive(ctx, i);
        i = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        i.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON);
        statusListener.receiver.onReceive(ctx, i);
        cdl.await(2, TimeUnit.SECONDS);
        statusListener.removeListener();
    }

    /**
     * Test typical case, bluetooth was on when app was started, user turns it off
     */
    @Test
    public void testBluetoothOnOffOnOffOnCallbackOnlyDeliveredOnce() throws InterruptedException {
        // assume BT was off
        statusListener.setCurrentState(BluetoothAdapter.STATE_OFF);
        CountDownLatch cdl = new CountDownLatch(1);
        final int[] onCallCount = new int[]{1};
        statusListener.setListener(new BluetoothRadioStatusListener.BluetoothOnListener() {
            @Override
            public void bluetoothOff() {
                fail();
            }

            @Override
            public void bluetoothOn() {
                onCallCount[0] = onCallCount[0]++;
                // not entirely sure if this works
                if(onCallCount[0] > 2) {
                    fail();
                } else {
                    mockHandler.postDelayed(() -> {
                        // if we didn't get called back more than once in 500ms success!
                        assertTrue(true);
                        cdl.countDown();
                    }, 500);
                }
            }

            @Override
            public void bluetoothTurningOff() {
                fail();
            }

            @Override
            public void bluetoothTurningOn() {
                fail();
            }
        });
        // bluetooth on intent
        Intent i = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        i.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON);
        statusListener.receiver.onReceive(ctx, i);
        i = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        i.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
        statusListener.receiver.onReceive(ctx, i);
        i = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        i.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON);
        statusListener.receiver.onReceive(ctx, i);
        i = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        i.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
        statusListener.receiver.onReceive(ctx, i);
        i = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        i.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON);
        statusListener.receiver.onReceive(ctx, i);
        cdl.await(2, TimeUnit.SECONDS);
        statusListener.removeListener();
    }

    /**
     * Test typical case, bluetooth was on when app was started, user turns it off
     */
    @Test
    public void testBluetoothOffTurningOnOnCallbackShouldHappenAfterDelay() throws InterruptedException {
        // assume BT was off
        statusListener.setCurrentState(BluetoothAdapter.STATE_OFF);
        final long startTime = SystemClock.elapsedRealtime();
        CountDownLatch cdl = new CountDownLatch(2);
        statusListener.setListener(new BluetoothRadioStatusListener.BluetoothOnListener() {
            @Override
            public void bluetoothOff() {
                fail();
            }

            @Override
            public void bluetoothOn() {
                long endTime = SystemClock.elapsedRealtime();
                long diff = endTime - startTime;
                assertTrue(diff >= BluetoothRadioStatusListener.MIN_TURNING_ON_CALLBACK_DELAY && diff <= (BluetoothRadioStatusListener.MIN_TURNING_ON_CALLBACK_DELAY * 2));
                cdl.countDown();
            }

            @Override
            public void bluetoothTurningOff() {
                fail();
            }

            @Override
            public void bluetoothTurningOn() {
                long endTime = SystemClock.elapsedRealtime();
                long diff = endTime - startTime;
                assertTrue(diff <= 50);
                cdl.countDown();
            }
        });
        // bluetooth on intent
        Intent i = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        i.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_TURNING_ON);
        statusListener.receiver.onReceive(ctx, i);
        i = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        i.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON);
        statusListener.receiver.onReceive(ctx, i);
        cdl.await(2, TimeUnit.SECONDS);
        statusListener.removeListener();
    }

    /**
     * Test typical case, bluetooth was on when app was started, user turns it off
     */
    @Test
    public void testBluetoothOnTurningOffCallbackShouldHappenAfterDelay() throws InterruptedException {
        // assume BT was off
        statusListener.setCurrentState(BluetoothAdapter.STATE_ON);
        final long startTime = SystemClock.elapsedRealtime();
        CountDownLatch cdl = new CountDownLatch(2);
        statusListener.setListener(new BluetoothRadioStatusListener.BluetoothOnListener() {
            @Override
            public void bluetoothOff() {
                long endTime = SystemClock.elapsedRealtime();
                long diff = endTime - startTime;
                assertTrue(diff >= BluetoothRadioStatusListener.MIN_TURNING_OFF_CALLBACK_DELAY && diff <= (BluetoothRadioStatusListener.MIN_TURNING_OFF_CALLBACK_DELAY * 2));
                cdl.countDown();
            }

            @Override
            public void bluetoothOn() {
                fail();
            }

            @Override
            public void bluetoothTurningOff() {
                long endTime = SystemClock.elapsedRealtime();
                long diff = endTime - startTime;
                assertTrue(diff >= 50);
                cdl.countDown();
            }

            @Override
            public void bluetoothTurningOn() {
                fail();
            }
        });
        // bluetooth on intent
        Intent i = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        i.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_TURNING_OFF);
        statusListener.receiver.onReceive(ctx, i);
        i = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        i.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
        statusListener.receiver.onReceive(ctx, i);
        cdl.await(2, TimeUnit.SECONDS);
        statusListener.removeListener();
    }
}
