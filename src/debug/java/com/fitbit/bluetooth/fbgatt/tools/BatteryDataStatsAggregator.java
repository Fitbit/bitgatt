/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tools;

import com.fitbit.bluetooth.fbgatt.BuildConfig;
import com.fitbit.bluetooth.fbgatt.FitbitGatt;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.health.HealthStats;
import android.os.health.SystemHealthManager;
import android.os.health.UidHealthStats;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import androidx.annotation.Nullable;
import timber.log.Timber;

/**
 * This class is designed to collect performance stats when running the performanceTest variant.
 * Performance stats in this case are realistically around battery and time consumption for different
 * areas of BTLE operation
 * <p>
 * Created by iowens on 2019-07-09
 */

class BatteryDataStatsAggregator {
    private static final long SNAPSHOT_INTERVAL = TimeUnit.SECONDS.toMillis(10);
    private static final String TAG = "BATTERY";
    private @Nullable
    Handler mainHandler;
    private AtomicLong btTxBytes = new AtomicLong(0);
    private AtomicLong btRxBytes = new AtomicLong(0);
    private AtomicLong bluetoothPowerMilliampMilliseconds = new AtomicLong(0);
    private @Nullable
    SystemHealthManager healthManager;

    @TargetApi(Build.VERSION_CODES.N)
    BatteryDataStatsAggregator(@Nullable Context context) {
        if (context != null && BuildConfig.BUILD_TYPE.equals("batteryDataTest") && FitbitGatt.atLeastSDK(Build.VERSION_CODES.N)) {
            healthManager = (SystemHealthManager) context.getSystemService(Context.SYSTEM_HEALTH_SERVICE);
            mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.postDelayed(this::snapshotAndLogMetrics, SNAPSHOT_INTERVAL);
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    @SuppressWarnings("CatchGeneralException")
    // because takeMyUidSnapshot wraps RemoteException in a RuntimeException
    private void snapshotAndLogMetrics() {
        if (healthManager == null || mainHandler == null || !FitbitGatt.atLeastSDK(Build.VERSION_CODES.N)) {
            Timber.v("Heath logging not enabled.");
            return;
        }
        HealthStats currentStats = null;
        try {
            currentStats = healthManager.takeMyUidSnapshot();
        } catch (RuntimeException e) {
            Timber.tag(TAG).e(e, "Couldn't snapshot");
        }
        if (currentStats == null) {
            Timber.tag(TAG).w("Couldn't log snapshot");
            return;
        }
        long oldTxBytes = btTxBytes.getAndSet(currentStats.getMeasurement(UidHealthStats.MEASUREMENT_BLUETOOTH_TX_BYTES));
        long oldRxBytes = btRxBytes.getAndSet(currentStats.getMeasurement(UidHealthStats.MEASUREMENT_BLUETOOTH_RX_BYTES));
        long oldMamsValue = bluetoothPowerMilliampMilliseconds.getAndAdd(currentStats.getMeasurement(UidHealthStats.MEASUREMENT_BLUETOOTH_POWER_MAMS));
        float oldMah = ((((float) oldMamsValue / 1000) * 60) * 60);
        float newMah = ((((float) bluetoothPowerMilliampMilliseconds.get() / 1000) * 60) * 60);
        mainHandler.postDelayed(this::snapshotAndLogMetrics, SNAPSHOT_INTERVAL);
        Timber
                .tag(TAG)
                .i("Old BTLE TX: %d, new BTLE TX: %d. Old BTLE RX: %d, new BTLE RX: %d. Old BTLE mAh: %f, new BTLE mAh: %f",
                        oldTxBytes, btTxBytes.get(), oldRxBytes, btRxBytes.get(), oldMah, newMah);
    }

}
