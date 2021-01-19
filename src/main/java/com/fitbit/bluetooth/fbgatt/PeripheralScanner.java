/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.util.BluetoothUtils;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import timber.log.Timber;
import static android.bluetooth.le.ScanCallback.SCAN_FAILED_ALREADY_STARTED;
import static android.bluetooth.le.ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED;
import static android.bluetooth.le.ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED;
import static android.bluetooth.le.ScanCallback.SCAN_FAILED_INTERNAL_ERROR;
import static com.fitbit.bluetooth.fbgatt.FitbitGatt.atLeastSDK;

/**
 * A Scanner for Bluetooth LE Devices
 */
@TargetApi(21)
class PeripheralScanner {

    final static long SCAN_DURATION = TimeUnit.SECONDS.toMillis(120);
    private final static long SCAN_INTERVAL = SCAN_DURATION * 2;
    final static long SCAN_TOO_MUCH_WARN_INTERVAL = TimeUnit.SECONDS.toMillis(30);
    final static long TEST_SCAN_DURATION = TimeUnit.SECONDS.toMillis(2);
    final static long TEST_SCAN_INTERVAL = TEST_SCAN_DURATION * 2;
    private final static int MAX_BACKOFF_MULTIPLIER = 2; // should always correspond to roughly 16 minutes worst case
    static final int BACKGROUND_SCAN_REQUEST_CODE = 21436;
    static final String SCANNED_DEVICE_ACTION = "com.fitbit.bluetooth.fbgatt.ScannedDevice";
    private static final int MAX_SCANS_ALLOWED_PER_30_SECONDS = 4;
    private static final int DEFAULT_SCAN_BACKOFF_MULTIPLIER = 1;

    Handler mHandler;
    @VisibleForTesting
    final Runnable scanTimeoutRunnable = new ScanTimeoutRunnable();
    private final Runnable periodicRunnable = new PeriodicScanRunnable();
    private ScannerInterface scanner;
    private boolean stopPeriodicalScan;
    private int scanBackoffMultiplier = 1;
    private int minRssi = Integer.MIN_VALUE;
    private final ArrayList<ScanFilter> scanFilters = new ArrayList<>(1);
    private final AtomicInteger scanCount;
    AtomicBoolean isScanning;
    private final AtomicBoolean pendingIntentIsScanning;
    AtomicBoolean periodicalScanEnabled;
    private final ScanCallback callback;
    private final TrackerScannerListener listener;
    private final BluetoothUtils bleUtils;
    private boolean instrumentationTestMode;

    private final Map<String, BluetoothDevice> foundDevices = new HashMap<>();
    private boolean resetScanBackoff;
    private PendingIntent backgroundIntentBasedScanIntent;
    private final FitbitGatt fbGatt;

    private ScanSettings scanSettings = new ScanSettings
            .Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build();

    interface TrackerScannerListener {
        void onScanStatusChanged(boolean isScanning);

        void onFitbitDeviceFound(FitbitBluetoothDevice device);

        void onPendingIntentScanStatusChanged(boolean isScanning);
    }

    private Runnable resetScanCounter = new Runnable() {
        @Override
        public void run() {
            long scannerTime = getScannerDuration();
            Timber.v("Resetting scan too much counter, %s ms have gone by.", scannerTime);
            scanCount.set(0);
            mHandler.postDelayed(resetScanCounter, SCAN_TOO_MUCH_WARN_INTERVAL);
        }

    };

    PeripheralScanner(@NonNull TrackerScannerListener listener, @NonNull FitbitGatt fbGatt) {
        this.listener = listener;
        this.fbGatt = fbGatt;
        isScanning = new AtomicBoolean(false);
        pendingIntentIsScanning = new AtomicBoolean(false);
        periodicalScanEnabled = new AtomicBoolean(false);
        // you can't call start / stop more than 5 times in 30 seconds, if you do
        // the system will convert your scan to opportunistic so you'll have to wait for something
        // else to scan and back off your scan interval to 3120ms.  It will also silently
        // fail any new scans, so we'll track it with this handy counter.
        scanCount = new AtomicInteger(0);
        // we can use the main looper because the scan command doesn't block
        mHandler = new Handler(fbGatt.getAppContext().getMainLooper());
        // we can just run this every 30s, if the caller doesn't do anything wrong it should never
        // exceed five ... once it gets to 4 don't let the user start another one
        mHandler.postDelayed(resetScanCounter, SCAN_TOO_MUCH_WARN_INTERVAL);
        bleUtils = new BluetoothUtils();
        scanner = new BitgattLeScanner(fbGatt.getAppContext());
        callback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                BluetoothDevice device = result.getDevice();
                FitbitBluetoothDevice dev = new FitbitBluetoothDevice(result.getDevice());
                if (minRssi == Integer.MIN_VALUE || minRssi < result.getRssi()) {
                    if (!foundDevices.containsKey(device.getAddress())) {
                        foundDevices.put(device.getAddress(), device);
                        resetScanBackoff = true;
                    }
                    dev.setRssi(result.getRssi());
                    dev.origin = FitbitBluetoothDevice.DeviceOrigin.SCANNED;
                    dev.setScanRecord(result.getScanRecord());
                    listener.onFitbitDeviceFound(dev);
                } else {
                    Timber.v("Scanned device %s below RSSI threshold", dev);
                }

            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                for (ScanResult result : results) {
                    FitbitBluetoothDevice dev = new FitbitBluetoothDevice(result.getDevice());
                    if (minRssi == Integer.MIN_VALUE || minRssi < result.getRssi()) {
                        dev.origin = FitbitBluetoothDevice.DeviceOrigin.SCANNED;
                        dev.setRssi(result.getRssi());
                        dev.setScanRecord(result.getScanRecord());
                        listener.onFitbitDeviceFound(dev);
                    } else {
                        Timber.v("Scanned device %s below RSSI threshold", dev);
                    }
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Timber.w("onScanFailed %s", ScanFailure.getFailureForReason(errorCode));
                isScanning.set(false);
                listener.onScanStatusChanged(isScanning.get());
                if (!fbGatt.isBluetoothOn()) {
                    Timber.v("Bluetooth was off, releasing the scanner");
                }
            }
        };
    }

    private long getScannerDuration() {
        return instrumentationTestMode ? TEST_SCAN_DURATION : SCAN_DURATION;
    }

    private long getScannerInterval() {
        return instrumentationTestMode ? TEST_SCAN_INTERVAL : SCAN_INTERVAL;
    }

    void setInstrumentationTestMode(boolean instrumentationTestMode) {
        this.instrumentationTestMode = instrumentationTestMode;
    }

    void setIsPendingIntentScanning(boolean isStillPendingIntentScanning) {
        this.pendingIntentIsScanning.set(isStillPendingIntentScanning);
    }

    void setHandler(Handler mockHandler) {
        this.mHandler = mockHandler;
    }

    /**
     * Stops the scans and releases any resources.
     */
    void onDestroy(Context context) {
        cancelPeriodicalScan(context);
        cancelScan(context);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    void injectMockScanner(ScannerInterface scanner) {
        this.scanner = scanner;
    }

    /**
     * Can be used if directly managing scan filters is required
     *
     * @param scanFilters The scan filters to be applied to the scan
     */

    void setScanFilters(List<ScanFilter> scanFilters) {
        synchronized (this.scanFilters) {
            this.scanFilters.clear();
            this.scanFilters.addAll(scanFilters);
        }
    }

    /**
     * This will start periodic scan with low power mode, please note that this will return false
     * if there is already a high-priority scan going on.
     *
     * @return true if scan started, false if not
     */
    synchronized boolean startPeriodicScan(@Nullable Context context) {
        if (context == null) {
            Timber.v("Can't start a high priority scan with a null context");
            return false;
        }
        if (stopPeriodicalScan || isScanning.get()) {
            Timber.v("Not starting periodical scan: isScanning: %b, was stopPeriodicalScan requested? %b", isScanning.get(), stopPeriodicalScan);
            return false;
        }
        Timber.d("Start Periodic Scan");
        scanBackoffMultiplier = DEFAULT_SCAN_BACKOFF_MULTIPLIER;
        periodicalScanEnabled.set(true);
        return startScan(context);
    }

    /**
     * This will start a high priority scan
     *
     * @return true if scan started, false if not
     */
    synchronized boolean startHighPriorityScan(@Nullable Context context) {
        if (context == null) {
            Timber.v("Can't start a high priority scan with a null context");
            return false;
        }
        if (isScanning.get()) {
            cancelScan(context);
        }
        Timber.d("Start High priority Scan");
        return startScan(context);
    }

    /**
     * This will cancel any current scans and will not stop the periodic ones.
     */
    synchronized void cancelScan(@Nullable Context context) {
        if (context == null) {
            Timber.v("Can't cancel the scan with a null context");
            return;
        }
        Timber.d("StopScanning requested ");
        stopScan(context);
        mHandler.removeCallbacks(scanTimeoutRunnable);
        mHandler.removeCallbacks(periodicRunnable);
    }

    /**
     * This will stop any high priority scans that are in progress, but will not cancel any
     * periodical scans that are already in progress.  If there are none, this is equivalent
     * to cancelScan(Context context).
     */

    synchronized void cancelHighPriorityScan(@Nullable Context context) {
        if (context == null) {
            Timber.v("Can't cancel the scan with a null context");
            return;
        }
        Timber.d("Stop high-priority scan requested");
        // if there was a periodical scan enabled we will want to make sure it is restarted after
        // we stop the scan
        if (periodicalScanEnabled.get()) {
            stopScan(context);
            mHandler.removeCallbacks(scanTimeoutRunnable);
            mHandler.removeCallbacks(periodicRunnable);
            mHandler.post(periodicRunnable);
        } else {
            cancelScan(context);
        }
    }

    /**
     * This will stop any future periodical scans that are happening, but will not affect any scans
     * that are already in progress, high-priority or otherwise.
     */

    synchronized void cancelPeriodicalScan(@Nullable Context context) {
        if (context == null) {
            Timber.v("Can't cancel the scan with a null context");
            return;
        }
        // set scan multipliers back to default
        scanBackoffMultiplier = DEFAULT_SCAN_BACKOFF_MULTIPLIER;
        periodicalScanEnabled.set(false);
        mHandler.removeCallbacks(scanTimeoutRunnable);
        mHandler.removeCallbacks(periodicRunnable);
        // if we are presently actively scanning we will let that scan run it's course and then
        // let it call onScanStatusChanged, otherwise we will fire it from here so the caller knows
        if (!isScanning()) {
            listener.onScanStatusChanged(isScanning());
        }
    }

    /**
     * Set Filters by device name, will clear and reset the filters such that the next periodical
     * or high priority scan will pick them up
     *
     * @param deviceNameFilters The bluetooth peripheral names to limit the filter for
     */
    void setDeviceNameFilters(List<String> deviceNameFilters) {
        synchronized (scanFilters) {
            scanFilters.clear();
            if (deviceNameFilters != null) {
                for (String name : deviceNameFilters) {
                    scanFilters.add(new ScanFilter.Builder().setDeviceName(name).build());
                }
            }
        }
    }

    /**
     * Will return a shallow copy of the scan filters, copy because we don't want something
     * outside of the scanner changing the actual contents of the array inadvertently.
     *
     * @return Shallow copy of the scan filters
     */

    ArrayList<ScanFilter> getScanFilters() {
        return new ArrayList<>(scanFilters);
    }

    /**
     * Set filters by UUID, will clear and reset the filters such that the next periodical
     * or high priority scan will pick them up
     *
     * @param uuidFilters The list of service UUIDs to use in the filter
     */
    void setServiceUuidFilters(List<ParcelUuid> uuidFilters) {
        synchronized (scanFilters) {
            scanFilters.clear();
            if (uuidFilters != null) {
                for (ParcelUuid uuid : uuidFilters) {
                    if (uuid != null) {
                        scanFilters.add(new ScanFilter.Builder().setServiceUuid(uuid).build());
                    }
                }
            }
        }
    }

    /**
     * If you want to only receive callbacks for devices with a rssi higher than a given value
     * set the rssi filter
     *
     * @param minRssi The minimum RSSI to be returned
     */

    void addRssiFilter(int minRssi) {
        this.minRssi = minRssi;
    }

    /**
     * Will add a new device name to the filter list such that the next scan will find devices
     * with this name, will not affect the currently in progress scan
     *
     * @param deviceName The remote bluetooth name of the device for which you are searching
     */

    void addDeviceNameFilter(String deviceName) {
        ScanFilter scanFilter = new ScanFilter.Builder().setDeviceName(deviceName).build();
        synchronized (scanFilters) {
            scanFilters.add(scanFilter);
        }
    }

    /**
     * Will add the service UUID with a given mask to find multiple devices that conform to a uuid
     * service pattern in the advertisement
     *
     * @param service The service parceluuid
     * @param mask    The parceluuid service mask
     */

    void addServiceUUIDWithMask(ParcelUuid service, ParcelUuid mask) {
        ScanFilter scanFilter = new ScanFilter.Builder().setServiceUuid(service, mask).build();
        synchronized (scanFilters) {
            scanFilters.add(scanFilter);
        }
    }

    /**
     * Add a filter for the scanner based on the service data
     *
     * @param serviceUUID     The parcel uuid for the service
     * @param serviceData     The actual service data
     * @param serviceDataMask The service data mask
     */
    void addFilterUsingServiceData(ParcelUuid serviceUUID, byte[] serviceData, byte[] serviceDataMask) {
        ScanFilter scanFilter = new ScanFilter.Builder().setServiceData(serviceUUID, serviceData, serviceDataMask).build();
        synchronized (scanFilters) {
            scanFilters.add(scanFilter);
        }
    }

    /**
     * Add scanner filter on device address.
     *
     * @param deviceAddress he device Bluetooth address for the filter. It needs to be in the
     *                      format of "01:02:03:AB:CD:EF". The device address can be validated using
     *                      {@link BluetoothAdapter#checkBluetoothAddress}.
     */
    void addDeviceAddressFilter(String deviceAddress) {
        ScanFilter scanFilter = new ScanFilter.Builder().setDeviceAddress(deviceAddress).build();
        synchronized (scanFilters) {
            scanFilters.add(scanFilter);
        }
    }

    /**
     * Will remove the cached scan filters, will not cancel the scan or affect a running scan
     * in any way
     */

    void resetFilters() {
        synchronized (scanFilters) {
            scanFilters.clear();
        }
    }

    void setScanSettings(ScanSettings scanSettings) {
        this.scanSettings = scanSettings;
    }

    /**
     * To determine if there is an active scan going on right now
     *
     * @return True if this class is actively scanning
     */

    public boolean isScanning() {
        return isScanning.get();
    }

    /**
     * To determine if there is a pending intent scan going right now
     *
     * @return True if there is a pending intent scan occurring
     */
    @SuppressWarnings("WeakerAccess")
    // API Method
    public boolean isPendingIntentScanning() {
        return pendingIntentIsScanning.get();
    }

    /**
     * To determine if there is a periodical scan enabled
     *
     * @return True if there is a periodical scan enabled
     */
    @SuppressWarnings("WeakerAccess")
    // API Method
    public boolean isPeriodicalScanEnabled() {
        return periodicalScanEnabled.get();
    }

    /**
     * Will cancel a running system managed pending intent based background scan
     */

    synchronized void cancelPendingIntentBasedBackgroundScan() {
        if (atLeastSDK(Build.VERSION_CODES.O)) {
            Context appContext = fbGatt.getAppContext();
            if (!scanner.isBluetoothEnabled()) {
                Timber.v("No scanners can be started while bluetooth is off");
                // must release the scanner here so that the system can clean it up since
                // we can't access it with bt off
                synchronized (PeripheralScanner.class) {
                    scanner = new BitgattLeScanner(appContext);
                }
                boolean oldValue = pendingIntentIsScanning.getAndSet(false);
                Timber.v("Stopping scan, changing from %b to %b", oldValue, false);
                listener.onPendingIntentScanStatusChanged(pendingIntentIsScanning.get());
                return;
            }
            if (backgroundIntentBasedScanIntent != null) {
                scanner.stopScan(backgroundIntentBasedScanIntent);
            } else {
                Timber.i("No existing pending intent, cancelling using system intent just in case ...");
                scanner.stopScan(getSystemPendingIntent(appContext));
            }
            stopPeriodicalScan = false;
            backgroundIntentBasedScanIntent = null;
            boolean oldValue = pendingIntentIsScanning.getAndSet(false);
            Timber.v("Stopping scan, changing from %b to %b", oldValue, false);
            listener.onPendingIntentScanStatusChanged(pendingIntentIsScanning.get());
        } else {
            Timber.v("This type of scan can not be executed or stopped if not API 26+");
        }
    }

    /**
     * Will start a background scan that will continue to run even if our process is killed.  This
     * will internally handle the result of particular intent based scan results and deliver
     * connection callbacks when items are found.  In order to start a pending intent based scan you will
     * need to stop any existing high-priority scan in order to enable the pending intent based scan, the intended
     * use of this API is for scanning while your application is in the background.  When you
     * come into the foreground, you should cancel the background scan
     * with {@link PeripheralScanner#cancelPendingIntentBasedBackgroundScan()} unless you want for
     * the background scan to continue. Be advised that this might result in multiple callbacks to
     * {@link FitbitGatt.FitbitGattCallback#onBluetoothPeripheralDiscovered(GattConnection)}.
     * <p>
     * This background scan will be auto cancelled by the Android operating system in a way that we
     * can not control if BT is turned off or if the phone is rebooted.  This is a function of the
     * pending intent scan Android API.
     * <p>
     * WARNING!!!! Using this with scan filters that are empty is extremely dangerous and is frowned upon
     * your application will potentially get hundreds of intent callbacks every second.  Please do
     * not use this to get around the scanfilter empty check.  Also, this call will consume a gatt_if
     * so Bitgatt will not let you have more than one scan running at a time, if you start a new scan
     * it will stop the previous one.
     *
     * @param scanFilters The specific scan filters for which to be called back
     * @param context     The Android context for creating the pending intent
     * @return true if the pending intent scan is started, false if not
     */
    synchronized boolean startPendingIntentBasedBackgroundScan(@NonNull List<ScanFilter> scanFilters, @NonNull Context context) {
        if (pendingIntentIsScanning.get()) {
            Timber.w("Not starting scan, you can't start a background scan without cancelling your existing scan");
            listener.onPendingIntentScanStatusChanged(pendingIntentIsScanning.get());
            return false;
        }
        if (scanCount.get() >= MAX_SCANS_ALLOWED_PER_30_SECONDS) {
            Timber.e("Yo Dawg I heard u like scanning ... You have already started %d scanners in this 30s, you must wait", MAX_SCANS_ALLOWED_PER_30_SECONDS);
            listener.onPendingIntentScanStatusChanged(pendingIntentIsScanning.get());
            return false;
        }
        if (atLeastSDK(Build.VERSION_CODES.O)) {
            BluetoothAdapter adapter = bleUtils.getBluetoothAdapter(context);
            if (adapter == null) {
                return false;
            }
            if (scanFilters.isEmpty()) {
                Timber.w("You can not start a background scan with no filters.");
                listener.onPendingIntentScanStatusChanged(pendingIntentIsScanning.get());
                return false;
            }
            backgroundIntentBasedScanIntent = getSystemPendingIntent(context);
            // if a pending intent scan is underway we will clear it first as we now know that
            // this consumes gatt_ifs we can not have more than one ever running at a time
            // we can't rely on the boolean because we might have been killed between calls
            // and need to cancel anyway so we will try to proactively cancel
            stopBackgroundScan(backgroundIntentBasedScanIntent);
            int didStart;
            try {
                didStart = scanner.startScan(scanFilters, scanSettings, backgroundIntentBasedScanIntent);
            } catch (NullPointerException e) {
                //https://fabric.io/fitbit7/android/apps/com.fitbit.fitbitmobile/issues/2e9748f5333eaa38d7f7141f73139504?time=last-ninety-days
                Timber.w(e, "Could not start scan, android internal stack NPE");
                listener.onPendingIntentScanStatusChanged(pendingIntentIsScanning.get());
                return false;
            }
            int count = scanCount.incrementAndGet();
            Timber.v("Starting scan, scan count in this %d ms is %d", getScannerDuration(), count);
            if (didStart == 0) {
                Timber.d("You have started a system background scan, any other scan is still running");
                boolean oldValue = pendingIntentIsScanning.getAndSet(true);
                Timber.v("Scan started, changing from scanning status %b to %b", oldValue, pendingIntentIsScanning.get());
                listener.onPendingIntentScanStatusChanged(pendingIntentIsScanning.get());
            } else {
                switch (didStart) {
                    case SCAN_FAILED_ALREADY_STARTED:
                        Timber.d("Can't start scan, already started");
                        listener.onPendingIntentScanStatusChanged(pendingIntentIsScanning.get());
                        break;
                    case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                        Timber.d("Can't start scan, application registration failed");
                        listener.onPendingIntentScanStatusChanged(pendingIntentIsScanning.get());
                        break;
                    case SCAN_FAILED_INTERNAL_ERROR:
                        Timber.d("Can't start scan, internal error");
                        listener.onPendingIntentScanStatusChanged(pendingIntentIsScanning.get());
                        break;
                    case SCAN_FAILED_FEATURE_UNSUPPORTED:
                        Timber.d("Can't start scan, feature unsupported");
                        listener.onPendingIntentScanStatusChanged(pendingIntentIsScanning.get());
                        break;
                    default:
                        Timber.d("Can't start scan, out of hardware resources, or scanning too frequently.");
                        listener.onPendingIntentScanStatusChanged(pendingIntentIsScanning.get());
                        break;
                }
                return false;
            }
            listener.onPendingIntentScanStatusChanged(pendingIntentIsScanning.get());
            stopPeriodicalScan = false;
            return true;
        } else {
            return false;
        }
    }

    private PendingIntent getSystemPendingIntent(Context context){
        Intent broadcastIntent = new Intent(context, HandleIntentBasedScanResult.class);
        broadcastIntent.setAction(SCANNED_DEVICE_ACTION);
        broadcastIntent.setClass(context, HandleIntentBasedScanResult.class);
        return PendingIntent.getBroadcast(context,
            BACKGROUND_SCAN_REQUEST_CODE, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Will start a background scan that will continue to run even if our process is killed.
     *
     * this call will consume a gatt_if
     * so Fitbitgatt will not let you have more than one scan running at a time, if you start a new scan
     * it will stop the previous one.
     *
     * @param macAddresses    The specific mac addresses for which to be called back
     * @param broadcastIntent The broadcast intent to be sent when the device is found.
     *                        Will wake up application if process is dead.
     * @param context         The Android context for creating the pending intent
     * @return The pending intent that should be used to cancel the scan if desired, or null
     * if the scan wasn't started.
     */

    @Nullable
    PendingIntent startBackgroundScan(List<String> macAddresses, Intent broadcastIntent, Context context) {
        if (atLeastSDK(Build.VERSION_CODES.O)) {
            BluetoothAdapter adapter = bleUtils.getBluetoothAdapter(context);
            if (adapter == null) {
                listener.onPendingIntentScanStatusChanged(pendingIntentIsScanning.get());
                return null;
            }

            if (!bleUtils.isBluetoothEnabled(context)) {
                Timber.w("Scanner cannot be started when Bluetooth is off");
                listener.onPendingIntentScanStatusChanged(pendingIntentIsScanning.get());
                return null;
            }
            if (scanCount.get() >= MAX_SCANS_ALLOWED_PER_30_SECONDS) {
                Timber.e("Yo Dawg I heard u like scanning ... You have already started %d scanners in this 30s, you must wait", MAX_SCANS_ALLOWED_PER_30_SECONDS);
                listener.onPendingIntentScanStatusChanged(pendingIntentIsScanning.get());
                return null;
            }

            List<ScanFilter> filters = new ArrayList<>(macAddresses.size());
            // if filters are already set up, we should use them
            synchronized (scanFilters) {
                filters.addAll(scanFilters);
            }
            // check to ensure that the address provided is valid
            if (filters.isEmpty()) {
                Timber.w("You can not start a background scan with no filters.");
                listener.onPendingIntentScanStatusChanged(pendingIntentIsScanning.get());
                return null;
            }
            PendingIntent pending = PendingIntent.getBroadcast(context,
                BACKGROUND_SCAN_REQUEST_CODE, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            // if a pending intent scan is underway we will clear it first as we now know that
            // this consumes gatt_ifs we can not have more than one ever running at a time
            // we can't rely on the boolean because we might have been killed between calls
            // and need to cancel anyway so we will try to proactively cancel
            stopBackgroundScan(pending);
            // in addition if there are mac addresses that we want to add we can do that
            for (String address : macAddresses) {
                if (adapter.getRemoteDevice(address) != null) {
                    Timber.v("Starting background scan for device : %s", address);
                    // you are not allowed to scan all, it will crash the app
                    ScanFilter filter = new ScanFilter.Builder().setDeviceAddress(address).build();
                    filters.add(filter);
                } else {
                    Timber.w("Invalid address %s provided.", address);
                }
            }
            if (!filters.isEmpty()) {
                int didStart;
                try {
                    didStart = scanner.startScan(filters, scanSettings, pending);
                } catch (NullPointerException e) {
                    Timber.w(e, "Could not start scan, android internal stack NPE");
                    listener.onPendingIntentScanStatusChanged(pendingIntentIsScanning.get());
                    return null;
                }
                int count = scanCount.incrementAndGet();
                Timber.v("Starting scan, scan count in this %d ms is %d", getScannerDuration(), count);
                if (didStart == 0) {
                    Timber.d("You have started a DIY system background scan, stopping periodical scan until background scan is stopped");
                    boolean oldValue = pendingIntentIsScanning.getAndSet(true);
                    Timber.v("Scan started, changing from scanning status %b to %b", oldValue, pendingIntentIsScanning.get());
                    listener.onPendingIntentScanStatusChanged(pendingIntentIsScanning.get());
                } else {
                    switch (didStart) {
                        case SCAN_FAILED_ALREADY_STARTED:
                            Timber.d("Can't start scan, already started");
                            listener.onPendingIntentScanStatusChanged(pendingIntentIsScanning.get());
                            break;
                        case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                            Timber.d("Can't start scan, application registration failed");
                            listener.onPendingIntentScanStatusChanged(pendingIntentIsScanning.get());
                            break;
                        case SCAN_FAILED_INTERNAL_ERROR:
                            Timber.d("Can't start scan, internal error");
                            listener.onPendingIntentScanStatusChanged(pendingIntentIsScanning.get());
                            break;
                        case SCAN_FAILED_FEATURE_UNSUPPORTED:
                            Timber.d("Can't start scan, feature unsupported");
                            listener.onPendingIntentScanStatusChanged(pendingIntentIsScanning.get());
                            break;
                        default:
                            Timber.d("Can't start scan, out of hardware resources, or scanning too frequently.");
                            listener.onPendingIntentScanStatusChanged(pendingIntentIsScanning.get());
                            break;
                    }
                    return null;
                }
                stopPeriodicalScan = false;
                return pending;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Will stop a previously started background scan
     *
     * @param pendingIntent The pending intent used to start the background scan
     */

    void stopBackgroundScan(PendingIntent pendingIntent) {
        if (atLeastSDK(Build.VERSION_CODES.O)) {
            if (!scanner.isBluetoothEnabled()) {
                Timber.v("No scanners can be started while bluetooth is off");
                // must release the scanner here so that the system can clean it up since
                // we can't access it with bt off
                synchronized (PeripheralScanner.class) {
                    scanner = new BitgattLeScanner(fbGatt.getAppContext());
                }
                return;
            }
            scanner.stopScan(pendingIntent);
            stopPeriodicalScan = false;
        } else {
            Timber.v("This type of scan can not be executed or stopped if not API 26+");
        }
    }

    private synchronized boolean startScan(@Nullable Context context) {
        ArrayList<ScanFilter> filters;
        synchronized (scanFilters) {
            filters = new ArrayList<>(scanFilters);
        }
        if (context == null) {
            Timber.v("Can't start scan with a null context");
            return false;
        }
        if (scanCount.get() >= MAX_SCANS_ALLOWED_PER_30_SECONDS) {
            Timber.e("Yo Dawg I heard u like scanning ... You have already started %d scanners in this %d, you must wait", MAX_SCANS_ALLOWED_PER_30_SECONDS, getScannerDuration());
            listener.onScanStatusChanged(isScanning.get());
            return false;
        }
        //remove timeout and other scan request
        mHandler.removeCallbacks(scanTimeoutRunnable);
        mHandler.removeCallbacks(periodicRunnable);
        //start scan
        if (!isScanning.getAndSet(true)) {
            ScanSettings settings = this.scanSettings;
            if (bleUtils.getBluetoothAdapter(context) == null) {
                // we should just use a basic one if we are in mock mode
                settings = new ScanSettings.Builder().build();
            } else {
                // don't start a scanner without scan filters
                Timber.v("Scan filter's size: %s", filters.size());
                if (filters.isEmpty()) {
                    Timber.w("We will not start a scan without filters");
                    isScanning.set(false);
                    listener.onScanStatusChanged(isScanning.get());
                    return false;
                }
            }
            boolean scanStarted;
            // if the scanner is null here it's either because BT is off / bt is in the
            // simulator or because we removed it before because the user disabled bt
            // we can get here is scanner had previously been defined, but now the
            // adapter is turned off
            if (scanner.isBluetoothEnabled()) {
                try {
                    scanner.startScan(filters, settings, callback);
                } catch (NullPointerException e) {
                    Timber.w(e, "Couldn't start the scanner, android internal NPE");
                    isScanning.set(false);
                    listener.onScanStatusChanged(isScanning.get());
                    return false;
                }
                int count = scanCount.incrementAndGet();
                Timber.v("Starting scan, scan count in this %d ms is %d", getScannerDuration(), count);
                scanStarted = true;
            } else {
                Timber.w("BT Seems to be off, not starting scan");
                isScanning.set(false);
                scanStarted = false;
            }
            listener.onScanStatusChanged(isScanning.get());
            resetScanBackoff = false;

            //Schedule timeout
            mHandler.postDelayed(scanTimeoutRunnable, getScannerDuration());
            return scanStarted;
        } else {
            Timber.w("Already scanning, will not start a new scan");
            return false;
        }
    }

    /**
     * Will stop an ongoing scan
     *
     * @param context The android context
     */
    @SuppressWarnings("WeakerAccess")
    // API Method
    synchronized void stopScan(@Nullable Context context) {
        if (context == null) {
            Timber.v("Can't stop scan with a null context");
            return;
        }
        if (scanner.isBluetoothEnabled()) {
            scanner.flushPendingScanResults(callback);
            boolean oldValue = isScanning.getAndSet(false);
            Timber.v("Stopping scan, changing from %b to %b", oldValue, false);
            scanner.stopScan(callback);
        } else {
            // adapter was null or BT was off
            Timber.w("Bluetooth must have been turned off");
            boolean oldValue = isScanning.getAndSet(false);
            Timber.v("Stopping scan, changing from %b to %b", oldValue, false);
            listener.onScanStatusChanged(false);
        }
        listener.onScanStatusChanged(false);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    void populateMockScanResultBatchValues(List<ScanResult> scanResults) {
        if (callback != null) {
            callback.onBatchScanResults(scanResults);
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    @SuppressWarnings("SameParameterValue")
        // API Method
    void populateMockScanResultIndividualValue(int callbackType, ScanResult scanResult) {
        if (callback != null) {
            callback.onScanResult(callbackType, scanResult);
        }
    }

    void onDeviceDisconnected(BluetoothDevice device) {
        if (foundDevices != null && device != null) {
            foundDevices.remove(device.getAddress());
        }
        resetScanBackoff = true;
        if (!isScanning.get() && periodicalScanEnabled.get()) {
            Timber.v("Not scanning when device disconnected, let's try to get it back.");
            scanBackoffMultiplier = DEFAULT_SCAN_BACKOFF_MULTIPLIER;
            startPeriodicScan(fbGatt.getAppContext());
        }
    }

    void recycleLeScanner() {
        synchronized (PeripheralScanner.class) {
            scanner = new BitgattLeScanner(fbGatt.getAppContext());
        }
    }

    class PeriodicScanRunnable implements Runnable {
        @Override
        public void run() {
            startPeriodicScan(fbGatt.getAppContext());
        }
    }

    @SuppressWarnings("PMD.AccessorMethodGeneration")
    class ScanTimeoutRunnable implements Runnable {
        @Override
        public void run() {
            Timber.d("Scan timeout");
            stopScan(fbGatt.getAppContext());
            if (periodicalScanEnabled.get()) {
                if (resetScanBackoff) {
                    scanBackoffMultiplier = DEFAULT_SCAN_BACKOFF_MULTIPLIER;
                } else {
                    scanBackoffMultiplier = Math.min(MAX_BACKOFF_MULTIPLIER, scanBackoffMultiplier << 1);
                }
                Timber.d("Posting the periodic start to run in %d ms", scanBackoffMultiplier * getScannerInterval());
                mHandler.postDelayed(periodicRunnable, scanBackoffMultiplier * getScannerInterval());
            }
        }
    }
}
