/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import android.app.PendingIntent;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import java.util.List;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * We place the scanner behind an interface so that we can replace the android scanner with a mock
 * scanner in unit tests
 *
 * Created by iowens on 06/11/19.
 */

public interface ScannerInterface {
    /**
     * Starts a scan with a given scan callback for delivering results
     * @param callback The scan callback
     */
    void startScan(final ScanCallback callback);

    /**
     * Starts a bluetooth scan with filters
     * @param filters Scan filters
     * @param settings Scan settings
     * @param callback The scan callback
     */
    void startScan(List<ScanFilter> filters, ScanSettings settings,
                          final ScanCallback callback);

    /**
     * Starts a scan with a list of filters, scan settings and delivers results with a pending intent
     * callback
     * @param filters Scan filters
     * @param settings Scan settings
     * @param callbackIntent The callback intent to be delivered when devices matching the scan
     *                       filters are discovered
     * @return an integer corresponding to the ScanCallback error message
     */
    int startScan(@Nullable List<ScanFilter> filters, @Nullable ScanSettings settings,
                         @NonNull PendingIntent callbackIntent);

    /**
     * Will stop the scanner
     * @param callback Will stop a scanner with the given callback ( should match )
     */
    void stopScan(ScanCallback callback);

    /**
     * Will stop the scan for the given intent
     * @param callbackIntent The callback intent to be delivered when devices are discovered
     */
    void stopScan(PendingIntent callbackIntent);

    /**
     * Internal method to the scanner to flush pending results
     * @param callback The scanner callback
     */
    void flushPendingScanResults(ScanCallback callback);

    /**
     * Will interact with the adapter to determine if BT is on / off
     * @return True if BT is on, false if it is off
     */
    boolean isBluetoothEnabled();

    /**
     * Internal method to the scanner to clean-up resources
     */
    void cleanup();
}
