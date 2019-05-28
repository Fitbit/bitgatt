/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.VisibleForTesting;

import timber.log.Timber;

/**
 * Responsible for listening to the broadcasts of the bluetooth radio status on the mobile device
 * and notifying a callback
 *
 * Created by iowens on 8/27/18.
 */
class BluetoothRadioStatusListener {
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
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        if(listener != null){
                            listener.bluetoothOff();
                        }
                        break;
                    case BluetoothAdapter.STATE_ON:
                        if(listener != null) {
                            listener.bluetoothOn();
                        }
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        if(listener != null) {
                            listener.bluetoothTurningOff();
                        }
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        if(listener != null) {
                            listener.bluetoothTurningOn();
                        }
                        break;
                    default:
                        Timber.w("The BT radio went into a state that we do not handle");
                }
            }
        }
    };

    /**
     * To set a delegate for listening to bluetooth adapter state changes on this class
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
     * @param context The android context
     * @param shouldInitializeListening Whether to attach to the global broadcast immediately
     */

    BluetoothRadioStatusListener(Context context, boolean shouldInitializeListening) {
        this.context = context;
        if(shouldInitializeListening) {
            Timber.d("Starting listener");
            startListening();
        }
    }

    /**
     * Will start listening to the global bluetooth adapter broadcasts
     */

    void startListening(){
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.getApplicationContext().registerReceiver(receiver, filter);
    }

    /**
     * Will stop listening to the global bluetooth adapter status broadcasts
     */

    void stopListening(){
        context.getApplicationContext().unregisterReceiver(receiver);
    }

    /**
     * Will parse out the bluetooth status integer into actual text to log
     * @param status The bluetooth status value from the intent
     * @return The string value of the bluetooth status response
     */

    private String parseBluetoothStatus(int status) {
        String statusString;
        switch(status) {
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
}


