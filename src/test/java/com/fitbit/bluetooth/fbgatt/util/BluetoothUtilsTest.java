/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowBluetoothAdapter;

@RunWith(RobolectricTestRunner.class)
@Config(minSdk = 21)
public class BluetoothUtilsTest {

    private Context ctx;
    private ShadowBluetoothAdapter shadowBluetoothAdapter;
    private BluetoothUtils utils = new BluetoothUtils();

    @Before
    public void before() {
        ctx = ApplicationProvider.getApplicationContext();
        shadowBluetoothAdapter = shadowOf(utils.getBluetoothAdapter(ctx));
    }

    @Test
    public void getBluetoothAdapter_shouldReturnNullIfBluetoothNotSupported() {
        ShadowBluetoothAdapter.setIsBluetoothSupported(false);
        BluetoothAdapter adapter = utils.getBluetoothAdapter(ctx);
        assertNull(adapter);
    }

    @Test
    public void getBluetoothAdapter_shouldReturnAdapterIfBluetoothIsSupported() {
        ShadowBluetoothAdapter.setIsBluetoothSupported(true);
        BluetoothAdapter adapter = utils.getBluetoothAdapter(ctx);
        assertNotNull(adapter);
    }

    @Test
    public void getBluetoothLeScanner_shouldReturnNullIfBluetoothNotSupported() {
        ShadowBluetoothAdapter.setIsBluetoothSupported(false);

        BluetoothLeScanner scanner = utils.getBluetoothLeScanner(ctx);
        assertNull(scanner);
    }

    @Test
    public void getBluetoothLeScanner_shouldReturnNullIfBluetoothNotEnabled() {
        shadowBluetoothAdapter.setEnabled(false);

        BluetoothLeScanner scanner = utils.getBluetoothLeScanner(ctx);
        assertNull(scanner);
    }

    @Test
    public void getBluetoothLeScanner_shouldReturnAdapterIfBluetoothIsSupported() {
        ShadowBluetoothAdapter.setIsBluetoothSupported(true);
        shadowBluetoothAdapter.setEnabled(true);
        BluetoothLeScanner scanner = utils.getBluetoothLeScanner(ctx);
        assertNotNull(scanner);
    }


    @Test
    public void isBluetoothEnabled_shouldReturnFalseIfBluetoothNotSupported() {
        ShadowBluetoothAdapter.setIsBluetoothSupported(false);

        boolean enabled = utils.isBluetoothEnabled(ctx);
        assertFalse(enabled);
    }

    @Test
    public void isBluetoothEnabled_shouldReturnTrueIfEnabled() {
        ShadowBluetoothAdapter.setIsBluetoothSupported(true);
        shadowBluetoothAdapter.setEnabled(true);
        boolean enabled = utils.isBluetoothEnabled(ctx);
        assertTrue(enabled);
    }
}
