/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AndroidDeviceTest {
    @Test
    public void testMismatchedPropertiesComparator() {
        AndroidDevice lhs = new AndroidDevice.Builder().manufacturerName("Samsung").build();
        AndroidDevice rhs = new AndroidDevice.Builder().host("com.android.google").build();
        Assert.assertFalse("These two devices should not be equal", lhs.equals(rhs));
        Assert.assertFalse("These two devices the other way should not be equal", rhs.equals(lhs));
    }

    @Test
    public void testEqualPropertiesDifferentCapitalization() {
        AndroidDevice lhs = new AndroidDevice.Builder().manufacturerName("Samsung").build();
        AndroidDevice rhs = new AndroidDevice.Builder().manufacturerName("Samsung").build();
        Assert.assertTrue("These two devices should be equal", lhs.equals(rhs));
        Assert.assertTrue("These two devices the other way should be equal", rhs.equals(lhs));
    }

    @Test
    public void testNoPropertiesNotEqual(){
        AndroidDevice lhs = new AndroidDevice.Builder().build();
        AndroidDevice rhs = new AndroidDevice.Builder().build();
        Assert.assertTrue("These two devices should be equal", lhs.equals(rhs));
        Assert.assertTrue("These two devices the other way should be equal", rhs.equals(lhs));
    }

    @Test
    public void testApiLevelsEqualManufacturerNotEqual(){
        AndroidDevice lhs = new AndroidDevice.Builder().apiLevel(22).manufacturerName("LG").build();
        AndroidDevice rhs = new AndroidDevice.Builder().apiLevel(22).manufacturerName("Motorola").build();
        Assert.assertFalse("These two devices should not be equal", lhs.equals(rhs));
        Assert.assertFalse("These two devices the other way should not be equal", rhs.equals(lhs));
    }

    @Test
    public void testApiLevelsEqualManufacturerEqualCase(){
        AndroidDevice lhs = new AndroidDevice.Builder().apiLevel(22).manufacturerName("Motorola").build();
        AndroidDevice rhs = new AndroidDevice.Builder().apiLevel(22).manufacturerName("Motorola").build();
        Assert.assertTrue("These two devices should be equal", lhs.equals(rhs));
        Assert.assertTrue("These two devices the other way should be equal", rhs.equals(lhs));
    }

    @Test
    public void testApiLevelsEqualManufacturerAsymmetricDefinition(){
        AndroidDevice lhs = new AndroidDevice.Builder().apiLevel(22).
                manufacturerName("Motorola").device("XT1204").
                hardware("XT1204Z7362873MFA").
                board("B72").
                bootloader("AOSP").
                brand("Motorola").
                product("Droid").
                type("Android").build();
        AndroidDevice rhs = new AndroidDevice.Builder().apiLevel(22).manufacturerName("Motorola").build();
        Assert.assertFalse("These two devices should not be equal", lhs.equals(rhs));
        Assert.assertFalse("These two devices the other way should not be equal", rhs.equals(lhs));
    }
}
