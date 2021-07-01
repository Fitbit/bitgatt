/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.util;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.os.Message;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Sadly, it's hard to test loopers & handlers, will build this out further as we go, but I wanted
 * to make sure that continue is called
 *
 * Created by iowens on 10/10/19.
 */
@RunWith(RobolectricTestRunner.class)
public class LooperWatchdogTest {

    private static final int MESSAGE_QUEUE_STILL_ALIVE = 25341;

    @Test
    public void testAlertClearedIfMessageReceived() {
        LooperWatchdog dog = spy(new LooperWatchdog(ApplicationProvider.getApplicationContext().getMainLooper()));
        Message m = new Message();
        m.what = MESSAGE_QUEUE_STILL_ALIVE;
        dog.handleMessage(m);
        verify(dog, atLeastOnce()).continueProbing();
    }
}
