/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.logging.BitgattDebugTree;
import com.fitbit.bluetooth.fbgatt.logging.BitgattReleaseTree;
import com.fitbit.bluetooth.fbgatt.tx.AddGattServerServiceTransaction;
import com.fitbit.bluetooth.fbgatt.tx.GattConnectTransaction;
import com.fitbit.bluetooth.fbgatt.tx.GattDisconnectTransaction;
import com.fitbit.bluetooth.fbgatt.util.GattUtils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.WorkerThread;
import timber.log.Timber;

import static com.fitbit.bluetooth.fbgatt.PeripheralScanner.BACKGROUND_SCAN_REQUEST_CODE;

/**
 * The gatt interface, will pass a connection object to a caller with a reference to this instance
 * so that they can communicate with a given device.
 * <p>
 * While the peripheral manager will be notified of new connections and disconnections, the actual
 * connection management will be handled here, by connection management we mean the caching of the
 * connection primarily as well as retiring unused connections.
 * <p>
 * The gatt server will by default have only the {@link GattServerCallback} listening for events
 * to make sure that your peripherals that are aggressive about discovering services and firing
 * off read requests don't disconnect because the services aren't started, we will respond with
 * gatt error until the services are ready.  Please use startWithServices if you require services
 * to be up as soon as the first client connects.
 * <p>
 * Created by iowens on 10/17/17.
 */

public class FitbitGatt implements PeripheralScanner.TrackerScannerListener, BluetoothRadioStatusListener.BluetoothOnListener {

    static final long MAX_TTL = TimeUnit.HOURS.toMillis(1);
    /*
     * The Fitbit gatt instance, this holds the context, lint is rightfully complaining about
     * leaking the context.
     */
    @SuppressLint("StaticFieldLeak")
    private static final FitbitGatt ourInstance = new FitbitGatt();

    private final ConcurrentHashMap<FitbitBluetoothDevice, GattConnection> connectionMap = new ConcurrentHashMap<>();
    private static final long CLEANUP_INTERVAL = TimeUnit.MINUTES.toMillis(5);
    // this is only used on init
    private final CopyOnWriteArrayList<FitbitGattCallback> overallGattEventListeners;

    private BluetoothGattServer gattServer;
    // this is only used on init
    private final CopyOnWriteArrayList<BluetoothGattService> servicesToAdd;
    private @NonNull
    BatteryDataStatsAggregator powerAggregator;
    private GattServerConnection serverConnection;
    private GattServerCallback serverCallback;
    private GattClientCallback clientCallback;
    private @Nullable
    PeripheralScanner peripheralScanner;
    private @NonNull AlwaysConnectedScanner alwaysConnectedScanner;
    @VisibleForTesting
    LowEnergyAclListener aclListener;
    private @Nullable
    Context appContext;
    @VisibleForTesting
    AtomicBoolean isStarted = new AtomicBoolean(false);
    private Handler connectionCleanup;
    private BluetoothRadioStatusListener radioStatusListener;
    @VisibleForTesting
    volatile boolean isBluetoothOn;

    public static FitbitGatt getInstance() {
        return ourInstance;
    }

    @VisibleForTesting
    @SuppressWarnings("WeakerAccess") // API Method
    public void setGattServer(GattServerConnection gattServer) {
        this.serverConnection = gattServer;
    }

    @SuppressWarnings("WeakerAccess") // API Method
    public boolean isBluetoothOn() {
        if (appContext == null) {
            Timber.w("Bitgatt must not be started yet, so as far as we know BT is off.");
            return false;
        }
        BluetoothAdapter adapter = new GattUtils().getBluetoothAdapter(appContext);
        if (adapter == null || !adapter.isEnabled()) {
            if (isBluetoothOn) {
                isBluetoothOn = false;
            }
        } else {
            if (!isBluetoothOn) {
                isBluetoothOn = true;
            }
        }
        return isBluetoothOn;
    }

    /**
     * Used to communicate to subscribers about changes in the global state of the gatt library
     * as well as when peripherals are ready to be used.
     */

    public interface FitbitGattCallback {

        /**
         * An recently discovered bluetooth peripheral has been detected as the result of a scan, to prevent errors
         * you should check the peripheral's connection state because this could reuse the connection
         *
         * @param connection The connection that we have created as the result of a scan result
         */
        void onBluetoothPeripheralDiscovered(GattConnection connection);

        /**
         * A bluetooth peripheral has disconnected with the given connection
         *
         * @param connection The connection in the map of the disconnected peripheral
         */
        void onBluetoothPeripheralDisconnected(GattConnection connection);

        /**
         * All gatt server services are attached and the gatt is ready to be used
         */
        void onFitbitGattReady();

        /**
         * Will notify of scanner start
         */
        void onScanStarted();

        /**
         * Will notify of scanner stop
         */
        void onScanStopped();

        /**
         * Will notify of pending intent scan stop
         */
        void onPendingIntentScanStopped();

        /**
         * Will notify of pending intent scan started
         */
        void onPendingIntentScanStarted();

        /**
         * In order to make sure that consumers of the bitgatt library only react to BT off
         * we will want for them to utilize the bt off / on as tracked by bitgatt and not
         * implement their own broadcast receiver.
         */
        @MainThread
        void onBluetoothOff();

        /**
         * In order to make sure that consumers of the bitgatt library only react to BT off
         * we will want for them to utilize the bt off / on as tracked by bitgatt and not
         * implement their own broadcast receiver.
         */
        @MainThread
        void onBluetoothOn();

        /**
         * Some phones may not deliver this, older ones especially may not, be aware that the code
         * in here may not be executed, however you can execute code that you want to have happen
         * before the radio turns on.
         */
        @MainThread
        void onBluetoothTurningOn();

        /**
         * Some phones deliver this, older ones may not, be aware that code inside of here may not
         * be executed, however you can execute code that you want to have happen
         * before the radio turns off
         */
        @MainThread
        void onBluetoothTurningOff();
    }

    @VisibleForTesting
    FitbitGatt() {
        // only add a custom logger if the implementer isn't using Timber, if they are using Timber
        // let them deal with it, just make sure your variants set BuildConfig.DEBUG correctly
        if(Timber.treeCount() == 0) {
            if (BuildConfig.DEBUG) {
                Timber.plant(new BitgattDebugTree());
            } else {
                Timber.plant(new BitgattReleaseTree());
            }
        }
        overallGattEventListeners = new CopyOnWriteArrayList<>();
        servicesToAdd = new CopyOnWriteArrayList<>();
        // we will default to one expected device and that it should not looking
        this.alwaysConnectedScanner = new AlwaysConnectedScanner(1, false);
        powerAggregator = new BatteryDataStatsAggregator(null);
    }

    public void registerGattEventListener(FitbitGattCallback callback) {
        if (!overallGattEventListeners.contains(callback)) {
            overallGattEventListeners.add(callback);
        }
    }

    @SuppressWarnings("WeakerAccess") // API Method
    public void unregisterGattEventListener(FitbitGattCallback callback) {
        overallGattEventListeners.remove(callback);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    @SuppressWarnings("unused")
        // API Method
    List<GattClientListener> getAllGattClientListeners() {
        return getClientCallback().getGattClientListeners();
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    void unregisterAllGattEventListeners() {
        overallGattEventListeners.clear();
    }

    /**
     * Will allow another peer to determine if all of the services required have started
     *
     * @return True if all services have started, false otherwise
     */

    boolean allServicesStarted() {
        synchronized (servicesToAdd) {
            return servicesToAdd.isEmpty();
        }
    }

    void setStarted() {
        Timber.i("Initalization complete, start finished");
        boolean success = isStarted.compareAndSet(false, true);
        if (!success) {
            Timber.w("There was a problem updating the started state, are you starting from two threads?");
        } else {
            for (FitbitGattCallback callback : overallGattEventListeners) {
                callback.onFitbitGattReady();
            }
        }
    }

    /**
     * Will start a high-priority scan, if there is already a scan in progress this call will cancel
     * the in-progress scan and start a new one at a high-duty cycle, please use this sparingly for
     * a couple of reasons:
     * 1) The concept of bitgatt is that you can rely on the system to find devices that match the
     * filter criteria.  This type of scan should only be necessary if you know that the device
     * is disconnected, and you suspect that a connection is not already in the cache.  Please use
     * the various background scanning APIs instead if your goal is to remain connected.
     * 2) This type of scan is very expensive power-wise for the phone and should not be used
     * constantly, please use the {@link FitbitGatt#startPeriodicScan(Context)} or {@link FitbitGatt#startBackgroundScan(Context, Intent, List)}
     * to find devices and rely on the device discovery callbacks or polling the connection cache
     * {@link FitbitGatt#getMatchingConnectionsForDeviceNames(List)} or {@link FitbitGatt#getMatchingConnectionsForServices(List)}
     *
     * @param context The android context for providing to the scanner
     * @return True if the scan started, false if it did not
     */

    public boolean startHighPriorityScan(Context context) {
        if(alwaysConnectedScanner.isAlwaysConnectedScannerEnabled()) {
            Timber.i("You are using the always connected scanner, stop it first before ad-hoc scanning");
            return false;
        }
        if (peripheralScanner == null) {
            Timber.w("You are trying to start a high-priority scan, but the scanner isn't set-up, did you call FitbitGatt#start?");
            return false;
        }
        return peripheralScanner.startHighPriorityScan(context);
    }

    /**
     * Upon setting up your scan filters, this call will start to periodically scan for matching devices, it will notify via the {@link FitbitGatt.FitbitGattCallback}
     * interface if a device is discovered and will provide the {@link GattConnection} to you
     *
     * @param context The android context for the scanner
     * @return True if the scan started, false if it did not
     */
    @SuppressWarnings({"unused", "WeakerAccess"}) // API Method
    public boolean startPeriodicScan(Context context) {
        if(alwaysConnectedScanner.isAlwaysConnectedScannerEnabled()) {
            Timber.i("You are using the always connected scanner, stop it first before ad-hoc scanning");
            return false;
        }
        if (peripheralScanner == null) {
            Timber.w("You are trying to start a periodical scan, but the scanner isn't set-up, did you call FitbitGatt#start?");
            return false;
        }
        return peripheralScanner.startPeriodicScan(context);
    }

    /**
     * Will cancel high priority and periodical scans that are currently running, but will not have any effect if using the
     * background PendingIntent based scanner, and will not un-schedule periodical scans. Use {@link PeripheralScanner#cancelPeriodicalScan(Context)}
     * to stop periodical scans.
     *
     * @param context The android context for the scanner
     */
    @SuppressWarnings("WeakerAccess") // API Method
    public void cancelScan(@Nullable Context context) {
        if(alwaysConnectedScanner.isAlwaysConnectedScannerEnabled()) {
            Timber.i("You are using the always connected scanner, stop it first before ad-hoc scanning");
            return;
        }
        if (peripheralScanner == null) {
            Timber.w("You are trying to cancel a scan, but the scanner isn't set-up, did you call FitbitGatt#start?");
            return;
        }
        peripheralScanner.cancelScan(context);
    }

    /**
     * Will cancel a periodical scan, but will not have any effect if using the background PendingIntent
     * based scanner.  Will not cancel an in progress high priority scan
     *
     * @param context The android context for the scanner
     */
    @SuppressWarnings("unused") // API Method
    public void cancelPeriodicalScan(@Nullable Context context) {
        if(alwaysConnectedScanner.isAlwaysConnectedScannerEnabled()) {
            Timber.i("You are using the always connected scanner, stop it first before ad-hoc scanning");
            return;
        }
        if (peripheralScanner == null) {
            Timber.w("You are trying to cancel a scan, but the scanner isn't set-up, did you call FitbitGatt#start?");
            return;
        }
        peripheralScanner.cancelPeriodicalScan(context);
    }

    /**
     * Will cancel a high-priority scan, but will not have any effect if using the background PendingIntent
     * based scanner.  Will not cancel an enabled periodical scan
     *
     * @param context The android context for the scanner
     */
    @SuppressWarnings("unused") // API Method
    public void cancelHighPriorityScan(@Nullable Context context) {
        if(alwaysConnectedScanner.isAlwaysConnectedScannerEnabled()) {
            Timber.i("You are using the always connected scanner, stop it first before ad-hoc scanning");
            return;
        }
        if (peripheralScanner == null) {
            Timber.w("You are trying to cancel a scan, but the scanner isn't set-up, did you call FitbitGatt#start?");
            return;
        }
        peripheralScanner.cancelHighPriorityScan(context);
    }

    /**
     * Will put the scanner into mock mode which is useful for testing
     *
     * @param mockMode true to set mock mode, false to disable it.
     */
    @SuppressWarnings({"unused", "WeakerAccess"}) // API Method
    public void setScannerMockMode(boolean mockMode) {
        if (peripheralScanner == null) {
            Timber.w("You are trying to put the scanner into mock mode, but the scanner isn't set-up, did you call FitbitGatt#start?");
            return;
        }
        alwaysConnectedScanner.setTestMode(mockMode);
    }

    /**
     * Will set filters on bluetooth device name, it is important to remember that these filters can
     * be rendered obsolete if the peripheral changes it's advertising device name.
     *
     * @param deviceNameFilters A list of device bluetooth names
     */
    @SuppressWarnings("unused") // API Method
    public void setDeviceNameScanFilters(List<String> deviceNameFilters) {
        if (peripheralScanner == null) {
            Timber.w("You are trying to set device name filters on the scanner, but the scanner isn't set-up, did you call FitbitGatt#start?");
            return;
        }
        peripheralScanner.setDeviceNameFilters(deviceNameFilters);
    }

    /**
     * Will add a name onto the bluetooth device name filters, it is important to remember that these filters can
     * be rendered obsolete if the peripheral changes it's advertising device name.  Also, this function
     * will add a new name that will only take effect after the currently running scan.
     *
     * @param deviceName A device bluetooth names
     */
    @SuppressWarnings("unused") // API Method
    public void addDeviceNameScanFilter(String deviceName) {
        if (peripheralScanner == null) {
            Timber.w("You are trying to add a device name filter onto the scanner, but the scanner isn't set-up, did you call FitbitGatt#start?");
            return;
        }
        peripheralScanner.addDeviceNameFilter(deviceName);
    }

    /**
     * Will set service uuid filters on the scanner, this works with adding proper service records
     * within the adv packet for the peripheral, but may not work with service records added as part
     * of the service data.  Please use the service data filter for these records to be certain as this
     * is dependent upon the HW implementation on the particular Android device.
     *
     * @param uuidFilters The service UUIDs upon which to filter advertising peripherals
     */

    public void setScanServiceUuidFilters(List<ParcelUuid> uuidFilters) {
        if (peripheralScanner == null) {
            Timber.w("You are trying to set service uuid filters on the scanner, but the scanner isn't set-up, did you call FitbitGatt#start?");
            return;
        }
        peripheralScanner.setServiceUuidFilters(uuidFilters);
    }

    /**
     * Will add the service UUID with a given mask to find multiple devices that conform to a uuid
     * service pattern in the advertisement.  Will only take effect after the current scan has ended
     * in the next scan.
     *
     * @param service The service parceluuid
     * @param mask    The parceluuid service mask
     */
    @SuppressWarnings("unused") // API Method
    public void addScanServiceUUIDWithMaskFilter(ParcelUuid service, ParcelUuid mask) {
        if (peripheralScanner == null) {
            Timber.w("You are trying to set service uuid with mask filters on the scanner, but the scanner isn't set-up, did you call FitbitGatt#start?");
            return;
        }
        peripheralScanner.addServiceUUIDWithMask(service, mask);
    }

    /**
     * Add a filter for the scanner based on the service data.  Will only take effect after the current
     * scan has ended if one is running.
     *
     * @param serviceUUID     The parcel uuid for the service
     * @param serviceData     The actual service data
     * @param serviceDataMask The service data mask
     */
    @SuppressWarnings("unused") // API Method
    public void addFilterUsingServiceData(ParcelUuid serviceUUID, byte[] serviceData, byte[] serviceDataMask) {
        if (peripheralScanner == null) {
            Timber.w("You are trying to add a filter using service data to the scanner, but the scanner isn't set-up, did you call FitbitGatt#start?");
            return;
        }
        peripheralScanner.addFilterUsingServiceData(serviceUUID, serviceData, serviceDataMask);
    }

    /**
     * Will filter scan results for a min rssi, not the most reliable way to determine nearby devices
     * since the RSSI values vary from phone to phone, but it is possible to build a model for this
     * and do this effectively.
     *
     * @param minRssi The minimum RSSI value to accept for a callback upon a found device
     */
    @SuppressWarnings("unused") // API Method
    public void addScanRssiFilter(int minRssi) {
        if (peripheralScanner == null) {
            Timber.w("You are trying to set rssi filters on the scanner, but the scanner isn't set-up, did you call FitbitGatt#start?");
            return;
        }
        peripheralScanner.addRssiFilter(minRssi);
    }

    /**
     * Add scanner filter on device address. Will only take effect after the current scan has ended
     * if one is running.
     * <p>
     * Note: This filter is known to not be supported on some hardware like "HTC One" and some Huawei phones
     *
     * @param deviceAddress he device Bluetooth address for the filter. It needs to be in the
     *                      format of "01:02:03:AB:CD:EF". The device address can be validated using
     *                      {@link BluetoothAdapter#checkBluetoothAddress}.
     */
    @SuppressWarnings("WeakerAccess") // API Method
    public void addDeviceAddressFilter(String deviceAddress) {
        if (peripheralScanner == null) {
            Timber.w("You are trying to add a device address filter onto the scanner, but the scanner isn't set-up, did you call FitbitGatt#start?");
            return;
        }
        peripheralScanner.addDeviceAddressFilter(deviceAddress);
    }

    /**
     * Will return a shallow copy of the current scan filters held by the scanner
     *
     * @return Shallow copy of the current scan filters
     */
    @SuppressWarnings("WeakerAccess") // API Method
    public List<ScanFilter> getScanFilters() {
        if (peripheralScanner == null) {
            Timber.w("You are trying to get scan filters, but the scanner isn't set-up, did you call FitbitGatt#start?");
            return Collections.emptyList();
        }
        return peripheralScanner.getScanFilters();
    }

    /**
     * Will clear the scan filters currently applied, will not apply to the current scan if one is running.
     */
    @SuppressWarnings("WeakerAccess") // API Method
    public void resetScanFilters() {
        if (peripheralScanner == null) {
            Timber.w("You are trying to reset the scan filters on the scanner, but the scanner isn't set-up, did you call FitbitGatt#start?");
            return;
        }
        peripheralScanner.resetFilters();
    }

    /**
     * To determine if the scanner is presently scanning or not
     *
     * @return true if a scan is currently happening, false if not
     */
    public boolean isScanning() {
        if (peripheralScanner == null) {
            Timber.w("You are trying to determine the scan state, but the scanner isn't set-up, did you call FitbitGatt#start?");
            return false;
        }
        return peripheralScanner.isScanning();
    }

    /**
     * To determine if there is a pending intent scan going right now
     *
     * @return True if there is a pending intent scan occurring
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    // API Method
    public boolean isPendingIntentScanning() {
        if (peripheralScanner == null) {
            Timber.w("You are trying to determine the scan state, but the scanner isn't set-up, did you call FitbitGatt#start?");
            return false;
        }
        return peripheralScanner.isPendingIntentScanning();
    }

    /**
     * To determine if there is a periodical scan enabled, should not be confused with whether a low priority scan is currently happening.
     *
     * @return True if there is a periodical scan enabled
     */
    @SuppressWarnings("unused")
    // API Method
    public boolean isPeriodicalScanEnabled() {
        if (peripheralScanner == null) {
            Timber.w("You are trying to determine the scan state, but the scanner isn't set-up, did you call FitbitGatt#start?");
            return false;
        }
        return peripheralScanner.isPeriodicalScanEnabled();
    }

    /**
     * Will allow starting with scan filters and services
     *
     * @param context  The android context
     * @param services The services desired
     * @param filters  The filters desired
     * @param callback The {@link FitbitGattCallback} instance to be called when ready
     */
    @WorkerThread
    @SuppressWarnings("WeakerAccess") // API Method
    public synchronized void startWithServicesAndScanFilters(Context context, @Nullable List<BluetoothGattService> services, @Nullable List<ScanFilter> filters, FitbitGattCallback callback) {
        if (!isStarted()) {
            if (!startSimple(context)) {
                Timber.w("Couldn't start because BT was off");
                return;
            }
            if (callback != null) {
                registerGattEventListener(callback);
            }
            boolean scanStarted;
            if (filters != null) {
                if (peripheralScanner == null) {
                    throw new IllegalStateException("The peripheral scanner should not be null at this point, and you should not try to make this call with an empty or null filter list.");
                }
                peripheralScanner.setScanFilters(filters);
                alwaysConnectedScanner.setScanFilters(filters);
                scanStarted = peripheralScanner.startPeriodicScan(context);
            } else {
                Timber.w("Shame, you shouldn't be trying to start a scan with no filters!!!");
                scanStarted = false;
            }
            Timber.i("Did scan start? %b", scanStarted);
            if (services != null) {
                Timber.v("Starting to add services, will set to started after complete");
                // usually the android stack will add the service setup to the bt stack, if this stack
                // is busy, this can take a while, so we'll need to wait until we get the callbacks
                // for all of the expected services.
                if (!services.isEmpty()) {
                    synchronized (servicesToAdd) {
                        servicesToAdd.addAll(services);
                        addServicesToGattServer();
                        return;
                    }
                }
            }
            boolean success = isStarted.compareAndSet(false, true);
            if (!success) {
                Timber.w("There was a problem updating the started state, are you starting from two threads?");
            } else {
                for (FitbitGattCallback readCallback : overallGattEventListeners) {
                    readCallback.onFitbitGattReady();
                }
            }
        } else {
            Timber.v("Already started");
        }
    }

    /**
     * Will allow starting with services, the stack will callback when ready
     *
     * @param context  The android context
     * @param services The services to be added
     * @param callback The {@link FitbitGattCallback} to be called when ready
     */
    @WorkerThread
    @SuppressWarnings("WeakerAccess") // API Method
    public synchronized void startWithServices(Context context, List<BluetoothGattService> services, FitbitGattCallback callback) {
        startWithServicesAndScanFilters(context, services, null, callback);
    }

    /**
     * Initalize the library with scan filters and start the passive scanner
     *
     * @param context     The android context
     * @param scanFilters {@link ScanFilter} list to be used for the background and subsequent scans
     */
    @WorkerThread
    @SuppressWarnings("unused") // API Method
    public synchronized void startWithScanFilters(Context context, List<ScanFilter> scanFilters, FitbitGattCallback callback) {
        startWithServicesAndScanFilters(context, null, scanFilters, callback);
    }

    /**
     * Will initialize the gatt server and add all of the internal default listeners
     *
     * @param context The Android context
     */
    @WorkerThread
    public synchronized void start(Context context) {
        if (!isStarted()) {
            if (!startSimple(context)) {
                Timber.w("Couldn't start because BT was off");
                return;
            }
            boolean success = isStarted.compareAndSet(false, true);
            if (!success) {
                Timber.w("There was a problem updating the started state, are you starting from two threads?");
            } else {
                for (FitbitGattCallback readCallback : overallGattEventListeners) {
                    readCallback.onFitbitGattReady();
                }
            }
        } else {
            Timber.v("Already started");
        }
    }

    private synchronized boolean startSimple(Context context) {
        Timber.v("Starting fitbit gatt");
        powerAggregator = new BatteryDataStatsAggregator(context);
        this.appContext = context.getApplicationContext();
        this.serverCallback = new GattServerCallback(context);
        this.clientCallback = new GattClientCallback(context);
        this.aclListener = new LowEnergyAclListener();
        this.aclListener.register(this.appContext);
        connectionCleanup = new Handler(context.getMainLooper());
        // we can initialize this value here and guard the scanner and the gatt
        // server this way
        BluetoothAdapter adapter = new GattUtils().getBluetoothAdapter(context);
        isBluetoothOn = adapter != null && adapter.isEnabled();
        this.peripheralScanner = new PeripheralScanner(context, this);
        Timber.v("Initializing the always connected scanner for one device, and that it should stop scanning when it finds one, if you wish to change this, please configure it.");
        addConnectedDevices(context);
        // will start the cleanup process
        decrementAndInvalidateClosedConnections();
        if (radioStatusListener == null) {
            radioStatusListener = new BluetoothRadioStatusListener(getAppContext(), false);
        }
        radioStatusListener.startListening();
        radioStatusListener.setListener(this);
        // we must be under test here, or this is a horribly broken device and my life doesn't
        // matter
        // We have found that the start server call can block in the gatt_if registration step
        // this has a timeout of 10 seconds, but the ANR timeout is 5 ..., so we'll latch here
        // for 5 seconds and wait for the return value, if it can't register in 5 seconds then
        // it's probably broken and startSimple should return false.
        if (this.appContext == null) {
            startServer(context);
            return true;
        }
        return startServer(this.appContext);
    }

    /**
     * Will fetch the always connected scanner for configuration and starting.  The always connected
     * scanner is designed for you to delegate all of the scanning to bitgatt where you want to
     * always connect to a peripheral when in range, once started, no ad-hoc scanning can be
     * accomplished.
     * @return the always connected scanner
     */
    @SuppressWarnings({"unused", "WeakerAccess"}) // API method
    public @NonNull AlwaysConnectedScanner getAlwaysConnectedScanner(){
        return this.alwaysConnectedScanner;
    }

    /**
     * Do not do this unless you truly know what you are doing, there are very, very few reasons
     * to perform this operation.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    @SuppressWarnings("unused")
    // API Method
    void shutdown() {
        Timber.v("Someone wants to shutdown the gatt");
        if (serverCallback != null) {
            serverCallback.unregisterAll();
        }
        connectionMap.clear();
        if (this.appContext != null) {
            this.appContext.unregisterReceiver(this.aclListener);
        }
        this.appContext = null;
        this.serverCallback = null;
        this.clientCallback = null;
        if (this.peripheralScanner != null) {
            this.peripheralScanner.onDestroy(FitbitGatt.getInstance().getAppContext());
        }
        this.peripheralScanner = null;
        if(this.aclListener != null) {
            this.aclListener.unregister(FitbitGatt.getInstance().getAppContext());
        }
        this.aclListener = null;
        connectionCleanup = null;
        if(this.getServer() != null && this.getServer().getServerTransactionQueueController() != null) {
            this.getServer().getServerTransactionQueueController().stop();
            this.getServer().getServer().close();
        }
        gattServer = null;
        this.isStarted.set(false);
        if (radioStatusListener != null) {
            radioStatusListener.stopListening();
            radioStatusListener.removeListener();
        }
    }

    @VisibleForTesting
    void setBluetoothListener(BluetoothRadioStatusListener listener) {
        this.radioStatusListener = listener;
    }

    public synchronized boolean isStarted() {
        return isStarted.get();
    }

    public @Nullable
    Context getAppContext() {
        return appContext;
    }

    @SuppressWarnings("WeakerAccess") // API Method
    public GattServerCallback getServerCallback() {
        return this.serverCallback;
    }

    @SuppressWarnings("WeakerAccess") // API Method
    public GattClientCallback getClientCallback() {
        return this.clientCallback;
    }

    @Nullable
    PeripheralScanner getPeripheralScanner() {
        if (this.peripheralScanner == null) {
            Timber.w("The scanner is null, did you call FitbitGatt#start?");
        }
        return this.peripheralScanner;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public synchronized void putConnectionIntoDevices(FitbitBluetoothDevice device, GattConnection conn) {
        // we don't want duplicate connections in the map
        if (!connectionMap.containsKey(device)) {
            connectionMap.put(device, conn);
            // since this directly calls notify listeners of connection added
            // we will need to synchronize around those listeners since
            // someone could be adding or removing a listener while this asynchronous
            // call happens.  To do this easily, we will use a
            // CopyOnWriteArrayList for the {@link FitbitGattCallback}
            FitbitGatt.getInstance().notifyListenersOfConnectionAdded(conn);
        }
    }

    /**
     * Will check to determine if the provided {@link FitbitBluetoothDevice} is actually in the map
     *
     * @param device The {@link FitbitBluetoothDevice} for which to search
     * @return true if the device is present, false otherwise
     */

    @SuppressWarnings("WeakerAccess") // API Method
    public synchronized boolean isDeviceInConnections(FitbitBluetoothDevice device) {
        return connectionMap.containsKey(device);
    }

    private void notifyListenersOfConnectionAdded(GattConnection connection) {
        for (FitbitGattCallback callback : this.overallGattEventListeners) {
            callback.onBluetoothPeripheralDiscovered(connection);
        }
    }

    void notifyListenersOfConnectionDisconnected(GattConnection connection) {
        for (FitbitGattCallback callback : this.overallGattEventListeners) {
            callback.onBluetoothPeripheralDisconnected(connection);
        }
    }

    void registerGattServerListener(GattServerListener listener) {
        serverCallback.addListener(listener);

    }

    void unregisterGattServerListener(GattServerListener listener) {
        serverCallback.removeListener(listener);
    }

    @SuppressWarnings("unused") // API Method
    public void connectToScannedDevice(BluetoothDevice device, GattTransactionCallback callback) {
        FitbitBluetoothDevice fitDevice = new FitbitBluetoothDevice(device);
        connectToScannedDevice(fitDevice, false, callback);
    }

    @VisibleForTesting
    void connectToScannedDevice(FitbitBluetoothDevice fitDevice, boolean shouldMock, GattTransactionCallback callback) {
        GattConnection conn = connectionMap.get(fitDevice);
        if (conn == null) {
            if (appContext == null) {
                Timber.w("[%s] Bitgatt must not be started, please start bitgatt", fitDevice);
                return;
            }
            conn = new GattConnection(fitDevice, appContext.getMainLooper());
            connectionMap.put(fitDevice, conn);
            notifyListenersOfConnectionAdded(conn);
        }
        conn.setMockMode(shouldMock);
        if (!conn.isConnected()) {
            GattConnectTransaction tx = new GattConnectTransaction(conn, GattState.CONNECTED);
            conn.runTx(tx, callback);
        } else {
            TransactionResult.Builder builder = new TransactionResult.Builder();
            builder.resultStatus(TransactionResult.TransactionResultStatus.SUCCESS)
                .gattState(conn.getGattState());
            callback.onTransactionComplete(builder.build());
        }
    }

    ConcurrentHashMap<FitbitBluetoothDevice, GattConnection> getConnectionMap() {
        return connectionMap;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    List<FitbitBluetoothDevice> getNewlyScannedDevicesOnly() {
        ArrayList<FitbitBluetoothDevice> devices = new ArrayList<>();
        for (FitbitBluetoothDevice iteratedDevice : getConnectionMap().keySet()) {
            if (iteratedDevice.origin.equals(FitbitBluetoothDevice.DeviceOrigin.SCANNED)) {
                devices.add(iteratedDevice);
            }
        }
        return devices;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    void clearConnectionsMap() {
        connectionMap.clear();
    }

    /**
     * Will iterate through connections in the map to decrement those that need decrementing, and
     * will evict those that need to be evicted.  This should run for as long as the singleton
     * exists.
     */

    private void decrementAndInvalidateClosedConnections() {
        connectionCleanup.postDelayed(this::doDecrementAndInvalidateClosedConnections, CLEANUP_INTERVAL);
    }

    @VisibleForTesting
    void doDecrementAndInvalidateClosedConnections() {
        if (this.appContext == null) {
            Timber.w("[%s] Bitgatt must not be started, please start bitgatt.", Build.DEVICE);
            return;
        }
        addConnectedDevices(this.appContext);
        for (FitbitBluetoothDevice fitbitBluetoothDevice : getConnectionMap().keySet()) {
            GattConnection conn = getConnectionMap().get(fitbitBluetoothDevice);
            if (conn != null) {
                // we only want to try to prune disconnected peripherals, there may be some
                // peripherals that are connected that we want to get rid of, but the caller
                // will need to disconnect them first, eventually we will clean up the
                // connection
                if (!conn.isConnected()) {
                    long currentTtl = conn.getDisconnectedTTL();
                    if (currentTtl <= 0) {
                        conn.close();
                        GattConnection connection = getConnectionMap().remove(fitbitBluetoothDevice);
                        if (connection != null) {
                            notifyListenersOfConnectionDisconnected(connection);
                            Timber.v("Connection for %s is disconnected and pruned", connection.getDevice());
                        }
                    } else {
                        conn.setDisconnectedTTL(currentTtl - CLEANUP_INTERVAL);
                    }
                }
            }
        }
        decrementAndInvalidateClosedConnections();
    }

    @VisibleForTesting
    synchronized void addScannedDevice(FitbitBluetoothDevice device) {
        // we need to deal with the scenario where the peripheral was connected, but now
        // it is disconnected, then it is picked up in the background with the scan
        // the listener could potentially be called back twice for the same connection
        // if the user has a background scan running while an active scan is running
        GattConnection conn = connectionMap.get(device);
        if (null == conn) {
            if (appContext == null) {
                Timber.w("[%s] Bitgatt must not be started, please start bitgatt", device);
                return;
            }
            Timber.v("Adding scanned device %s", device.toString());
            conn = new GattConnection(device, appContext.getMainLooper());
            device.origin = FitbitBluetoothDevice.DeviceOrigin.SCANNED;
            connectionMap.put(device, conn);
            notifyListenersOfConnectionAdded(conn);
        } else {
            FitbitBluetoothDevice oldDevice = conn.getDevice();
            String previousDeviceName = oldDevice.getName();
            ScanRecord previousScanRecord = oldDevice.getScanRecord();
            int previousRssi = oldDevice.getRssi();
            if (!previousDeviceName.equals(device.getName())) {
                Timber.w("This device has the same mac (bluetooth ID) as a known device, but has changed it's BT name, IRL be careful this can break upstream logic, or have security implications.");
            }
            oldDevice.origin = FitbitBluetoothDevice.DeviceOrigin.SCANNED;
            if (!previousDeviceName.equals(device.getName()) ||
                previousRssi != device.getRssi() ||
                (previousScanRecord != null && device.getScanRecord() != null &&
                    !Arrays.equals(previousScanRecord.getBytes(), device.getScanRecord().getBytes()))) {
                Timber.v("Found device may have changed was %s, and now is %s", oldDevice, device);
                oldDevice.setName(device.getName());
                oldDevice.setScanRecord(device.getScanRecord());
                oldDevice.setRssi(device.getRssi());
            }
            notifyListenersOfConnectionAdded(conn);
        }
    }

    /**
     * Will create connection objects representing all of the BTLE devices connected presently
     * or bonded to this phone
     */

    private void addConnectedDevices(Context context) {
        Timber.v("Adding connected or bonded devices");
        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager != null) {
            BluetoothAdapter adapter = manager.getAdapter();
            if (adapter != null) {
                Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
                for (BluetoothDevice bondedDevice : bondedDevices) {
                    FitbitBluetoothDevice fitBluetoothDevice = new FitbitBluetoothDevice(bondedDevice);
                    fitBluetoothDevice.origin = FitbitBluetoothDevice.DeviceOrigin.BONDED;
                    if (null == connectionMap.get(fitBluetoothDevice)) {
                        if (appContext == null) {
                            Timber.w("[%s] Bitgatt must not be started, please start bitgatt", fitBluetoothDevice);
                            return;
                        }
                        GattConnection conn = new GattConnection(fitBluetoothDevice, appContext.getMainLooper());
                        Timber.v("Adding bonded device named %s, with address %s", bondedDevice.getName(), bondedDevice.getAddress());
                        connectionMap.put(fitBluetoothDevice, conn);
                        FitbitGatt.getInstance().notifyListenersOfConnectionAdded(conn);
                    }
                }
                List<BluetoothDevice> connectedDevices = manager.getConnectedDevices(BluetoothProfile.GATT);
                for (BluetoothDevice connectedDevice : connectedDevices) {
                    FitbitBluetoothDevice fitbitBluetoothDevice = new FitbitBluetoothDevice(connectedDevice);
                    fitbitBluetoothDevice.origin = FitbitBluetoothDevice.DeviceOrigin.CONNECTED;
                    if (null == connectionMap.get(fitbitBluetoothDevice)) {
                        Timber.v("Adding connected device named %s, with address %s", connectedDevice.getName(), connectedDevice.getAddress());
                        GattConnection conn = new GattConnection(fitbitBluetoothDevice, appContext.getMainLooper());
                        connectionMap.put(fitbitBluetoothDevice, conn);
                        FitbitGatt.getInstance().notifyListenersOfConnectionAdded(conn);
                    }
                }
            }
        }
        Timber.v("Added all connected or bonded devices");
    }

    /**
     * Will return a list of {@link GattConnection} objects that match the provided bluetooth device
     * names, providing a null list returns all connections
     *
     * @param names The list of bluetooth device names by which to filter the list
     * @return The list of connections matching these names
     */
    @SuppressWarnings("WeakerAccess") // API Method
    public List<GattConnection> getMatchingConnectionsForDeviceNames(@Nullable List<String> names) {
        ArrayList<GattConnection> connections = new ArrayList<>(2);
        if (names == null) {
            connections.addAll(connectionMap.values());
            return connections;
        }
        for (String name : names) {
            Enumeration<FitbitBluetoothDevice> fitbitDeviceEnumeration = connectionMap.keys();
            while (fitbitDeviceEnumeration.hasMoreElements()) {
                FitbitBluetoothDevice device = fitbitDeviceEnumeration.nextElement();
                if (device.getName().equals(name)) {
                    connections.add(connectionMap.get(device));
                }
            }
        }
        return connections;
    }

    /**
     * Will return a list of connections that match a series of service UUIDs, will return if any
     * of the services provided matches a hosted service.  If discovery has not been performed
     * on the connected device, or if it is not connected, it will not be returned.
     *
     * @param services A list of services to filter
     * @return The connections that match any of the items in the list
     */
    @SuppressWarnings("WeakerAccess") // API Method
    public List<GattConnection> getMatchingConnectionsForServices(@Nullable List<UUID> services) {
        ArrayList<GattConnection> connections = new ArrayList<>(2);
        if (services == null) {
            connections.addAll(connectionMap.values());
            return connections;
        }
        Enumeration<FitbitBluetoothDevice> fitbitDeviceEnumeration = connectionMap.keys();
        while (fitbitDeviceEnumeration.hasMoreElements()) {
            FitbitBluetoothDevice device = fitbitDeviceEnumeration.nextElement();
            GattConnection conn = connectionMap.get(device);
            if (conn != null) {
                BluetoothGatt gatt = conn.getGatt();
                if ((conn.getMockMode() || gatt != null) && conn.isConnected()) {
                    for (UUID serviceUuid : services) {
                        if (conn.connectedDeviceHostsService(serviceUuid)) {
                            connections.add(conn);
                        }
                    }
                }
            }
        }
        return connections;
    }

    /**
     * Will retrieve a connection from the map if one exists using the bluetooth mac address
     * of the device, or will return null if it does not.
     *
     * @param bluetoothAddress The bluetooth mac address
     * @return NULL if error creating creating connection, the connection if it does
     */
    @SuppressWarnings("unused") // API Method
    public @Nullable
    GattConnection getConnectionForBluetoothAddress(String bluetoothAddress) {
        if (appContext != null) {
            return getConnectionForBluetoothAddress(appContext, bluetoothAddress);
        } else {
            Timber.w("Error getting connection FitbitGatt start state %s", isStarted());
            return null;
        }
    }

    /**
     * Will retrieve a connection from the map if one exists using the bluetooth mac address
     * of the device, or will create one if it does not.
     *
     * @param context          The Android context
     * @param bluetoothAddress The bluetooth mac address
     * @return NULL if error creating creating connection, the connection if it does
     * @deprecated Using this method is discouraged as it will soon become private
     */
    @Deprecated
    public @Nullable
    GattConnection getConnectionForBluetoothAddress(Context context, String bluetoothAddress) {
        BluetoothManager mgr = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (mgr != null) {
            BluetoothAdapter adp = mgr.getAdapter();
            if (adp != null) {
                BluetoothDevice bluetoothDevice = adp.getRemoteDevice(bluetoothAddress);
                return getConnection(bluetoothDevice);
            } else {
                Timber.e("Couldn't fetch the connection because we couldn't initialize the adapter");
                return null;
            }
        } else {
            Timber.e("Couldn't fetch the connection because we couldn't initialize the manager");
            return null;
        }
    }

    /**
     * Synchronized because this can be called in effect from bluetooth on and start, there could
     * be some odd behavior if this were called concurrently
     * @param context The android context
     * @return true if the gatt server started successfully, false if it didn't
     */

    private synchronized boolean startServer(Context context) {
        boolean isStarted;
        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager != null && manager.getAdapter() != null) {
            /*
             * We've observed that the registration of the callback inside of the android
             * source for the gatt_if can lead to a hang for up to 10 seconds, but the
             * ANR time is 5 seconds currently (Summer 2019)
             */
            CountDownLatch cdl = new CountDownLatch(1);
            boolean didNotTimeout = false;
            this.getServerCallback().getServerCallbackHandler().post(() -> {
                gattServer = manager.openGattServer(context, serverCallback);
                cdl.countDown();
            });
            try {
                didNotTimeout = cdl.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Timber.e(e, "The gatt server couldn't be started because the thread was interrupted");
            }
            isStarted = true;
            if (gattServer == null) {
                if(!didNotTimeout) {
                    Timber.w("The manager could not open the gatt server!!! are you sure BT is on!");
                } else {
                    Timber.w("The openGattServer call timed out, the phone should probably be restarted");
                }
                isStarted = false;
            } else {
                // make sure there are no hidden services, we don't want duplicates for the phones who
                // carry services across BT toggle.
                gattServer.clearServices();
            }
        } else {
            Timber.w("No bluetooth manager, we must be simulating, or BT is off!!!");
            isStarted = false;
        }
        serverConnection = new GattServerConnection(gattServer, context.getMainLooper());
        // we ready.
        serverConnection.setState(GattState.IDLE);
        return isStarted;
    }

    /**
     * Will add a device that was discovered via the background scan in a provided scan result to
     * the connected devices map and will notify listeners of the availability of the new connection.  This will allow
     * an API user to add devices from scans that occur outside of the context of the periodical scanner.
     *
     * @param device The scan result from the background system scan
     */

    synchronized void addBackgroundScannedDeviceConnection(@Nullable FitbitBluetoothDevice device) {
        if (device != null) {
            device.origin = FitbitBluetoothDevice.DeviceOrigin.SCANNED;
            addScannedDevice(device);
        } else {
            Timber.w("No result provided.");
        }
    }

    /**
     * Will add a device that was discovered via the background scan in a provided scan result to
     * the connected devices map and will notify listeners of the availability of the new connection.  This will allow
     * an API user to add devices from scans that occur outside of the context of the periodical scanner.
     *
     * @param result The scan result from the background system scan
     */
    @SuppressWarnings("unused") // API Method
    public synchronized void addBackgroundScannedDeviceConnection(@Nullable ScanResult result) {
        if (result != null) {
            BluetoothDevice bluetoothDevice = result.getDevice();
            FitbitBluetoothDevice device = new FitbitBluetoothDevice(bluetoothDevice);
            device.origin = FitbitBluetoothDevice.DeviceOrigin.SCANNED;
            device.setRssi(result.getRssi());
            addScannedDevice(device);
        } else {
            Timber.w("No result provided.");
        }
    }

    /**
     * To provide an API to attempt to always find a bluetooth device that the caller wants to know
     * is in close proximity.  If this is attempted on a version prior to Android Oreo,
     * the result will be a no-op and will return false.  This API can be used to remain connected
     * to a particular set of devices.  The type of scan that occurs via this API is a privileged
     * scan that can run generally forever, but is not entirely under our control.
     * This scan is run by the system, so the resulting broadcast intent's content RE the bluetooth
     * device may change with the Android version.  Be advised that this scan will be automatically
     * cancelled if the user turns bluetooth off.
     *
     * @param context         The Android context for creating the pending intent
     * @param broadcastIntent The broadcast intent to be sent when the device is found.
     *                        Will wake up application if process is dead.
     * @param macAddresses    The specific mac addresses for which to be called back
     * @return The pending intent that should be used to cancel the scan if desired, or null
     * if the scan wasn't started.
     */
    @SuppressWarnings({"WeakerAccess", "unused"}) // API Method
    public PendingIntent startBackgroundScan(@NonNull Context context, @NonNull Intent broadcastIntent, @NonNull List<String> macAddresses) {
        if(alwaysConnectedScanner != null && alwaysConnectedScanner.isAlwaysConnectedScannerEnabled()) {
            Timber.i("You are using the always connected scanner, stop it first before ad-hoc scanning");
            return null;
        }
        if (isStarted() && peripheralScanner != null) {
            return peripheralScanner.startBackgroundScan(macAddresses, broadcastIntent, context);
        } else {
            Timber.w("The FitbitGatt must have been started in order to use the background scanner.");
            return null;
        }
    }

    /**
     * Will start a background scan that will continue to run even if our process is killed.  This
     * will internally handle the result of particular intent based scan results and deliver
     * connection callbacks when items are found.  In order to start a pending intent based scan you will
     * need to stop any existing high-priority scan in order to enable the pending intent based scan, the intended
     * use of this API is for scanning while your application is in the background.  When you
     * come into the foreground, you should cancel the background scan
     * with {@link FitbitGatt#stopSystemManagedPendingIntentScan()} unless you want for
     * the background scan to continue. Be advised that this might result in multiple callbacks to
     * {@link FitbitGatt.FitbitGattCallback#onBluetoothPeripheralDiscovered(GattConnection)}.
     * <p>
     * This background scan will be auto cancelled by the Android operating system in a way that we
     * can not control if BT is turned off or if the phone is rebooted.  This is a function of the
     * pending intent scan Android API.
     * <p>
     * WARNING!!!! Using this with scan filters that are empty is extremely dangerous and is frowned upon
     * your application will potentially get hundreds of intent callbacks every second.  Please do
     * not use this to get around the scanfilter empty check.
     *
     * @param context     The Android context for creating the pending intent
     * @param scanFilters The specific scan filters for which to be called back
     * @return true if the scan was able to be started, false if not
     */
    public boolean startSystemManagedPendingIntentScan(@NonNull Context context, @NonNull List<ScanFilter> scanFilters) {
        if(alwaysConnectedScanner.isAlwaysConnectedScannerEnabled()) {
            Timber.i("You are using the always connected scanner, stop it first before ad-hoc scanning");
            return false;
        }
        if (isStarted() && peripheralScanner != null) {
            return peripheralScanner.startPendingIntentBasedBackgroundScan(scanFilters, context);
        } else {
            Timber.i("Can't start because we aren't started, or the scanner is null");
            return false;
        }
    }

    /**
     * Will stop the currently running pending intent based scan
     */
    public void stopSystemManagedPendingIntentScan() {
        if(alwaysConnectedScanner.isAlwaysConnectedScannerEnabled()) {
            Timber.i("You are using the always connected scanner, stop it first before ad-hoc scanning");
            return;
        }
        if (isStarted() && peripheralScanner != null) {
            try {
                peripheralScanner.cancelPendingIntentBasedBackgroundScan();
            } catch (NoSuchMethodError error) {
                Timber.i(error, "There was a no such method error stopping the pending intent scan, assuming stopped.");
            }
        } else {
            Timber.i("Can't stop because we aren't started, or the scanner is null");
        }
    }

    /**
     * Will stop the background scan started earlier by {@link FitbitGatt#startBackgroundScan(Context, Intent, List)}
     *
     * @param pendingIntent The pending intent returned by {@link FitbitGatt#startBackgroundScan(Context, Intent, List)}
     */
    @SuppressWarnings("unused") // API Method
    public void stopBackgroundScan(@Nullable PendingIntent pendingIntent) {
        if(alwaysConnectedScanner.isAlwaysConnectedScannerEnabled()) {
            Timber.i("You are using the always connected scanner, stop it first before ad-hoc scanning");
            return;
        }
        if (pendingIntent == null) {
            Timber.v("No pending intent.");
        } else {
            if (peripheralScanner != null) {
                peripheralScanner.stopBackgroundScan(pendingIntent);
            } else {
                Timber.w("Peripheral scanner was null, did you forget to call FitbitGatt#start?");
            }
        }
    }

    /**
     * Will stop the background scan with a regular intent
     *
     * @param context       The android context
     * @param regularIntent The regular intent
     */
    @SuppressWarnings("unused") // API Method
    public void stopBackgroundScanWithRegularIntent(Context context, @Nullable Intent regularIntent) {
        if(alwaysConnectedScanner.isAlwaysConnectedScannerEnabled()) {
            Timber.i("You are using the always connected scanner, stop it first before ad-hoc scanning");
            return;
        }
        if (regularIntent == null) {
            Timber.v("No intent.");
        } else {
            PendingIntent pending;
            try {
                pending = PendingIntent.getBroadcast(context,
                    BACKGROUND_SCAN_REQUEST_CODE, regularIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            } catch (NoSuchMethodError error) {
                Timber.i(error, "There was a no such method error stopping the pending intent scan, assuming stopped");
                return;
            }
            if (peripheralScanner != null && pending != null) {
                peripheralScanner.stopBackgroundScan(pending);
            } else {
                Timber.w("Peripheral scanner was null, did you forget to call FitbitGatt#start?");
            }
        }
    }

    public GattServerConnection getServer() {
        return serverConnection;
    }


    public @Nullable
    GattConnection getConnection(BluetoothDevice device) {
        FitbitBluetoothDevice wrappedDevice = new FitbitBluetoothDevice(device);
        return getConnection(wrappedDevice);
    }

    /**
     * This method will create a new connection if one does not already exist for the provided
     * device and add it to the connection map
     *
     * @param device The fitbit bluetooth device
     * @return The connection, it can be null if no connection is in the map, this is necessary to prevent too many client_ifs from races
     */
    public @Nullable
    GattConnection getConnection(FitbitBluetoothDevice device) {
        return connectionMap.get(device);
    }

    @TargetApi(24)
    @SuppressWarnings("WeakerAccess") // API Method
    protected BluetoothAdapter getAdapter(Context context) {
        if (atLeastSDK(Build.VERSION_CODES.M)) {
            BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            assert manager != null;
            return manager.getAdapter();
        } else {
            return BluetoothAdapter.getDefaultAdapter();
        }
    }

    /**
     * Returns the BluetoothDevice Object for a remote Device that has the mac Address specified
     * <p> The mac Address should be a valid, such as "00:43:A8:23:10:F0"
     * Alphabetic characters must be uppercase to be valid.
     *
     * @param macAddress the mac address of the bluetooth device in question
     * @return The bluetooth device or null if not connected
     */
    public BluetoothDevice getBluetoothDevice(String macAddress) {
        BluetoothAdapter adapter = getAdapter(appContext);
        if (adapter != null && BluetoothAdapter.checkBluetoothAddress(macAddress)) {
            return adapter.getRemoteDevice(macAddress);
        }
        return null;
    }

    /**
     * Simple function to return true if the current device is running the given API Level or higher.
     * Recommended to use as a static import when comparing api levels
     *
     * @param buildVersion the buildVersion or API Level as defined by android.os.Build.VERSION_CODES.
     * @return true if the current device's API level is equal to or greater than the given API Level code
     */
    public static boolean atLeastSDK(int buildVersion) {
        return Build.VERSION.SDK_INT >= buildVersion;
    }


    @Override
    public void onScanStatusChanged(boolean isScanning) {
        Timber.i("On scan status changed %b", isScanning);
        if (isScanning) {
            for (FitbitGattCallback callback : this.overallGattEventListeners) {
                callback.onScanStarted();
            }
        } else {
            for (FitbitGattCallback callback : this.overallGattEventListeners) {
                callback.onScanStopped();
            }
        }
    }

    @Override
    public void onPendingIntentScanStatusChanged(boolean isScanning) {
        if (isScanning) {
            for (FitbitGattCallback callback : this.overallGattEventListeners) {
                callback.onPendingIntentScanStarted();
            }
        } else {
            for (FitbitGattCallback callback : this.overallGattEventListeners) {
                callback.onPendingIntentScanStopped();
            }
        }
    }

    @Override
    public void onFitbitDeviceFound(FitbitBluetoothDevice device) {
        addScannedDevice(device);
    }

    void removeServiceWithIdFromServicesToAdd(UUID serviceId) {
        synchronized (servicesToAdd) {
            for (BluetoothGattService service : servicesToAdd) {
                if (service.getUuid().equals(serviceId)) {
                    servicesToAdd.remove(service);
                }
            }
        }
    }

    void addServicesToGattServer() {
        synchronized (servicesToAdd) {
            AddGattServerServiceTransaction transaction = new AddGattServerServiceTransaction(getServer(), GattState.ADD_SERVICE_SUCCESS, servicesToAdd.get(0));
            getServer().runTx(transaction, result -> Timber.d("Gatt server init add service result: %s", result));
        }
    }

    /**
     * Clean up the states of the devices, the bluetooth adapter is turning off so we will have to
     * mark them all as disconnected and start the TTL
     */
    private void cleanUpBecauseBluetoothIsTurningOff() {
        for (Map.Entry<FitbitBluetoothDevice, GattConnection> entry : getConnectionMap().entrySet()) {
            cleanUpConnection(entry.getValue());
        }
        // need to clean up scanner also, if BT turns off then we can no longer be scanning
        if (getPeripheralScanner() != null) {
            if (getPeripheralScanner().isPendingIntentScanning() || getPeripheralScanner().isScanning()) {
                // we will try to clean up the normal way
                getPeripheralScanner().cancelPendingIntentBasedBackgroundScan();
                getPeripheralScanner().cancelScan(appContext);
            }
        }
    }

    private void cleanUpConnection(GattConnection conn) {
        // by setting the state to bt_off no additional transactions can run except for
        // connect.  The existing transaction might timeout in this case, but this seems to be
        // the best way.
        conn.cleanUpConnection();
        conn.justClearGatt();
        conn.setState(GattState.BT_OFF);

    }

    private void switchAllConnectionsToDisconnectedBecauseBtIsOn() {
        for (Map.Entry<FitbitBluetoothDevice, GattConnection> entry : getConnectionMap().entrySet()) {
            entry.getValue().setState(GattState.DISCONNECTED);
        }
    }

    @Override
    public void bluetoothOff() {
        isBluetoothOn = false;
        Timber.v("Bluetooth is off");
        cleanUpBecauseBluetoothIsTurningOff();
        for (FitbitGattCallback callback : overallGattEventListeners) {
            callback.onBluetoothOff();
        }
    }

    @Override
    public void bluetoothOn() {
        isBluetoothOn = true;
        /*
         * In testing we see that sometimes there will be a dead object exception from the stack
         * after bluetooth is turned off and then on again.  It seems that the IBluetoothGatt is
         * no longer connected to any process, to make sure that we don't have this situation, let's
         * replace the instance.
         *
         */
        if (this.appContext == null || !startServer(this.appContext)) {
            Timber.e("Refreshing the gatt server after BT was enabled failed");
        }
        if(peripheralScanner != null) {
            peripheralScanner.recycleLeScanner();
        }
        Timber.v("Bluetooth is on");
        switchAllConnectionsToDisconnectedBecauseBtIsOn();
        for (FitbitGattCallback callback : overallGattEventListeners) {
            callback.onBluetoothOn();
        }
    }

    @Override
    public void bluetoothTurningOff() {
        isBluetoothOn = false;
        Timber.v("Bluetooth is turning off");
        for (FitbitGattCallback callback : overallGattEventListeners) {
            callback.onBluetoothTurningOff();
        }
        // you can not cancel the scan here because if you do, there is a chance that the actual
        // scanner implementation inside of the adapter could become null, even if you cache
        // the reference, like in IPD-103133 where we set it and in the next call it's null
        cleanUpBecauseBluetoothIsTurningOff();
    }

    @Override
    public void bluetoothTurningOn() {
        // still can't use it until it's on
        isBluetoothOn = false;
        Timber.v("Bluetooth is turning on");
        for (FitbitGattCallback callback : overallGattEventListeners) {
            callback.onBluetoothTurningOn();
        }
    }
}
