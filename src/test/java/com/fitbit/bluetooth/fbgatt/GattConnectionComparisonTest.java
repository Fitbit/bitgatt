/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Looper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import java.util.ArrayList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class GattConnectionComparisonTest {
    private static final String MOCK_ADDRESS = "02:00:00:00:00:00";
    private static final String MOCK_ADDRESS_2 = "04:00:00:00:00:00";
    private static final String FOO_1 = "foo1";
    private static final String FOO_2 = "foo2";
    private Looper mockLooper;

    @Before
    public void beforeTest() {
        mockLooper = mock(Looper.class);
        Context mockContext = mock(Context.class);
        when(mockContext.getMainLooper()).thenReturn(mockLooper);
    }

    @After
    public void afterTest(){
        FitbitGatt.getInstance().clearConnectionsMap();
    }

    @Test
    public void testConnectionsNotEqualSameFitbitBluetoothDevice(){
        BluetoothDevice mockDevice = getMockDeviceFor();
        FitbitBluetoothDevice fitbitBluetoothDevice = new FitbitBluetoothDevice(mockDevice);
        GattConnection conn1 = new GattConnection(fitbitBluetoothDevice, mockLooper);
        conn1.setMockMode(true);
        GattConnection conn2 = new GattConnection(fitbitBluetoothDevice, mockLooper);
        conn2.setMockMode(true);
        Assert.assertNotEquals("The two connections should not be equal because they are different instances", conn1, conn2);
    }

    @Test
    public void testAddDifferentInstanceSameBluetoothDevice(){
        BluetoothDevice mockDevice = getMockDeviceFor();
        FitbitBluetoothDevice fitbitBluetoothDevice = new FitbitBluetoothDevice(mockDevice);
        GattConnection conn1 = new GattConnection(fitbitBluetoothDevice, mockLooper);
        conn1.setMockMode(true);
        FitbitGatt.getInstance().putConnectionIntoDevices(fitbitBluetoothDevice, conn1);
        GattConnection conn2 = new GattConnection(fitbitBluetoothDevice, mockLooper);
        FitbitGatt.getInstance().putConnectionIntoDevices(fitbitBluetoothDevice, conn2);
        conn2.setMockMode(true);
        ArrayList<String> deviceNames = new ArrayList<>();
        deviceNames.add(FOO_1);
        Assert.assertFalse("There should only be one connection in the map", FitbitGatt.getInstance().getMatchingConnectionsForDeviceNames(deviceNames).size() != 1);
    }

    @Test
    public void testConnectionsNotEqualDifferentFitbitBluetoothDeviceSameAddressAndName(){
        BluetoothDevice mockDevice = getMockDeviceFor();
        BluetoothDevice mockDevice2 = getMockDeviceFor();
        FitbitBluetoothDevice fitbitBluetoothDevice = new FitbitBluetoothDevice(mockDevice);
        FitbitBluetoothDevice fitbitBluetoothDevice1 = new FitbitBluetoothDevice(mockDevice2);
        GattConnection conn1 = new GattConnection(fitbitBluetoothDevice, mockLooper);
        conn1.setMockMode(true);
        GattConnection conn2 = new GattConnection(fitbitBluetoothDevice1, mockLooper);
        conn2.setMockMode(true);
        Assert.assertNotEquals("The two connections should not be equal because they are different instances", conn1, conn2);
    }

    @Test
    public void testConnectionsNotEqualDifferentFitbitBluetoothDeviceSameBluetoothDevice(){
        BluetoothDevice mockDevice = getMockDeviceFor();
        FitbitBluetoothDevice fitbitBluetoothDevice = new FitbitBluetoothDevice(mockDevice);
        FitbitBluetoothDevice fitbitBluetoothDevice1 = new FitbitBluetoothDevice(mockDevice);
        GattConnection conn1 = new GattConnection(fitbitBluetoothDevice, mockLooper);
        conn1.setMockMode(true);
        GattConnection conn2 = new GattConnection(fitbitBluetoothDevice1, mockLooper);
        conn2.setMockMode(true);
        Assert.assertNotEquals("The two connections should not be equal because they are different instances", conn1, conn2);
    }

    @Test
    public void testSameFitbitBluetoothDeviceDifferentBluetoothDevicesSameAddress(){
        BluetoothDevice mockDevice = getMockDeviceFor();
        BluetoothDevice mockDevice2 = getMockDeviceFor();
        FitbitBluetoothDevice fitbitBluetoothDevice = new FitbitBluetoothDevice(mockDevice);
        FitbitBluetoothDevice fitbitBluetoothDevice1 = new FitbitBluetoothDevice(mockDevice2);
        Assert.assertEquals(fitbitBluetoothDevice, fitbitBluetoothDevice1);
    }

    @Test
    public void testSameFitbitBluetoothDeviceSameBluetoothDeviceSameAddress(){
        BluetoothDevice mockDevice = getMockDeviceFor();
        FitbitBluetoothDevice fitbitBluetoothDevice = new FitbitBluetoothDevice(mockDevice);
        FitbitBluetoothDevice fitbitBluetoothDevice1 = new FitbitBluetoothDevice(mockDevice);
        Assert.assertEquals(fitbitBluetoothDevice, fitbitBluetoothDevice1);
    }

    @Test
    public void testSameFitbitBluetoothDeviceSameSameAddress(){
        BluetoothDevice mockDevice = getMockDeviceFor();
        FitbitBluetoothDevice fitbitBluetoothDevice = new FitbitBluetoothDevice(MOCK_ADDRESS, FOO_1, mockDevice);
        FitbitBluetoothDevice fitbitBluetoothDevice1 = new FitbitBluetoothDevice(MOCK_ADDRESS, FOO_1, mockDevice);
        Assert.assertEquals(fitbitBluetoothDevice, fitbitBluetoothDevice1);
    }

    @Test
    public void testSameFitbitBluetoothDeviceSameSameAddressDifferentName(){
        BluetoothDevice mockDevice = getMockDeviceFor();
        FitbitBluetoothDevice fitbitBluetoothDevice = new FitbitBluetoothDevice(MOCK_ADDRESS, FOO_1, mockDevice);
        FitbitBluetoothDevice fitbitBluetoothDevice1 = new FitbitBluetoothDevice(MOCK_ADDRESS, FOO_2, mockDevice);
        Assert.assertEquals(fitbitBluetoothDevice, fitbitBluetoothDevice1);
    }

    @Test
    public void testSameFitbitBluetoothDeviceSameDifferentAddressDifferentName(){
        BluetoothDevice mockDevice = getMockDeviceFor();
        FitbitBluetoothDevice fitbitBluetoothDevice = new FitbitBluetoothDevice(MOCK_ADDRESS, FOO_1, mockDevice);
        FitbitBluetoothDevice fitbitBluetoothDevice1 = new FitbitBluetoothDevice(MOCK_ADDRESS_2, FOO_2, mockDevice);
        Assert.assertNotEquals(fitbitBluetoothDevice, fitbitBluetoothDevice1);
    }

    @Test
    public void testSameFitbitBluetoothDeviceSameDifferentAddressSameName(){
        BluetoothDevice mockDevice = getMockDeviceFor();
        FitbitBluetoothDevice fitbitBluetoothDevice = new FitbitBluetoothDevice(MOCK_ADDRESS, FOO_1, mockDevice);
        FitbitBluetoothDevice fitbitBluetoothDevice1 = new FitbitBluetoothDevice(MOCK_ADDRESS_2, FOO_1, mockDevice);
        Assert.assertNotEquals(fitbitBluetoothDevice, fitbitBluetoothDevice1);
    }

    @Test
    public void testConnectionMapGetWithDevice(){
        BluetoothDevice mockDevice = getMockDeviceFor();
        FitbitBluetoothDevice fitbitBluetoothDevice = new FitbitBluetoothDevice(MOCK_ADDRESS, FOO_1, mockDevice);
        GattConnection conn = new GattConnection(fitbitBluetoothDevice, mockLooper);
        conn.setMockMode(true);
        conn.setState(GattState.DISCONNECTED);
        FitbitGatt.getInstance().getConnectionMap().put(fitbitBluetoothDevice, conn);
        Assert.assertEquals(conn, FitbitGatt.getInstance().getConnectionMap().get(fitbitBluetoothDevice));
    }

    private BluetoothDevice getMockDeviceFor() {
        BluetoothDevice mockDevice = mock(BluetoothDevice.class);
        when(mockDevice.getAddress()).thenReturn(GattConnectionComparisonTest.MOCK_ADDRESS);
        when(mockDevice.getName()).thenReturn(GattConnectionComparisonTest.FOO_1);
        return mockDevice;
    }
}
