/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.fitbit.bluetooth.fbgatt;

import android.app.PendingIntent;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.os.WorkSource;
import android.util.Log;
import org.mockito.stubbing.Answer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This is not fitbit code, this is designed to mock, in as close fidelity as possible the google
 * android scanner from Android Pie.  This still isn't flawless, but it should allow us to test
 * some of the harder scenarios to unit test by providing a mock adapter and a mock scanner that
 * is injectable from a mockito mock of the adapter, or just mocking the scanner if possible.
 *
 * This is a work-in-progress : 2019-07-08
 */

public class MockLollipopScanner implements ScannerInterface {

    private static final String TAG = "MockLollipopScanner";
    private static final boolean DBG = true;
    private static final boolean VDBG = false;

    private Handler mockHandler;

    private static ScheduledExecutorService singleThreadExecutor = Executors.newSingleThreadScheduledExecutor();

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

    public static class BluetoothAdapter {
        static boolean adapterOn = false;
        static IBluetoothGatt gatt = new IBluetoothGatt();

        public static boolean getAdapterState() {
            return adapterOn;
        }

        public static void turnBluetoothOff() {
            adapterOn = false;
        }

        public static void turnBluetoothOn() {
            adapterOn = true;
        }

        public static MockLollipopScanner getBluetoothLeScanner() {
            return new MockLollipopScanner();
        }

        public static IBluetoothGatt getGatt() {
            return gatt;
        }

    }

    public static final class ResultStorageDescriptor implements Parcelable {
        private int mType;
        private int mOffset;
        private int mLength;

        public int getType() {
            return mType;
        }

        public int getOffset() {
            return mOffset;
        }

        public int getLength() {
            return mLength;
        }

        /**
         * Constructor of {@link ResultStorageDescriptor}
         *
         * @param type   Type of the data.
         * @param offset Offset from start of the advertise packet payload.
         * @param length Byte length of the data
         */
        public ResultStorageDescriptor(int type, int offset, int length) {
            mType = type;
            mOffset = offset;
            mLength = length;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(mType);
            dest.writeInt(mOffset);
            dest.writeInt(mLength);
        }

        private ResultStorageDescriptor(Parcel in) {
            ReadFromParcel(in);
        }

        private void ReadFromParcel(Parcel in) {
            mType = in.readInt();
            mOffset = in.readInt();
            mLength = in.readInt();
        }

        public static final Parcelable.Creator<ResultStorageDescriptor> CREATOR =
            new Creator<ResultStorageDescriptor>() {
                @Override
                public ResultStorageDescriptor createFromParcel(Parcel source) {
                    return new ResultStorageDescriptor(source);
                }

                @Override
                public ResultStorageDescriptor[] newArray(int size) {
                    return new ResultStorageDescriptor[size];
                }
            };
    }

    /**
     * Extra containing a list of ScanResults. It can have one or more results if there was no
     * error. In case of error, {@link #EXTRA_ERROR_CODE} will contain the error code and this
     * extra will not be available.
     */
    public static final String EXTRA_LIST_SCAN_RESULT =
        "android.bluetooth.le.extra.LIST_SCAN_RESULT";

    /**
     * Optional extra indicating the error code, if any. The error code will be one of the
     * SCAN_FAILED_* codes in {@link ScanCallback}.
     */
    public static final String EXTRA_ERROR_CODE = "android.bluetooth.le.extra.ERROR_CODE";

    /**
     * Optional extra indicating the callback type, which will be one of
     * CALLBACK_TYPE_* constants in {@link ScanSettings}.
     *
     * @see ScanCallback#onScanResult(int, ScanResult)
     */
    public static final String EXTRA_CALLBACK_TYPE = "android.bluetooth.le.extra.CALLBACK_TYPE";

    private Handler mHandler;

    private final Map<ScanCallback, BleScanCallbackWrapper> mLeScanClients;

    /**
     * Use {@link BluetoothAdapter#getBluetoothLeScanner()} instead.
     *
     */
    public MockLollipopScanner() {
        Looper mockMainThreadLooper = mock(Looper.class);
        Thread mockMainThread = mock(Thread.class);
        when(mockMainThread.getName()).thenReturn("Irvin's mock thread");
        when(mockMainThreadLooper.getThread()).thenReturn(mockMainThread);
        mockHandler = mock(Handler.class);
        doAnswer(handlerPostAnswer).when(mockHandler).post(any(Runnable.class));
        doAnswer(handlerPostAnswer).when(mockHandler).postDelayed(any(Runnable.class), anyLong());
        when(mockHandler.getLooper()).thenReturn(mockMainThreadLooper);
        mHandler = mockHandler;
        mLeScanClients = new HashMap<ScanCallback, BleScanCallbackWrapper>();
    }

    /**
     * Start Bluetooth LE scan with default parameters and no filters. The scan results will be
     * delivered through {@code callback}. For unfiltered scans, scanning is stopped on screen
     * off to save power. Scanning is resumed when screen is turned on again. To avoid this, use
     * {@link #startScan(List, ScanSettings, ScanCallback)} with desired {@link ScanFilter}.
     * <p>
     * An app must hold
     * {@link android.Manifest.permission#ACCESS_COARSE_LOCATION ACCESS_COARSE_LOCATION} or
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION ACCESS_FINE_LOCATION} permission
     * in order to get results.
     *
     * @param callback Callback used to deliver scan results.
     */
    @Override
    public void startScan(final ScanCallback callback) {
        startScan(null, new ScanSettings.Builder().build(), callback);
    }

    /**
     * Start Bluetooth LE scan. The scan results will be delivered through {@code callback}.
     * For unfiltered scans, scanning is stopped on screen off to save power. Scanning is
     * resumed when screen is turned on again. To avoid this, do filetered scanning by
     * using proper {@link ScanFilter}.
     * <p>
     * An app must hold
     * {@link android.Manifest.permission#ACCESS_COARSE_LOCATION ACCESS_COARSE_LOCATION} or
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION ACCESS_FINE_LOCATION} permission
     * in order to get results.
     *
     * @param filters  {@link ScanFilter}s for finding exact BLE devices.
     * @param settings Settings for the scan.
     * @param callback Callback used to deliver scan results.
     */
    @Override
    public void startScan(List<ScanFilter> filters, ScanSettings settings,
                          final ScanCallback callback) {
        startScan(filters, settings, null, callback, /*callbackIntent=*/ null, null);
    }

    /**
     * Start Bluetooth LE scan using a {@link PendingIntent}. The scan results will be delivered via
     * the PendingIntent. Use this method of scanning if your process is not always running and it
     * should be started when scan results are available.
     * <p>
     * An app must hold
     * {@link android.Manifest.permission#ACCESS_COARSE_LOCATION ACCESS_COARSE_LOCATION} or
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION ACCESS_FINE_LOCATION} permission
     * in order to get results.
     * <p>
     * When the PendingIntent is delivered, the Intent passed to the receiver or activity
     * will contain one or more of the extras {@link #EXTRA_CALLBACK_TYPE},
     * {@link #EXTRA_ERROR_CODE} and {@link #EXTRA_LIST_SCAN_RESULT} to indicate the result of
     * the scan.
     *
     * @param filters        Optional list of ScanFilters for finding exact BLE devices.
     * @param settings       Optional settings for the scan.
     * @param callbackIntent The PendingIntent to deliver the result to.
     * @return Returns 0 for success or an error code from {@link ScanCallback} if the scan request
     * could not be sent.
     * @see #stopScan(PendingIntent)
     */

    public int startScan(@Nullable List<ScanFilter> filters, @Nullable ScanSettings settings,
                         @NonNull PendingIntent callbackIntent) {
        return startScan(filters,
            settings != null ? settings : new ScanSettings.Builder().build(),
            null, null, callbackIntent, null);
    }

    private int startScan(List<ScanFilter> filters, ScanSettings settings,
                          final WorkSource workSource, final ScanCallback callback,
                          final PendingIntent callbackIntent,
                          List<List<ResultStorageDescriptor>> resultStorages) {
        if (!BluetoothAdapter.getAdapterState()) {
            throw new IllegalStateException("Bluetooth off");
        }
        if (callback == null && callbackIntent == null) {
            throw new IllegalArgumentException("callback is null");
        }
        if (settings == null) {
            throw new IllegalArgumentException("settings is null");
        }
        synchronized (mLeScanClients) {
            if (callback != null && mLeScanClients.containsKey(callback)) {
                return postCallbackErrorOrReturn(callback,
                    ScanCallback.SCAN_FAILED_ALREADY_STARTED);
            }
            IBluetoothGatt gatt = new IBluetoothGatt();
            if (!isSettingsConfigAllowedForScan(settings)) {
                return postCallbackErrorOrReturn(callback,
                    ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED);
            }
            if (!isHardwareResourcesAvailableForScan(settings)) {
                return postCallbackErrorOrReturn(callback,
                    5);
            }
            if (!isSettingsAndFilterComboAllowed(settings, filters)) {
                return postCallbackErrorOrReturn(callback,
                    ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED);
            }
            if (callback != null) {
                BleScanCallbackWrapper wrapper = new BleScanCallbackWrapper(gatt, filters,
                    settings, workSource, callback, resultStorages);
                wrapper.startRegistration();
            } else {
                gatt.startScanForIntent(callbackIntent, settings, filters,
                    "com.fitbit.bluetooth.fbgatt");
            }
        }
        return 0;
    }

    /**
     * Stops an ongoing Bluetooth LE scan.
     *
     * @param callback
     */

    public void stopScan(ScanCallback callback) {
        if (!BluetoothAdapter.getAdapterState()) {
            throw new IllegalStateException("Bluetooth off");
        }
        synchronized (mLeScanClients) {
            BleScanCallbackWrapper wrapper = mLeScanClients.remove(callback);
            if (wrapper == null) {
                if (DBG) Log.d(TAG, "could not find callback wrapper");
                return;
            }
            wrapper.stopLeScan();
        }
    }

    /**
     * Stops an ongoing Bluetooth LE scan started using a PendingIntent.
     *
     * @param callbackIntent The PendingIntent that was used to start the scan.
     * @see #startScan(List, ScanSettings, PendingIntent)
     */

    public void stopScan(PendingIntent callbackIntent) {
        if (!BluetoothAdapter.getAdapterState()) {
            throw new IllegalStateException("Bluetooth off");
        }
        IBluetoothGatt gatt = BluetoothAdapter.getGatt();
        gatt.stopScanForIntent(callbackIntent, "com.fitbit.bluetooth.fbgatt");
    }

    /**
     * Flush pending batch scan results stored in Bluetooth controller. This will return Bluetooth
     * LE scan results batched on bluetooth controller. Returns immediately, batch scan results data
     * will be delivered through the {@code callback}.
     *
     * @param callback Callback of the Bluetooth LE Scan, it has to be the same instance as the one
     *                 used to start scan.
     */
    public void flushPendingScanResults(ScanCallback callback) {
        if (!BluetoothAdapter.getAdapterState()) {
            throw new IllegalStateException("Bluetooth off");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback cannot be null!");
        }
        synchronized (mLeScanClients) {
            BleScanCallbackWrapper wrapper = mLeScanClients.get(callback);
            if (wrapper == null) {
                return;
            }
            wrapper.flushPendingBatchResults();
        }
    }

    @Override
    public boolean isBluetoothEnabled() {
        return BluetoothAdapter.getAdapterState();
    }


    /**
     * Cleans up scan clients. Should be called when bluetooth is down.
     */
    public void cleanup() {
        mLeScanClients.clear();
    }

    private static class IBluetoothGatt {
        private static final long TIME_BETWEEN_RESULTS = 1000;
        HashMap<BleScanCallbackWrapper, WorkSource> scanners = new HashMap<>();
        HashMap<Integer, ScanState> startedScanners = new HashMap<>();
        private MockScanResultProvider provider = new MockScanResultProvider(5, -145, -11);
        private ScanState currentScanState = ScanState.NOT_SCANNING;

        private Runnable resultsRunnable = new Runnable() {


            @Override
            public void run() {
                for (BleScanCallbackWrapper wrapper : IBluetoothGatt.this.scanners.keySet()) {
                    List<ScanResult> results = provider.getAllResults();
                    for (ScanResult result : results) {
                        wrapper.onScanResult(result);
                    }
                }
            }
        };

        private Runnable intentDeliveryRunnable = new Runnable() {
            @Override
            public void run() {
                for (BleScanCallbackWrapper wrapper : IBluetoothGatt.this.scanners.keySet()) {
                    List<ScanResult> results = provider.getAllResults();
                    for (ScanResult result : results) {
                        wrapper.onScanResult(result);
                    }
                }
            }
        };

        @SuppressWarnings("FutureReturnValueIgnored")
        public void startScanForIntent(PendingIntent callbackIntent, ScanSettings settings, List<ScanFilter> filters, String packageName) {
            singleThreadExecutor.schedule(resultsRunnable, TIME_BETWEEN_RESULTS, TimeUnit.MILLISECONDS);
            if (settings.getScanMode() == ScanSettings.SCAN_MODE_LOW_LATENCY) {
                currentScanState = ScanState.INTENT_SCANNING;
            } else if (settings.getScanMode() == ScanSettings.SCAN_MODE_BALANCED) {
                currentScanState = ScanState.INTENT_SCANNING;
            } else if (settings.getScanMode() == ScanSettings.SCAN_MODE_LOW_POWER) {
                currentScanState = ScanState.INTENT_SCANNING;
            }
        }

        public void stopScanForIntent(PendingIntent callbackIntent, String packageName) {
            /*
            Set<Integer> keys = startedScanners.keySet();
            for (Integer key : keys) {
                if (key == scanId) {
                    Timber.v("Cancelling scanner with id: %d", scanId);
                    startedScanners.remove(scanId);
                }
            }
            */
        }

        public void unregisterClient(int scannerId) {
            Set<BleScanCallbackWrapper> keys = scanners.keySet();
            for (BleScanCallbackWrapper key : keys) {
                if (key.mScannerId == scannerId) {
                    scanners.remove(key);
                }
            }
        }

        public void startScan(int mScannerId, ScanSettings mSettings, List<ScanFilter> mFilters, List<List<ResultStorageDescriptor>> mResultStorages, String packageName) {
            if (mSettings.getScanMode() == ScanSettings.SCAN_MODE_LOW_LATENCY) {
                currentScanState = ScanState.LOW_LATENCY;
            } else if (mSettings.getScanMode() == ScanSettings.SCAN_MODE_BALANCED) {
                currentScanState = ScanState.BALANCED_LATENCY;
            } else if (mSettings.getScanMode() == ScanSettings.SCAN_MODE_LOW_POWER) {
                currentScanState = ScanState.HIGH_LATENCY;
            }
            SystemClock.sleep(TIME_BETWEEN_RESULTS);
            resultsRunnable.run();
        }

        private enum ScanState {
            NOT_SCANNING,
            HIGH_LATENCY,
            BALANCED_LATENCY,
            LOW_LATENCY,
            INTENT_SCANNING;
        }

        public void registerScanner(BleScanCallbackWrapper scanWrapper, WorkSource workSource) {
            scanners.put(scanWrapper, workSource);
            int i = 0;
            for (BleScanCallbackWrapper wrapper : scanners.keySet()) {
                if (wrapper.equals(scanWrapper)) {
                    break;
                }
                i++;
            }
            scanWrapper.onScannerRegistered(0, i);
        }

        public void stopScan(int scanId) {
            Set<Integer> keys = startedScanners.keySet();
            for (Integer key : keys) {
                if (key == scanId) {
                    startedScanners.remove(scanId);
                }
            }
        }

        public void unregisterScanner(int scannerId) {
            Set<BleScanCallbackWrapper> keys = scanners.keySet();
            for (BleScanCallbackWrapper key : keys) {
                if (key.mScannerId == scannerId) {
                    scanners.remove(key);
                }
            }
        }

        public void flushPendingBatchResults(int scannerId) {
            List<ScanResult> results = provider.getAllResults();
            Set<BleScanCallbackWrapper> keys = scanners.keySet();
            BleScanCallbackWrapper wrapper = null;
            for (BleScanCallbackWrapper key : keys) {
                if (key.mScannerId == scannerId) {
                    wrapper = key;
                    break;
                }
            }
            if (wrapper != null) {
                wrapper.onBatchScanResults(results);
            }
        }
    }

    /**
     * Bluetooth GATT interface callbacks
     */
    private class BleScanCallbackWrapper {
        private static final int REGISTRATION_CALLBACK_TIMEOUT_MILLIS = 2000;

        private final ScanCallback mScanCallback;
        private final List<ScanFilter> mFilters;
        private final WorkSource mWorkSource;
        private ScanSettings mSettings;
        private IBluetoothGatt mBluetoothGatt;
        private List<List<ResultStorageDescriptor>> mResultStorages;

        // mLeHandle 0: not registered
        // -2: registration failed because app is scanning to frequently
        // -1: scan stopped or registration failed
        // > 0: registered and scan started
        private int mScannerId;

        public BleScanCallbackWrapper(IBluetoothGatt bluetoothGatt,
                                      List<ScanFilter> filters, ScanSettings settings,
                                      WorkSource workSource, ScanCallback scanCallback,
                                      List<List<ResultStorageDescriptor>> resultStorages) {
            mBluetoothGatt = bluetoothGatt;
            mFilters = filters;
            mSettings = settings;
            mWorkSource = workSource;
            mScanCallback = scanCallback;
            mScannerId = 0;
            mResultStorages = resultStorages;
        }

        public void startRegistration() {
            synchronized (this) {
                // Scan stopped.
                if (mScannerId == -1 || mScannerId == -2) return;
                try {
                    mBluetoothGatt.registerScanner(this, mWorkSource);
                    wait(REGISTRATION_CALLBACK_TIMEOUT_MILLIS);
                } catch (InterruptedException e) {
                    postCallbackError(mScanCallback, ScanCallback.SCAN_FAILED_INTERNAL_ERROR);
                }
                if (mScannerId > 0) {
                    mLeScanClients.put(mScanCallback, this);
                } else {
                    // Registration timed out or got exception, reset RscannerId to -1 so no
                    // subsequent operations can proceed.
                    if (mScannerId == 0) mScannerId = -1;

                    // If scanning too frequently, don't report anything to the app.
                    if (mScannerId == -2) return;

                    postCallbackError(mScanCallback,
                        ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED);
                }
            }
        }

        public void stopLeScan() {
            synchronized (this) {
                if (mScannerId <= 0) {
                    Log.e(TAG, "Error state, mLeHandle: " + mScannerId);
                    return;
                }
                mBluetoothGatt.stopScan(mScannerId);
                mBluetoothGatt.unregisterScanner(mScannerId);
                mScannerId = -1;
            }
        }

        void flushPendingBatchResults() {
            synchronized (this) {
                if (mScannerId <= 0) {
                    Log.e(TAG, "Error state, mLeHandle: " + mScannerId);
                    return;
                }
                mBluetoothGatt.flushPendingBatchResults(mScannerId);
            }
        }

        /**
         * Application interface registered - app is ready to go
         */

        public void onScannerRegistered(int status, int scannerId) {
            Log.d(TAG, "onScannerRegistered() - status=" + status
                + " scannerId=" + scannerId + " mScannerId=" + mScannerId);
            synchronized (this) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (mScannerId == -1) {
                        // Registration succeeds after timeout, unregister client.
                        mBluetoothGatt.unregisterClient(scannerId);
                    } else {
                        mScannerId = scannerId;
                        mBluetoothGatt.startScan(mScannerId, mSettings, mFilters,
                            mResultStorages,
                            "com.fitbit.bluetooth.fbgatt");
                    }
                } else if (status == 0x06) {
                    // applicaiton was scanning too frequently
                    mScannerId = -2;
                } else {
                    // registration failed
                    mScannerId = -1;
                }
                notifyAll();
            }
        }

        /**
         * Callback reporting an LE scan result.
         */

        public void onScanResult(final ScanResult scanResult) {
            if (VDBG) Log.d(TAG, "onScanResult() - " + scanResult.toString());

            // Check null in case the scan has been stopped
            synchronized (this) {
                if (mScannerId <= 0) return;
            }
            Handler handler = mHandler;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    mScanCallback.onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, scanResult);
                }
            });
        }


        public void onBatchScanResults(final List<ScanResult> results) {
            Handler handler = mHandler;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    mScanCallback.onBatchScanResults(results);
                }
            });
        }


        public void onFoundOrLost(final boolean onFound, final ScanResult scanResult) {
            if (VDBG) {
                Log.d(TAG, "onFoundOrLost() - onFound = " + onFound + " " + scanResult.toString());
            }

            // Check null in case the scan has been stopped
            synchronized (this) {
                if (mScannerId <= 0) {
                    return;
                }
            }
            Handler handler = mHandler;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (onFound) {
                        mScanCallback.onScanResult(ScanSettings.CALLBACK_TYPE_FIRST_MATCH,
                            scanResult);
                    } else {
                        mScanCallback.onScanResult(ScanSettings.CALLBACK_TYPE_MATCH_LOST,
                            scanResult);
                    }
                }
            });
        }


        public void onScanManagerErrorCallback(final int errorCode) {
            if (VDBG) {
                Log.d(TAG, "onScanManagerErrorCallback() - errorCode = " + errorCode);
            }
            synchronized (this) {
                if (mScannerId <= 0) {
                    return;
                }
            }
            postCallbackError(mScanCallback, errorCode);
        }
    }

    private int postCallbackErrorOrReturn(final ScanCallback callback, final int errorCode) {
        if (callback == null) {
            return errorCode;
        } else {
            postCallbackError(callback, errorCode);
            return 0;
        }
    }

    private void postCallbackError(final ScanCallback callback, final int errorCode) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onScanFailed(errorCode);
            }
        });
    }

    private boolean isSettingsConfigAllowedForScan(ScanSettings settings) {

        final int callbackType = settings.getCallbackType();
        // Only support regular scan if no offloaded filter support.
        if (callbackType == ScanSettings.CALLBACK_TYPE_ALL_MATCHES
            && settings.getReportDelayMillis() == 0) {
            return true;
        }
        return false;
    }

    private static final ScanFilter EMPTY = new ScanFilter.Builder().build();

    private boolean isSettingsAndFilterComboAllowed(ScanSettings settings,
                                                    List<ScanFilter> filterList) {
        final int callbackType = settings.getCallbackType();
        // If onlost/onfound is requested, a non-empty filter is expected
        if ((callbackType & (ScanSettings.CALLBACK_TYPE_FIRST_MATCH
            | ScanSettings.CALLBACK_TYPE_MATCH_LOST)) != 0) {
            if (filterList == null) {
                return false;
            }
            for (ScanFilter filter : filterList) {
                if (filter.equals(EMPTY)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isHardwareResourcesAvailableForScan(ScanSettings settings) {
        return true;
    }
}
