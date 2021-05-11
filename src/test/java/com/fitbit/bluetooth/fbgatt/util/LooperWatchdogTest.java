/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.util;

import android.os.Looper;
import android.os.Message;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Sadly, it's hard to test loopers & handlers, will build this out further as we go, but I wanted
 * to make sure that continue is called
 *
 * Created by iowens on 10/10/19.
 */
@RunWith(JUnit4.class)
public class LooperWatchdogTest {

    private static final int MESSAGE_QUEUE_STILL_ALIVE = 25341;

    @Test
    public void testAlertClearedIfMessageReceived() {
        Looper mockLooper = mock(Looper.class);
        LooperWatchdog dog = spy(new LooperWatchdog(mockLooper));
        Message m = new Message();
        m.what = MESSAGE_QUEUE_STILL_ALIVE;
        dog.handleMessage(m);
        verify(dog, atLeastOnce()).continueProbing();
    }
}
