/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for Bytes utility
 * <p>
 * Created by jrodriguez on 10/15/2018
 */
@RunWith(JUnit4.class)
public class BytesTest {
    @Test
    public void testIsValidHexString() {
        Boolean isValid;
        //Assert.assertTrue("This test has been executed",false);
        /* valid cases */
        isValid = Bytes.isValidHexString("0123456789");
        Assert.assertTrue("All decdigits should be accepted (in pairs)", isValid);
        isValid = Bytes.isValidHexString("abcdef");
        Assert.assertTrue("All hexdigits should be accepted (in pairs)", isValid);
        isValid = Bytes.isValidHexString("ABCDEF");
        Assert.assertTrue("Lowercase or uppercase should be accepted",isValid);
        isValid = Bytes.isValidHexString("00");
        Assert.assertTrue("One octet should be accepted",isValid);
        isValid = Bytes.isValidHexString("35bB");
        Assert.assertTrue("Two octets should be accepted",isValid);
        isValid = Bytes.isValidHexString("00112233445566778899aabbccddeeffAABBCCDDEEFF");
        Assert.assertTrue("A long stream (in complete octets) should be accepted",isValid);
        /* illegal cases */
        isValid = Bytes.isValidHexString("0123456789a");
        Assert.assertFalse("An odd number of digits (incomplete octets) should be rejected",isValid);
        isValid = Bytes.isValidHexString("abcdef0");
        Assert.assertFalse("An odd number of digits (incomplete octets) should be rejected",isValid);
        isValid = Bytes.isValidHexString("1");
        Assert.assertFalse("An odd number of digits (incomplete octets) should be rejected",isValid);
        isValid = Bytes.isValidHexString("aaB");
        Assert.assertFalse("An odd number of digits (incomplete octets) should be rejected",isValid);
        isValid = Bytes.isValidHexString("44  55");
        Assert.assertFalse("All octets should come as a single string",isValid);
        isValid = Bytes.isValidHexString("");
        Assert.assertFalse("Empty string should be rejected",isValid);
        isValid = Bytes.isValidHexString("44ww55");
        Assert.assertFalse("Non-hex should be rejected",isValid);
        isValid = Bytes.isValidHexString("uvwxyz");
        Assert.assertFalse("Non-hex should be rejected",isValid);
        isValid = Bytes.isValidHexString("-22");
        Assert.assertFalse("Negative sign should be rejected",isValid);
        isValid = Bytes.isValidHexString("0x22");
        Assert.assertFalse("Hex prefix should be rejected",isValid);
        isValid = Bytes.isValidHexString("$22");
        Assert.assertFalse("Hex prefix should be rejected",isValid);
    }
}
