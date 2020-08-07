/*
 *  Copyright 2020 Fitbit, Inc. All rights reserved.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.util.BluetoothUtils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import androidx.test.platform.app.InstrumentationRegistry;

import static org.junit.Assert.*;

/**
 * Instrumentation test to play with scanner references
 * At the time of this commit these tests pass
 */
public class BitgattLeScannerTest {

    private Context context = InstrumentationRegistry.getInstrumentation().getContext();
    private BluetoothUtils bluetoothUtils = new BluetoothUtils();

    @Test
    public void testScannerConsistency() throws InterruptedException {
        BluetoothAdapter adapter = bluetoothUtils.getBluetoothAdapter(context);
        CountDownLatch cdl = new CountDownLatch(1);
        assert adapter != null;
        adapter.enable();

        cdl.await(1, TimeUnit.SECONDS);
        BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();

        assertNotNull(scanner);

        assertSame(scanner, adapter.getBluetoothLeScanner());

        adapter.disable();
        cdl.await(1, TimeUnit.SECONDS);


        adapter.enable();
        cdl.await(1, TimeUnit.SECONDS);

        assertSame(scanner, adapter.getBluetoothLeScanner());
    }
}
