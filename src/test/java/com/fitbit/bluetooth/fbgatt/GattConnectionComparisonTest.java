/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import android.bluetooth.BluetoothDevice;
import android.os.Looper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.ArrayList;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowBluetoothDevice;

import static org.mockito.Mockito.mock;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class GattConnectionComparisonTest {
    private static final String MOCK_ADDRESS = "02:00:00:00:00:00";
    private static final String MOCK_ADDRESS_2 = "04:00:00:00:00:00";
    private static final String FOO_1 = "foo1";
    private static final String FOO_2 = "foo2";

    @After
    public void afterTest(){
        FitbitGatt.getInstance().clearConnectionsMap();
    }

    @Test
    public void testConnectionsNotEqualSameFitbitBluetoothDevice(){
        BluetoothDevice mockDevice = getMockDeviceFor();
        FitbitBluetoothDevice fitbitBluetoothDevice = new FitbitBluetoothDevice(mockDevice);
        GattConnection conn1 = new GattConnection(fitbitBluetoothDevice, Looper.getMainLooper());
        conn1.setMockMode(true);
        GattConnection conn2 = new GattConnection(fitbitBluetoothDevice, Looper.getMainLooper());
        conn2.setMockMode(true);
        Assert.assertNotEquals("The two connections should not be equal because they are different instances", conn1, conn2);
    }

    @Test
    public void testAddDifferentInstanceSameBluetoothDevice(){
        BluetoothDevice mockDevice = getMockDeviceFor();
        FitbitBluetoothDevice fitbitBluetoothDevice = new FitbitBluetoothDevice(mockDevice);
        GattConnection conn1 = new GattConnection(fitbitBluetoothDevice, Looper.getMainLooper());
        conn1.setMockMode(true);
        FitbitGatt.getInstance().putConnectionIntoDevices(fitbitBluetoothDevice, conn1);
        GattConnection conn2 = new GattConnection(fitbitBluetoothDevice, Looper.getMainLooper());
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
        GattConnection conn1 = new GattConnection(fitbitBluetoothDevice, Looper.getMainLooper());
        conn1.setMockMode(true);
        GattConnection conn2 = new GattConnection(fitbitBluetoothDevice1, Looper.getMainLooper());
        conn2.setMockMode(true);
        Assert.assertNotEquals("The two connections should not be equal because they are different instances", conn1, conn2);
    }

    @Test
    public void testConnectionsNotEqualDifferentFitbitBluetoothDeviceSameBluetoothDevice(){
        BluetoothDevice mockDevice = getMockDeviceFor();
        FitbitBluetoothDevice fitbitBluetoothDevice = new FitbitBluetoothDevice(mockDevice);
        FitbitBluetoothDevice fitbitBluetoothDevice1 = new FitbitBluetoothDevice(mockDevice);
        GattConnection conn1 = new GattConnection(fitbitBluetoothDevice, Looper.getMainLooper());
        conn1.setMockMode(true);
        GattConnection conn2 = new GattConnection(fitbitBluetoothDevice1, Looper.getMainLooper());
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
        GattConnection conn = new GattConnection(fitbitBluetoothDevice, Looper.getMainLooper());
        conn.setMockMode(true);
        conn.setState(GattState.DISCONNECTED);
        FitbitGatt.getInstance().getConnectionMap().put(fitbitBluetoothDevice, conn);
        Assert.assertEquals(conn, FitbitGatt.getInstance().getConnectionMap().get(fitbitBluetoothDevice));
    }

    private BluetoothDevice getMockDeviceFor() {
        BluetoothDevice bluetoothDevice = ShadowBluetoothDevice.newInstance(MOCK_ADDRESS);
        shadowOf(bluetoothDevice).setName(FOO_1);
        return bluetoothDevice;
    }
}
