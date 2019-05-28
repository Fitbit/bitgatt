/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.fitbit.bluetooth.fbgatt.strategies.DelaySubscriptionResultStrategy;
import com.fitbit.bluetooth.fbgatt.strategies.HandleTrackerVanishingUnderGattOperationStrategy;
import com.fitbit.bluetooth.fbgatt.strategies.Strategy;

import timber.log.Timber;

/**
 * Will provide specific strategies based on scenarios that should be run given the android mobile's
 * metadata around firmware version, device model, api level, etc ...
 * and a provided set of android mobile device features for which we should apply this strategy.
 *
 * Note: It is possible to utilize the peripheral name and address in the strategy selection
 * consideration, however at this time there are no strategies at this level that will need to take
 * the tracker product name into consideration.  If there is something that needs to consider
 * items like firmware version, tracker product id, or other business logic properties, it should
 * be strategized at a higher level of abstraction.
 */

public class StrategyProvider {

    private static final String UNMATCHABLE_DEVICE_NAME = "71E6CB80-BCD7-4F11-9433-66DB2D4ABE4E"; // guid from a long time ago

    /**
     * An unmatchable device that can be used for developers who wish to experiment with strategies
     * while still deploying to production.  Can be wrapped with Build.DEBUG or whatever
     * @return An unmatchable device
     */

    public AndroidDevice getUnmatchableDevice(){
        return new AndroidDevice.Builder().deviceModel(UNMATCHABLE_DEVICE_NAME).build();
    }
    /**
     * Will return appropriate strategy based on the current android device setup and whether the
     * passed in device has matching properties with the aforementioned android device.
     * @param strategyDevice The device with properties that match or don't match the current phone, null means greedy ( match everything for the provided situation )
     * @param conn The gatt connection associated with this strategy
     * @param situation The situation to use to determine which strategy to apply
     * @return The strategy or null if there is no match
     */
    public @Nullable
    Strategy getStrategyForPhoneAndGattConnection(@Nullable AndroidDevice strategyDevice, GattConnection conn, Situation situation) {
        AndroidDevice currentDevice = new AndroidDevice.Builder().
                device(Build.DEVICE).
                deviceModel(Build.MODEL).
                apiLevel(Build.VERSION.SDK_INT).
                board(Build.BOARD).
                bootloader(Build.BOOTLOADER).
                brand(Build.BRAND).
                display(Build.DISPLAY).
                fingerprint(Build.FINGERPRINT).
                hardware(Build.HARDWARE).
                host(Build.HOST).
                id(Build.ID).
                manufacturerName(Build.MANUFACTURER).
                product(Build.PRODUCT).
                radioVersion(Build.getRadioVersion()).
                type(Build.TYPE).build();
                return getStrategyForPhoneAndGattConnection(currentDevice, strategyDevice, conn, situation);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    Strategy getStrategyForPhoneAndGattConnection(AndroidDevice currentDevice, @Nullable AndroidDevice strategyDevice, GattConnection conn, Situation situation) {
        if (currentDeviceHasEqualDefinedProperties(currentDevice, strategyDevice)) {
            switch (situation) {
                case TRACKER_WENT_AWAY_DURING_GATT_OPERATION:
                    return new HandleTrackerVanishingUnderGattOperationStrategy(conn, currentDevice);
                case DELAY_ANDROID_SUBSCRIPTION_EVENT:
                    return new DelaySubscriptionResultStrategy(conn, currentDevice);
                default:
                    return null;
            }
        } else {
            Timber.d("[%s] Target android device does not match, no need for strategy", conn.getDevice());
            return null;
        }
    }

    @VisibleForTesting
    boolean currentDeviceHasEqualDefinedProperties(AndroidDevice currentDevice, @Nullable AndroidDevice strategyDevice) {
        if(strategyDevice == null) {
            return true;
        }
        boolean match = false;
        for (String key : strategyDevice.getAndroidProperties().keySet()) {
            Object property = strategyDevice.getAndroidProperties().get(key);
            Object currentProperty = currentDevice.getAndroidProperties().get(key);
            if (property != null && currentProperty != null) {
                match = currentProperty.equals(property);
            }
        }
        return match;
    }
}
