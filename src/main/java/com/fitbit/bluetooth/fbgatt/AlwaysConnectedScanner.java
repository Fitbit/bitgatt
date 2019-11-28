/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.exception.BitGattStartException;
import com.fitbit.bluetooth.fbgatt.tx.GattClientDiscoverServicesTransaction;
import com.fitbit.bluetooth.fbgatt.tx.GattConnectTransaction;
import com.fitbit.bluetooth.fbgatt.tx.GattDisconnectTransaction;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import timber.log.Timber;

/**
 * This scanner is designed to remain connected to one or more bluetooth peripherals through
 * bluetooth toggles, etc ...  Changing the filters will internally stop and start the scanner.
 * <p>
 * This is a common use-case for wearable peripherals, and for power reasons it is preferred to
 * remain connected rather than to continuously scan and connect.
 * <p>
 * Please see the README.md for more detailed instructions
 * <p>
 * Created by iowens on 6/10/19.
 */
@SuppressWarnings("unused") // API Method
public class AlwaysConnectedScanner implements FitbitGatt.FitbitGattCallback {
    /**
     * Time between high priority scans
     */
    private static final long MIN_TIME_BETWEEN_LOW_LATENCY_SCANS = TimeUnit.MINUTES.toMillis(5);
    /**
     * The maximum amount of time allowed for a high-priority scan, there is no reason that any peripheral
     * should require more than ten seconds of high priority scanning
     */
    private static final long MAX_TIME_FOR_HIGH_PRIORITY_SCAN = TimeUnit.SECONDS.toMillis(10);
    /**
     * Whether this is test mode or not
     */
    private boolean testMode;
    /**
     * Whether we should keep looking even after a device connects ( we will know by ACL )
     */
    private boolean shouldKeepLooking;
    /**
     * How many devices should we be looking for, if less than 1 then this feature is disabled
     * disabled and the system will keep scanning regardless of shouldKeepLooking
     */
    private int numberOfExpectedDevices;
    /**
     * The number of devices that we have actually matched and connected
     */
    private AtomicInteger numberOfMatchingConnectedDevices = new AtomicInteger(0);
    /**
     * The pointer to the peripheral scanner
     */
    private @Nullable
    PeripheralScanner scanner;
    /**
     * The scan filters to find the devices we are looking for
     */
    private CopyOnWriteArrayList<ScanFilter> scanFilters = new CopyOnWriteArrayList<>();
    /**
     * The connected scanner listeners
     */
    private CopyOnWriteArrayList<AlwaysConnectedScannerListener> listeners = new CopyOnWriteArrayList<>();
    /**
     * Whether the scanner is enabled or not
     */
    private AtomicBoolean isScannerEnabled = new AtomicBoolean(false);
    /**
     * Will indicate whether a high priority scan can be performed
     */
    private AtomicBoolean canStartHighPriorityScan = new AtomicBoolean(true);
    /**
     * Member for a main handler for scheduling
     */
    private Handler mainHandlerForScheduling;
    /**
     * Test mode high priority scan time, shouldn't be used for production purposes
     */
    @VisibleForTesting
    static final long MAX_TIME_FOR_TEST_MODE_HIGH_PRIORITY_SCAN = TimeUnit.SECONDS.toMillis(2);

    /**
     * Only here for mocking purposes
     */

    @VisibleForTesting
    AlwaysConnectedScanner(){

    }

    /**
     * Will set the initial expectation for how this scanner should operate when devices are found
     *
     * @param numberOfExpectedDevices The number of expected devices
     * @param shouldKeepLooking       Whether to keep looking after one of the devices is found
     * @param looper                  The looper for scheduling events
     */
    @SuppressWarnings("unused")
    // API Method
    AlwaysConnectedScanner(int numberOfExpectedDevices, boolean shouldKeepLooking, Looper looper) {
        this(numberOfExpectedDevices, shouldKeepLooking, false, looper);
    }

    private AlwaysConnectedScanner(int numberOfExpectedDevices, boolean shouldKeepLooking, boolean testMode, Looper looper) {
        this.numberOfExpectedDevices = numberOfExpectedDevices;
        this.shouldKeepLooking = shouldKeepLooking;
        this.testMode = testMode;
        mainHandlerForScheduling = new Handler(looper);
    }

    @VisibleForTesting
    void setHandler(Handler hand) {
        mainHandlerForScheduling = hand;
    }

    /**
     * We need to be able to do this for testing
     */
    @VisibleForTesting
    void restartCanStart() {
        canStartHighPriorityScan.set(true);
    }

    /**
     * Will register a listener for always connected events if not already registered, if the instance
     * is already registered, then it will be ignored
     *
     * @param listener The always connected scanner listener for callbacks
     */
    @SuppressWarnings("unused") // API Method
    public void registerAlwaysConnectedScannerListener(AlwaysConnectedScannerListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Will unregister an always connected listener
     *
     * @param listener The always connected scanner listener for callbacks
     */
    @SuppressWarnings("unused") // API Method
    public void unregisterAlwaysConnectedScannerListener(AlwaysConnectedScannerListener listener) {
        listeners.remove(listener);
    }

    /**
     * Will fetch the number of expected devices
     *
     * @return the number of expected devices
     */
    @SuppressWarnings("unused") // API Method
    public int getNumberOfExpectedDevices() {
        return numberOfExpectedDevices;
    }

    /**
     * Will establish whether this scanner should keep looking for matches
     *
     * @return whether to keep looking once a single matching device is connected
     */
    @SuppressWarnings("unused") // API Method
    public boolean shouldKeepLooking() {
        return shouldKeepLooking;
    }

    /**
     * Will set whether the scanner should keep looking when device is connected
     *
     * @param shouldKeepLooking true if the scanner should
     */
    @SuppressWarnings("unused") // API Method
    public void setShouldKeepLooking(boolean shouldKeepLooking) {
        this.shouldKeepLooking = shouldKeepLooking;
    }

    /**
     * Will return the state of this scanner, as to whether it is in operation or not
     *
     * @return true if this scanner is enabled, false if it is not
     */
    @SuppressWarnings("unused") // API Method
    public boolean isAlwaysConnectedScannerEnabled() {
        return isScannerEnabled.get();
    }

    /**
     * Will set the number of expected devices, will be picked up once the next set of scans begin
     *
     * @param numberOfExpectedDevices The number of expected devices
     */
    @SuppressWarnings("unused") // API Method
    public void setNumberOfExpectedDevices(int numberOfExpectedDevices) {
        this.numberOfExpectedDevices = numberOfExpectedDevices;
    }

    /**
     * Will indicate whether the always connected scanner is in the test mode or not
     *
     * @return true if in test mode, false if not
     */
    @SuppressWarnings("unused")
    // API Method
    boolean isInTestMode() {
        return testMode;
    }

    /**
     * Will add all provided scan filters, intended to only be used internally
     *
     * @param filters The list of filters to be added
     */
    void setScanFilters(List<ScanFilter> filters) {
        this.scanFilters.addAll(filters);
    }

    /**
     * Will set the system up in test mode
     *
     * @param testMode true for test mode, false if not
     */
    void setTestMode(boolean testMode) {
        this.testMode = testMode;
    }

    /**
     * Start the scanner with filters
     *
     * @param context The android context
     * @param filters The scan filters to use
     * @return True if started successfully
     */
    @SuppressWarnings("unused") // API Method
    public boolean startWithFilters(@NonNull Context context, @NonNull List<ScanFilter> filters) {
        scanFilters.addAll(filters);
        return start(context);
    }

    /**
     * Will add a list of filters to the existing set of filters, will not check for duplicates
     * and will not clear the existing filter set.  This method will only change the current set of
     * filters once every 30s so if you call this method multiple times, the changes will be spread
     * over 30s x n calls.
     *
     * @param context The android context
     * @param filters The list of filters
     */
    public synchronized void addScanFilters(@NonNull Context context, @NonNull List<ScanFilter> filters) {
        scanFilters.addAll(filters);
        if (FitbitGatt.getInstance().getPeripheralScanner() != null) {
            FitbitGatt.getInstance().getPeripheralScanner().setScanFilters(scanFilters);
        }
        // let's only change this once per scan too much warn interval
        mainHandlerForScheduling.postDelayed(() -> {
            FitbitGatt.getInstance().getPeripheralScanner().cancelPendingIntentBasedBackgroundScan();
            FitbitGatt.getInstance().getPeripheralScanner().startPendingIntentBasedBackgroundScan(scanFilters, context);
        }, PeripheralScanner.SCAN_TOO_MUCH_WARN_INTERVAL);
    }

    /**
     * Will append a new scan filter to the set of scan filters, will restart the internal
     * background scanner to ensure that the new filter is picked up.  Since we want to always
     * avoid the scan-too-much no-op for our scanner, we will only change this once per 30s.
     * <p>
     * This method will only change the current set of
     * filters once every 30s so if you call this method multiple times, the changes will be spread
     * over 30s x n calls.
     *
     * @param context    The android context
     * @param scanFilter The new scan filter to add
     */

    public synchronized void addScanFilter(@NonNull Context context, @NonNull ScanFilter scanFilter) {
        if (!scanFilters.contains(scanFilter)) {
            scanFilters.add(scanFilter);
        }
        if (FitbitGatt.getInstance().getPeripheralScanner() != null) {
            FitbitGatt.getInstance().getPeripheralScanner().setScanFilters(scanFilters);
        }
        // let's only change this once per scan too much warn interval
        mainHandlerForScheduling.postDelayed(() -> {
            FitbitGatt.getInstance().getPeripheralScanner().cancelPendingIntentBasedBackgroundScan();
            FitbitGatt.getInstance().getPeripheralScanner().startPendingIntentBasedBackgroundScan(scanFilters, context);
        }, PeripheralScanner.SCAN_TOO_MUCH_WARN_INTERVAL);
    }

    /**
     * Will remove a scan filter from the set of scan filters.
     * <p>
     * This method will only change the current set of
     * filters once every 30s so if you call this method multiple times, the changes will be spread
     * over 30s x n calls.
     *
     * @param context    The android context
     * @param scanFilter The scan filter to remove
     */

    public synchronized void removeScanFilter(@NonNull Context context, @NonNull ScanFilter scanFilter) {
        if (!scanFilters.contains(scanFilter)) {
            scanFilters.remove(scanFilter);
        }
        if (FitbitGatt.getInstance().getPeripheralScanner() != null) {
            FitbitGatt.getInstance().getPeripheralScanner().setScanFilters(scanFilters);
        }
        // let's only change this once per scan too much warn interval
        mainHandlerForScheduling.postDelayed(() -> {
            FitbitGatt.getInstance().getPeripheralScanner().cancelPendingIntentBasedBackgroundScan();
            FitbitGatt.getInstance().getPeripheralScanner().startPendingIntentBasedBackgroundScan(scanFilters, context);
        }, PeripheralScanner.SCAN_TOO_MUCH_WARN_INTERVAL);
    }

    /**
     * Will start finding and connecting to devices that match the filters.  Will not deal with matching
     * presently connected devices at the time of the start call.
     *
     * @param context The android application context
     * @return true if the scanner can start searching, false if the raw scanner is in use or the filters are insufficient
     */

    public boolean start(@NonNull Context context) {
        scanner = FitbitGatt.getInstance().getPeripheralScanner();
        if (scanner == null) {
            Timber.w("The scanner isn't set up yet, did you call FitbitGatt#start(...)?");
            return false;
        }
        if (scanner.isScanning() || scanner.isPeriodicalScanEnabled() || scanner.isPendingIntentScanning()) {
            Timber.w("You can not start an always connected scanner while using the regular scanner");
            return false;
        }
        if (scanFilters.isEmpty()) {
            Timber.w("You can not start a scanner with no filters");
            return false;
        }
        if (isScannerEnabled.get()) {
            Timber.w("The scanner was already enabled, no need to call this again");
            return false;
        }
        FitbitGatt.getInstance().registerGattEventListener(this);
        return startBackgroundScanning(context);
    }

    private boolean startBackgroundScanning(@NonNull Context context) {
        // we only want to start a pending intent scan if this is > Oreo MR1
        // otherwise we'll use the pending intent scan
        PeripheralScanner peripheralScanner = FitbitGatt.getInstance().getPeripheralScanner();
        if (peripheralScanner == null) {
            Timber.w("The scanner isn't set up yet, did you call FitbitGatt#start(...)?");
            return false;
        }
        if (FitbitGatt.atLeastSDK(Build.VERSION_CODES.O_MR1)) {
            peripheralScanner.startPendingIntentBasedBackgroundScan(scanFilters, context);
        } else {
            // this will start a low intensity scan immediately
            boolean didStart = peripheralScanner.startPeriodicScan(context);
            if (!didStart) {
                isScannerEnabled.set(false);
                return false;
            }
        }
        isScannerEnabled.set(true);
        return true;
    }

    private void stopScanningUntilDisconnectionEvent(Context context) {
        if (isAlwaysConnectedScannerEnabled()) {
            PeripheralScanner peripheralScanner = FitbitGatt.getInstance().getPeripheralScanner();
            if (peripheralScanner != null) {
                if (FitbitGatt.atLeastSDK(Build.VERSION_CODES.O_MR1)) {
                    peripheralScanner.cancelPendingIntentBasedBackgroundScan();
                } else {
                    peripheralScanner.cancelPeriodicalScan(context);
                }
                peripheralScanner.cancelHighPriorityScan(context);
                peripheralScanner.cancelScan(context);
                Timber.v("Stopped all of the scanning");
            } else {
                Timber.w("The scanner isn't set up yet, did you call FitbitGatt#start(...)?");
            }
        } else {
            Timber.w("The scanner was disabled so we will not continue");
        }
    }

    private boolean continueBackgroundScanning(@NonNull Context context) {
        if (isAlwaysConnectedScannerEnabled()) {
            PeripheralScanner peripheralScanner = FitbitGatt.getInstance().getPeripheralScanner();
            if (peripheralScanner != null) {
                if (FitbitGatt.atLeastSDK(Build.VERSION_CODES.O_MR1)) {
                    return peripheralScanner.startPendingIntentBasedBackgroundScan(scanFilters, context);
                } else {
                    return peripheralScanner.startPeriodicScan(context);
                }
            } else {
                Timber.w("The scanner isn't set up yet, did you call FitbitGatt#start(...)?");
                return false;
            }
        } else {
            Timber.w("The scanner was disabled so we will not continue");
            return false;
        }
    }

    /**
     * This should be performed due to some user interaction that requires as near a real-time
     * interaction with the peripheral as possible, can only be performed when the screen is on
     * and only once every 5 minutes.
     *
     * @return false if high priority scan was not started
     */
    @SuppressWarnings({"unused", "WeakerAccess"}) // API Method
    public boolean startHighPriorityScan(@NonNull Context context) {
        if (!canStartHighPriorityScan.get()) {
            Timber.w("You can't start a high priority scan right now");
            return false;
        }
        if (testMode) {
            return startScanIfPossible(context);
        } else {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm == null) {
                Timber.i("The power manager was null");
                return false;
            }
            if (pm.isInteractive()) {
                return startScanIfPossible(context);
            } else {
                Timber.d("The screen isn't on, can't perform a high priority scan");
                return false;
            }
        }
    }

    private boolean startScanIfPossible(@NonNull Context context) {
        PeripheralScanner peripheralScanner = FitbitGatt.getInstance().getPeripheralScanner();
        if (peripheralScanner == null) {
            Timber.w("The scanner isn't set up yet, did you call FitbitGatt#start(...)?");
            return false;
        }
        boolean didStart = peripheralScanner.startHighPriorityScan(context);
        if (!didStart) {
            Timber.w("Couldn't start a high priority scan");
            return false;
        }
        if (testMode) {
            mainHandlerForScheduling.postDelayed(() -> peripheralScanner.cancelHighPriorityScan(context), MAX_TIME_FOR_TEST_MODE_HIGH_PRIORITY_SCAN);
        } else {
            mainHandlerForScheduling.postDelayed(() -> peripheralScanner.cancelHighPriorityScan(context), MAX_TIME_FOR_HIGH_PRIORITY_SCAN);
        }
        Timber.v("Will start high priority scanning, but will cancel in %dms", MAX_TIME_FOR_HIGH_PRIORITY_SCAN);
        mainHandlerForScheduling.postDelayed(() -> canStartHighPriorityScan.set(true), MIN_TIME_BETWEEN_LOW_LATENCY_SCANS);
        canStartHighPriorityScan.set(false);
        return true;
    }

    /**
     * Will stop the operation of this scanner
     */

    public void stop(@NonNull Context context) {
        // will stop the always connected scanner
        FitbitGatt.getInstance().unregisterGattEventListener(this);
        PeripheralScanner peripheralScanner = FitbitGatt.getInstance().getPeripheralScanner();
        if (peripheralScanner != null) {
            peripheralScanner.cancelScan(context);
            peripheralScanner.cancelPendingIntentBasedBackgroundScan();
            peripheralScanner.cancelPeriodicalScan(context);
        } else {
            Timber.w("The scanner isn't set up yet, did you call FitbitGatt#start(...)?");
            return;
        }
        isScannerEnabled.set(false);
    }

    /* ---------------------------------- General GATT callbacks ------------------------------ */

    @SuppressLint("VisibleForTests")
    @Override
    public void onBluetoothPeripheralDiscovered(GattConnection connection) {
        // we've discovered a device that is matching, let's try to connect
        Context appContext = FitbitGatt.getInstance().getAppContext();
        if (appContext != null) {
            // well, we will try to connect and evaluate the device against our filters.  If it
            // matches, then we will leave it connected, otherwise we will release the client_if
            // if we are in mock mode, we should finish the connection mock
            if (testMode) {
                connection.setMockMode(true);
                connection.setState(GattState.CONNECTED);
                int matchedDevices = numberOfMatchingConnectedDevices.incrementAndGet();
                // if the number of expected matched devices has been hit and continue scanning is
                // not enabled, then the scans will be cancelled until the connected devices
                // drops below the expected number
                if (numberOfExpectedDevices > 0 && !shouldKeepLooking && matchedDevices >= numberOfExpectedDevices) {
                    stopScanningUntilDisconnectionEvent(appContext);
                }
                Timber.v("Connection %s matches, calling listeners", connection);
                for (AlwaysConnectedScannerListener listener : listeners) {
                    listener.onPeripheralConnected(connection);
                }
                Timber.v("Matched devices connected: %d", matchedDevices);
            } else {
                GattConnectTransaction tx = new GattConnectTransaction(connection, GattState.CONNECTED);
                connection.runTx(tx, result -> {
                    if (result.getResultStatus().equals(TransactionResult.TransactionResultStatus.SUCCESS)) {
                        Timber.v("Connected, now discovering");
                        // we will go ahead and discover, since we are going through the hassle of connecting
                        // it doesn't make sense to hand the caller a connection that isn't immediately usable
                        // shouldn't be a big deal if already discovered, will return from cache
                        GattClientDiscoverServicesTransaction discover = new GattClientDiscoverServicesTransaction(connection, GattState.DISCOVERY_SUCCESS);
                        connection.runTx(discover, result1 -> {
                            if (result1.getResultStatus().equals(TransactionResult.TransactionResultStatus.SUCCESS)) {
                                // let's match it against filters, this could be expensive, so let's do it on the
                                // connection thread
                                connection.getClientTransactionQueueController().queueTransaction(() -> {
                                    for (ScanFilter filter : scanFilters) {
                                        if (doesConnectionMatchFilter(connection, filter)) {
                                            int matchedDevices = numberOfMatchingConnectedDevices.incrementAndGet();
                                            // if the number of expected matched devices has been hit and continue scanning is
                                            // not enabled, then the scans will be cancelled until the connected devices
                                            // drops below the expected number
                                            if (numberOfExpectedDevices > 0 && !shouldKeepLooking && matchedDevices >= numberOfExpectedDevices) {
                                                stopScanningUntilDisconnectionEvent(appContext);
                                            }
                                            Timber.v("Connection %s matches, calling listeners", connection);
                                            for (AlwaysConnectedScannerListener listener : listeners) {
                                                listener.onPeripheralConnected(connection);
                                            }
                                            Timber.v("Matched devices connected: %d", matchedDevices);
                                            return;
                                        }
                                    }
                                    // if there is no match, we can remain silent about it
                                    GattDisconnectTransaction disconnect = new GattDisconnectTransaction(connection, GattState.DISCONNECTED);
                                    connection.runTx(disconnect, result11 -> Timber.v("No match, tried to disconnect with result : %s", result11));
                                });
                            }
                        });
                    } else {
                        for (AlwaysConnectedScannerListener listener : listeners) {
                            listener.onPeripheralConnectionError(result);
                        }
                    }
                });
            }
        }
    }

    @Override
    public void onBluetoothPeripheralDisconnected(GattConnection connection) {
        // realistically we don't need to do anything here except decrement matching devices
        connection.getClientTransactionQueueController().queueTransaction(() -> {
            for (ScanFilter filter : scanFilters) {
                if (doesConnectionMatchFilter(connection, filter)) {
                    int matchedDevices = numberOfMatchingConnectedDevices.get();
                    if (matchedDevices > 0) {
                        matchedDevices = numberOfMatchingConnectedDevices.decrementAndGet();
                        Timber.v("Matched devices decremented to: %d", matchedDevices);
                    }
                }
            }
            // if something disconnected and we are configured to continue to match, we will
            // continue scanning, if we are already scanning this will do nothing.
            int matchingDevices = numberOfMatchingConnectedDevices.get();
            if (numberOfExpectedDevices > 1 && matchingDevices < numberOfExpectedDevices) {
                Timber.v("Dropped below the number of expected devices, so we should start scanning again");
                if (FitbitGatt.getInstance().getAppContext() != null) {
                    boolean didStart = continueBackgroundScanning(FitbitGatt.getInstance().getAppContext());
                    if (!didStart) {
                        Timber.w("Always connected scanner tried to start scanning but failed");
                    }
                }
            }
        });
    }

    @Override
    public void onScanStarted() {
        // that's cool
        //no-op
    }

    @Override
    public void onScanStopped() {
        Timber.i("Always connected scanner scan stopped");
    }

    @Override
    public void onScannerInitError(BitGattStartException error) {
        //no-op
    }

    @Override
    public void onPendingIntentScanStopped() {
        // bluetooth on means that we should restart our pending intent scan if it is not already
        // in operation
        if (FitbitGatt.getInstance().getAppContext() != null) {
            startBackgroundScanning(FitbitGatt.getInstance().getAppContext());
        } else {
            Timber.w("Couldn't resume scans because context is null");
        }
    }

    @Override
    public void onPendingIntentScanStarted() {
        // that's cool too
    }

    @Override
    public void onBluetoothOff() {
        // bluetooth off will stop all scans, but will not reset the 30s scan count
        numberOfMatchingConnectedDevices.set(0);
        isScannerEnabled.set(false);
        canStartHighPriorityScan.set(true);
    }

    @Override
    public void onBluetoothOn() {
        // bluetooth on means that we should restart our pending intent scan if it is not already
        // in operation
        Timber.i("If BT is coming on you can assume that all of your scan state has been stopped");
    }

    @Override
    public void onBluetoothTurningOn() {

    }

    @Override
    public void onBluetoothTurningOff() {

    }

    @Override
    public void onGattServerStarted(GattServerConnection serverConnection) {
        //no-op
    }

    @Override
    public void onGattServerStartError(BitGattStartException error) {
        //no-op
    }

    @Override
    public void onGattClientStarted() {
        //no-op
    }

    @Override
    public void onGattClientStartError(BitGattStartException error) {
        //no-op
    }

    /**
     * Will determine if the cached scan result matches the provided scan filter
     *
     * @param connection The gatt connection
     * @param filter     The scan filter
     * @return true if it's a match, false if not
     */
    @TargetApi(26)
    private boolean doesConnectionMatchFilter(@NonNull GattConnection connection, ScanFilter filter) {
        ScanRecord record = connection.getDevice().getScanRecord();
        if (record == null) {
            return false;
        }
        ScanResult result;
        if (FitbitGatt.atLeastSDK(Build.VERSION_CODES.O)) {
            result = new ScanResult(connection.getDevice().device,
                0x00, 1, 0x00, 0xFF, 127,
                connection.getDevice().getRssi(), record.getTxPowerLevel(), record, 0);
        } else {
            result = new ScanResult(connection.getDevice().device, record, connection.getDevice().getRssi(),
                0);
        }
        return filter.matches(result);
    }
}
