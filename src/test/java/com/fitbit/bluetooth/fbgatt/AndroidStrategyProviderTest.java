/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import com.fitbit.bluetooth.fbgatt.strategies.Strategy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AndroidStrategyProviderTest {
    private static final String MOCK_ADDRESS = "02:00:00:00:00:00";
    private AndroidDevice currentDevice;
    private Context mockContext;

    @Before
    public void beforeTest() {
        mockContext = ApplicationProvider.getApplicationContext();
        currentDevice = new AndroidDevice.Builder().
            device("custom").
            deviceModel("SGH-2000").
            apiLevel(24).
            board("Exynos3000").
            bootloader("AOSP").
            brand("Samsung").
            display("Super AMOLED").
            fingerprint("SHA2048").
            hardware("Deuces").
            host("com.samsung.usa").
            id("2704").
            manufacturerName("samsung").
            product("S12").
            radioVersion("B21W17N11").
            type("Android").build();
    }

    @Test
    public void strategyProviderShouldNotReturnNull(){
        AndroidDevice device = new AndroidDevice.Builder().apiLevel(24).manufacturerName("Samsung").build();
        FitbitBluetoothDevice mock = mock(FitbitBluetoothDevice.class);
        doReturn(MOCK_ADDRESS).when(mock).getAddress();
        doReturn("foo").when(mock).getName();
        Strategy s = new StrategyProvider().getStrategyForPhoneAndGattConnection(currentDevice, device, new GattConnection(mock, mockContext.getMainLooper()), Situation.TRACKER_WENT_AWAY_DURING_GATT_OPERATION);
        assertNotNull(s);
    }

    @Test
    public void currentDeviceEqualsShouldReturnTrue(){
        AndroidDevice currentDevice = new AndroidDevice.Builder().
            device("custom").
            deviceModel("SGH-2000").
            apiLevel(24).
            board("Exynos3000").
            bootloader("AOSP").
            brand("Samsung").
            display("Super AMOLED").
            fingerprint("SHA2048").
            hardware("Deuces").
            host("com.samsung.usa").
            id("2704").
            manufacturerName("samsung").
            product("S12").
            radioVersion("B21W17N11").
            type("Android").build();
        boolean match = new StrategyProvider().currentDeviceHasEqualDefinedProperties(currentDevice, new AndroidDevice.Builder().apiLevel(24).brand("Samsung").build());
        assertTrue("match should be true", match);
    }

    @Test
    public void currentDeviceEqualsNullShouldReturnTrue(){
        AndroidDevice currentDevice = new AndroidDevice.Builder().
            device("custom").
            deviceModel("SGH-2000").
            apiLevel(24).
            board("Exynos3000").
            bootloader("AOSP").
            brand("Samsung").
            display("Super AMOLED").
            fingerprint("SHA2048").
            hardware("Deuces").
            host("com.samsung.usa").
            id("2704").
            manufacturerName("samsung").
            product("S12").
            radioVersion("B21W17N11").
            type("Android").build();
        boolean match = new StrategyProvider().currentDeviceHasEqualDefinedProperties(currentDevice,null);
        assertTrue("match should be true", match);
    }

    @Test
    public void currentDeviceEqualsShouldReturnFalse(){
        AndroidDevice currentDevice = new AndroidDevice.Builder().
            device("custom").
            deviceModel("SGH-2000").
            apiLevel(24).
            board("Exynos3000").
            bootloader("AOSP").
            brand("Samsung").
            display("Super AMOLED").
            fingerprint("SHA2048").
            hardware("Deuces").
            host("com.samsung.usa").
            id("2704").
            manufacturerName("samsung").
            product("S12").
            radioVersion("B21W17N11").
            type("Android").build();
        boolean match = new StrategyProvider().currentDeviceHasEqualDefinedProperties(currentDevice, new AndroidDevice.Builder().apiLevel(22).brand("Samsung").build());
        assertTrue("match should be true", match);
    }

    @Test
    public void currentDeviceEqualsShouldReturnTrueWithSameApiLowercase(){
        AndroidDevice currentDevice = new AndroidDevice.Builder().
            device("custom").
            deviceModel("SGH-2000").
            apiLevel(24).
            board("Exynos3000").
            bootloader("AOSP").
            brand("Samsung").
            display("Super AMOLED").
            fingerprint("SHA2048").
            hardware("Deuces").
            host("com.samsung.usa").
            id("2704").
            manufacturerName("samsung").
            product("S12").
            radioVersion("B21W17N11").
            type("Android").build();
        boolean match = new StrategyProvider().currentDeviceHasEqualDefinedProperties(currentDevice, new AndroidDevice.Builder().apiLevel(24).brand("samsung").build());
        assertTrue("match should be true", match);
    }

    @Test
    public void currentDeviceEqualsShouldReturnTrueWithDeviceNameMatchOnly(){
        AndroidDevice currentDevice = new AndroidDevice.Builder().
            device("custom").
            deviceModel("Pixel 2").
            apiLevel(28).
            board("Qualcomm825").
            bootloader("AOSP").
            brand("Google").
            display("AMOLED").
            fingerprint("SHA2048").
            hardware("Deuces").
            host("com.google").
            id("2724").
            manufacturerName("htc").
            product("HT831").
            radioVersion("B21W17N11").
            type("Android").build();
        boolean match = new StrategyProvider().currentDeviceHasEqualDefinedProperties(currentDevice, new AndroidDevice.Builder().deviceModel("Pixel 2").build());
        assertTrue("match should be true", match);
    }
}
