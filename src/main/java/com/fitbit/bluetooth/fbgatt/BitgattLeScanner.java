/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.util.GattUtils;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import java.util.List;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import timber.log.Timber;

/**
 * Concrete implementation of the Android scanner
 * <p>
 * Created by iowens on 07/08/19.
 */

class BitgattLeScanner implements ScannerInterface {

    @Nullable
    private final BluetoothAdapter adapter;

    @VisibleForTesting
    BitgattLeScanner(@Nullable Context context, GattUtils gattUtils) {
        if (context != null) {
            this.adapter = gattUtils.getBluetoothAdapter(context);
        } else {
            this.adapter = null;
            Timber.w("The context was null");
        }
    }

    /**
     * Provides a context for use with manipulating the adapter
     *
     * @param context The android context
     */
    BitgattLeScanner(@Nullable Context context) {
        this(context, new GattUtils());
    }

    @Override
    public void startScan(ScanCallback callback) {
        BluetoothLeScanner leScanner = getScanner();
        if (leScanner == null) {
            Timber.w("The scanner was null, context or adapter was null");
            return;
        }
        leScanner.startScan(callback);
    }

    @Nullable
    private BluetoothLeScanner getScanner() {
        // Normaly we should not check if ble is enabled due to the underlying implementation
        // which check for bluetooth state. This is added to ensure consistency on all OEM's
        if (adapter != null && adapter.getBluetoothLeScanner() != null && isBluetoothEnabled()) {
            return adapter.getBluetoothLeScanner();
        }
        return null;
    }

    @Override
    public void startScan(List<ScanFilter> filters, ScanSettings settings, ScanCallback callback) {
        BluetoothLeScanner leScanner = getScanner();
        if (leScanner == null) {
            Timber.w("The scanner was null, context or adapter was null");
            return;
        }
        leScanner.startScan(filters, settings, callback);
    }

    @TargetApi(Build.VERSION_CODES.O)
    @Override
    public int startScan(@Nullable List<ScanFilter> filters, @Nullable ScanSettings settings, @NonNull PendingIntent callbackIntent) {
        BluetoothLeScanner leScanner = getScanner();
        if(leScanner == null || !FitbitGatt.atLeastSDK(Build.VERSION_CODES.O)) {
            Timber.w("The scanner was null, context or adapter was null");
            return ScanCallback.SCAN_FAILED_INTERNAL_ERROR;
        }
        return leScanner.startScan(filters, settings, callbackIntent);
    }

    @Override
    public void stopScan(ScanCallback callback) {
        BluetoothLeScanner leScanner = getScanner();
        if (leScanner == null) {
            Timber.w("The scanner was null, context or adapter was null");
            return;
        }
        leScanner.stopScan(callback);
    }

    @TargetApi(Build.VERSION_CODES.O)
    @Override
    public void stopScan(PendingIntent callbackIntent) {
        BluetoothLeScanner leScanner = getScanner();
        if (leScanner == null || !FitbitGatt.atLeastSDK(Build.VERSION_CODES.O)) {
            Timber.w("The scanner was null, the context or adapter must have been null");
            return;
        }
        leScanner.stopScan(callbackIntent);
    }

    @Override
    public void flushPendingScanResults(ScanCallback callback) {
        BluetoothLeScanner leScanner = getScanner();
        if(leScanner == null) {
            Timber.w("The scanner was null, the context or adpater also must have been null");
            return;
        }
        leScanner.flushPendingScanResults(callback);
    }

    @Override
    public boolean isBluetoothEnabled() {
        if(adapter != null) {
            return adapter.isEnabled();
        } else {
            return false;
        }
    }

    @Override
    public void cleanup() {
        // no implementation in concrete class
    }
}
