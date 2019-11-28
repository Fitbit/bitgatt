/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.exception.BitGattStartException;
import com.fitbit.bluetooth.fbgatt.util.GattUtils;
import com.fitbit.bluetooth.fbgatt.util.ScanFailedReason;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.List;

import androidx.annotation.NonNull;
import timber.log.Timber;

/**
 * It is important to remember that this class may be instantiated by the system, and we may
 * have been dead in the background, so no assumptions about runtime state should be made
 * by any calls herein.
 *
 * This class needs to be public because we receive this as a global broadcast. Why can't we dynamically
 * register the receiver for this? Because dynamically registered receivers are only valid for as long
 * as their contexts are valid, if the app dies so does the receiver unless registered in the manifest,
 * and callable by the system.
 */

public class HandleIntentBasedScanResult extends BroadcastReceiver {


    @TargetApi(Build.VERSION_CODES.O)
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            // need to use the adapter directly because bitgatt may not have been instantiated
            BluetoothAdapter adapter = new GattUtils().getBluetoothAdapter(context);
            if(adapter == null || !adapter.isEnabled()) {
                Timber.w("Bluetooth is turned off, ignoring");
                return;
            }
            if (!FitbitGatt.getInstance().isInitialized()) {
                Timber.v("Bitgatt wasn't started, starting before handling...");
            }
            Timber.v("Received connection update : %s", intent.getAction());
            // added by the system to the intent defined in {@link OreoBackgroundScanner#explicitIntentHelper}
            int callbackType = intent.getIntExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, ScanSettings.CALLBACK_TYPE_FIRST_MATCH);
            // added by the system to the intent defined in {@link OreoBackgroundScanner#explicitIntentHelper}
            int errorCode = intent.getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode());
            if (errorCode == ScanFailedReason.SCAN_SUCCESS_NO_ERROR.getCode()) {
                // added by the system to the intent defined in {@link OreoBackgroundScanner#explicitIntentHelper}
                List<ScanResult> results = intent.getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);
                if (results != null && !results.isEmpty()) {
                    // there are callback results, so now we should do something with this, it's
                    // not OK to do this on the main thread so we'll jump onto a scheduler
                    for(ScanResult result : results) {
                        GattClientCallback callbackClient = FitbitGatt.getInstance().getClientCallback();
                        if (callbackClient != null) {
                            callbackClient.getClientCallbackHandler().post(() -> {
                                FitbitBluetoothDevice fitBd = new FitbitBluetoothDevice(result.getDevice());
                                fitBd.setScanRecord(result.getScanRecord());
                                fitBd.origin = FitbitBluetoothDevice.DeviceOrigin.SCANNED;
                                fitBd.setRssi(result.getRssi());
                                /*
                                 * If bitgatt is started, then we need to determine whether we are still
                                 * pending intent scanning from a different start and set the correct
                                 * state internally.  If we are not started, then is pending intent scanning
                                 * will be false because the scanner will have been null, so the next scan event
                                 * we will enter is started and is pending intent scanning false, so we will
                                 * update the state.
                                 */
                                if (FitbitGatt.getInstance().isInitialized()) {
                                    if (FitbitGatt.getInstance().isPendingIntentScanning()) {
                                        Timber.v("Bitgatt is started and scanning, so we should add %s", fitBd);
                                        FitbitGatt.getInstance().addBackgroundScannedDeviceConnection(fitBd);
                                    } else {
                                        Timber.v("Bitgatt is started, but is not intent scanning, so we may have died in the background adding %s and telling bitgatt that we are still intent scanning", fitBd);
                                        PeripheralScanner scanner = FitbitGatt.getInstance().getPeripheralScanner();
                                        if (scanner != null) {
                                            scanner.setIsPendingIntentScanning(true);
                                        } else {
                                            Timber.v("Tried to handle the event and update the scanner's internal state, but the scanner was null");
                                        }
                                        FitbitGatt.getInstance().addBackgroundScannedDeviceConnection(fitBd);
                                    }
                                } else {
                                    Timber.w("Bitgatt is not started, or we aren't pending intent scanning, let's try starting for %s", fitBd);
                                    // this will take us off of the main thread
                                    FitbitGatt.getInstance().registerGattEventListener(new FitbitGatt.FitbitGattCallback() {
                                        @Override
                                        public void onBluetoothPeripheralDiscovered(@NonNull GattConnection connection) {
                                            //no-op
                                        }

                                        @Override
                                        public void onBluetoothPeripheralDisconnected(@NonNull GattConnection connection) {
                                            //no-op
                                        }


                                        @Override
                                        public void onScanStarted() {
                                            //no-op
                                        }

                                        @Override
                                        public void onScanStopped() {
                                            //no-op
                                        }

                                        @Override
                                        public void onScannerInitError(BitGattStartException error) {
                                            //no-op
                                        }

                                        @Override
                                        public void onPendingIntentScanStopped() {
                                            //no-op
                                        }

                                        @Override
                                        public void onPendingIntentScanStarted() {
                                            //no-op
                                        }

                                        @Override
                                        public void onBluetoothOff() {
                                            //no-op
                                        }

                                        @Override
                                        public void onBluetoothOn() {
                                            //no-op
                                        }

                                        @Override
                                        public void onBluetoothTurningOn() {
                                            //no-op
                                        }

                                        @Override
                                        public void onBluetoothTurningOff() {
                                            //no-op
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
                                    });
                                }
                            });
                        }
                    }
                } else {
                    Timber.w("Scan callback with no results");
                }
            } else {
                Timber.v("There was an error in the background scan of ScanCallback.SCAN_FAILED_* const type %s", ScanFailedReason.getReasonForCode(errorCode));
                Timber.v("The callback type was ScanSettings.CALLBACK_TYPE_* %d", callbackType);
            }
        } else {
            Timber.v("The intent service was started with a null intent.  We are probably initializing");
        }
    }
}
