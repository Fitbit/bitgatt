/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class ScanFailureEnumTest {

    @Test
    public void testScanFailureOutOfBounds(){
        assertEquals(ScanFailure.UNKNOWN, ScanFailure.getFailureForReason(89));
    }

    @Test
    public void testScanFailureNotRegisterApplication(){
        assertEquals(ScanFailure.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED, ScanFailure.getFailureForReason(2));
    }
}
