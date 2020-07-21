/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.util.BluetoothUtils;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.VisibleForTesting;
import timber.log.Timber;

/**
 * Responsible for listening to the broadcasts of the bluetooth radio status on the mobile device
 * and notifying a callback.  This listener will also attempt to prevent flapping in the case that
 * the user is rapidly toggling bluetooth on and off.
 * <p>
 * Created by iowens on 8/27/18.
 */
class BluetoothRadioStatusListener {
    @VisibleForTesting
    static final long MIN_TURNING_OFF_CALLBACK_DELAY = 500;
    static final long MIN_TURNING_ON_CALLBACK_DELAY = 1000;
    private Handler mainHandler;
    private int currentState;
    private long lastEvent = SystemClock.elapsedRealtimeNanos();

    @VisibleForTesting
    BluetoothRadioStatusListener(Context context, boolean shouldInitializeListening, Looper mockMainThreadLooper) {
        this.context = context;
        // this handler is to deliver the callbacks in the same way as they would usually
        // be delivered, but we want to avoid flapping ( user toggling on and off quickly )
        // so that our protocol stacks do not get set up in a half state
        this.mainHandler = new Handler(mockMainThreadLooper);
        if (shouldInitializeListening) {
            Timber.d("Starting listener");
            startListening();
        }
        // we are testing, default to BT on
        currentState = BluetoothAdapter.STATE_ON;
    }

    /**
     * The delegate interface for changes on the radio status
     */
    interface BluetoothOnListener {
        void bluetoothOff();

        void bluetoothOn();

        void bluetoothTurningOff();

        void bluetoothTurningOn();
    }

    /**
     * The android context
     */
    Context context;
    /**
     * The delegate listener member var
     */
    BluetoothOnListener listener;
    /**
     * The android system broadcast receiver
     */
    @VisibleForTesting
    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                Timber.d("Received BluetoothState: [%s]", parseBluetoothStatus(state));
                performActionIfNecessaryAndUpdateState(state);
            }
        }

        private void performActionIfNecessaryAndUpdateState(int state) {
            // if a dev wants to know about flapping listen to turning on / off
            if (state == BluetoothAdapter.STATE_TURNING_ON || state == BluetoothAdapter.STATE_TURNING_OFF) {
                Timber.v("Turning off or turning on, passing through with no delay");
                mainHandler.post(() -> {
                    if (listener != null) {
                        switch (state) {
                            case BluetoothAdapter.STATE_TURNING_OFF:
                                listener.bluetoothTurningOff();
                                break;
                            case BluetoothAdapter.STATE_TURNING_ON:
                                listener.bluetoothTurningOn();
                                break;
                            default:
                                Timber.w("The BT radio went into a state that we do not handle");
                        }
                    }
                });
            } else {
                boolean shouldCallback = shouldScheduleCallback(currentState, state);
                currentState = state;
                if (shouldCallback) {
                    // if we got that we should callback, then we should cancel any existing pending
                    // callback before starting the new one
                    mainHandler.removeCallbacksAndMessages(null);
                    Timber.v("Clearing old messages");
                    Timber.v("BT on or off, sending after %dms", (state == BluetoothAdapter.STATE_OFF) ? MIN_TURNING_OFF_CALLBACK_DELAY : MIN_TURNING_ON_CALLBACK_DELAY);
                    mainHandler.postDelayed(() -> {
                        if (listener != null) {
                            switch (state) {
                                case BluetoothAdapter.STATE_OFF:
                                    Timber.v("Notifying off");
                                    listener.bluetoothOff();
                                    break;
                                case BluetoothAdapter.STATE_ON:
                                    Timber.v("Notifying on");
                                    listener.bluetoothOn();
                                    break;
                                default:
                                    Timber.w("The BT radio went into a state that we do not handle");
                            }
                        }
                    }, ((state == BluetoothAdapter.STATE_OFF) ? MIN_TURNING_OFF_CALLBACK_DELAY : MIN_TURNING_ON_CALLBACK_DELAY));
                } else {
                    Timber.d("Not calling back, flapping");
                }
            }
        }
    };

    @VisibleForTesting
    void setCurrentState(int currentAdapterState) {
        this.currentState = currentAdapterState;
    }

    @VisibleForTesting
    void setLastEvent(long lastEventTime) {
        this.lastEvent = lastEventTime;
    }

    /**
     * To set a delegate for listening to bluetooth adapter state changes on this class
     *
     * @param onListener The listener for status changes
     */

    void setListener(BluetoothOnListener onListener) {
        this.listener = onListener;
    }

    /**
     * Will remove the listener
     */
    void removeListener() {
        this.listener = null;
    }

    /**
     * Constructor for the listener, if desired as a convenience can start listening immediately
     *
     * @param context                   The android context
     * @param shouldInitializeListening Whether to attach to the global broadcast immediately
     */

    BluetoothRadioStatusListener(Context context, boolean shouldInitializeListening) {
        this.context = context;
        BluetoothUtils bluetoothUtils = new BluetoothUtils();
        BluetoothAdapter adapter = bluetoothUtils.getBluetoothAdapter(context);
        // if we are in this condition, something is seriously wrong
        this.currentState = (adapter != null) ? adapter.getState() : BluetoothAdapter.STATE_OFF;
        // this handler is to deliver the callbacks in the same way as they would usually
        // be delivered, but we want to avoid flapping ( user toggling on and off quickly )
        // so that our protocol stacks do not get set up in a half state
        this.mainHandler = new Handler(Looper.getMainLooper());
        if (shouldInitializeListening) {
            Timber.d("Starting listener");
            startListening();
        }
    }

    /**
     * Will start listening to the global bluetooth adapter broadcasts
     */

    void startListening() {
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.getApplicationContext().registerReceiver(receiver, filter);
    }

    /**
     * Will stop listening to the global bluetooth adapter status broadcasts
     */

    void stopListening() {
        context.getApplicationContext().unregisterReceiver(receiver);
    }

    /**
     * Will parse out the bluetooth status integer into actual text to log
     *
     * @param status The bluetooth status value from the intent
     * @return The string value of the bluetooth status response
     */

    private String parseBluetoothStatus(int status) {
        String statusString;
        switch (status) {
            case BluetoothAdapter.STATE_TURNING_ON:
                statusString = "Turning On";
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                statusString = "Turning Off";
                break;
            case BluetoothAdapter.STATE_ON:
                statusString = "On";
                break;
            case BluetoothAdapter.STATE_OFF:
                statusString = "Off";
                break;
            default:
                statusString = "Unknown";
                break;
        }
        return statusString;
    }

    /**
     * What this method does is to determine, based on a stream of inputs whether to schedule an
     * on / off callback ( we'll leave the turning on / turning off callbacks alone ).  What we want
     * is to send an off callback immediately, but only send an on callback if the state has
     * settled into an on state for one second.
     *
     * Turning off and turning on are not considered by this logic
     *
     * @param previousState The previous bt state
     * @param currentState  The current bt state
     * @return true if a callback should be scheduled based on the current state, false if nothing should occur
     */
    @VisibleForTesting
    boolean shouldScheduleCallback(int previousState, int currentState) {
        boolean shouldReturn;
        long currentRt = SystemClock.elapsedRealtimeNanos();
        // this method should not hold it's own state, but instead operate on the values provided
        // for now we will just write it out and then test the crap out of it to make sure that
        // all cases are handled.
        // check less than MIN_CALLBACK_DELAY, if less than say no, there is a minor edge case where
        // currentRt and lastEvent can be the same nanos, if this is true, just do nothing since
        // the app just launched and the toggle is already happening, this is flapping too.
        if (currentRt - lastEvent < ((currentState == BluetoothAdapter.STATE_OFF) ? MIN_TURNING_OFF_CALLBACK_DELAY : MIN_TURNING_ON_CALLBACK_DELAY)) {
            Timber.d("Time since last BT radio change is less than min, not doing anything");
            return false;
        }
        if (previousState == BluetoothAdapter.STATE_ON && currentState == BluetoothAdapter.STATE_OFF) {
            // we always want to send a message when the adapter goes off
            Timber.v("The adapter is off");
            shouldReturn = true;
        } else if (previousState == BluetoothAdapter.STATE_ON && currentState == BluetoothAdapter.STATE_ON) {
            // this is here for just completeness, realistically this should never happen
            Timber.v("The adapter was on and somehow we got on again, sad.");
            shouldReturn = false;
        } else if (previousState == BluetoothAdapter.STATE_OFF && currentState == BluetoothAdapter.STATE_OFF) {
            // likewise, this should never happen
            Timber.v("We are in a state that should never occur, this phone may have an untrustworthy BT stack");
            shouldReturn = false;
        } else if (previousState == BluetoothAdapter.STATE_OFF && currentState == BluetoothAdapter.STATE_ON) {
            Timber.v("Adapter was off and is on");
            shouldReturn = true;
        } else {
            Timber.v("Unknown state");
            shouldReturn = false;
        }
        // we want elapsed real-time millis because the user could be doing funky things with
        // SystemClock sleep or whatever
        lastEvent = SystemClock.elapsedRealtimeNanos();
        return shouldReturn;
    }
}


