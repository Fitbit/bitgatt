/*
 * Copyright 2020 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.fitbit.bluetooth.fbgatt;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@RunWith(JUnit4.class)
public class TransactionQueueControllerTest {

    private FitbitBluetoothDevice device = mock(FitbitBluetoothDevice.class);
    private GattConnection gattConnectionMock = mock(GattConnection.class);
    private TransactionQueueController sut;

    @Before
    public void before() {
        doReturn("SUTBTDevice").when(device).getName();
        doReturn(device).when(gattConnectionMock).getDevice();
        sut = new TransactionQueueController(gattConnectionMock);
    }

    @Test
    public void addingARunnableStartsTheThread() throws InterruptedException {
        CountDownLatch cdl = new CountDownLatch(1);
        assertTrue(sut.isQueueThreadStopped());

        sut.queueTransaction(cdl::countDown);

        cdl.await(100, TimeUnit.MILLISECONDS);

        assertEquals(0, cdl.getCount());
        assertFalse(sut.isQueueThreadStopped());
    }

    @Test
    public void controllerIsAbleToRestart() throws InterruptedException {
        CountDownLatch cdl = new CountDownLatch(1);
        assertTrue(sut.isQueueThreadStopped());

        sut.start();
        assertFalse(sut.isQueueThreadStopped());
        sut.stop();
        assertTrue(sut.isQueueThreadStopped());

        sut.queueTransaction(cdl::countDown);
        cdl.await(100, TimeUnit.MILLISECONDS);

        assertEquals(0, cdl.getCount());
        assertFalse(sut.isQueueThreadStopped());
    }

    @Test
    public void controllerShouldHandleInterupts() {
        CountDownLatch cdl = new CountDownLatch(1);
        assertTrue(sut.isQueueThreadStopped());

        sut.queueTransaction(() -> {
            cdl.countDown();
            Thread.currentThread().interrupt();
        });

        try {
            cdl.await(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            fail("Latch interrupted");
        }

        assertEquals(0, cdl.getCount());
        assertFalse(sut.isQueueThreadStopped());
    }
}
