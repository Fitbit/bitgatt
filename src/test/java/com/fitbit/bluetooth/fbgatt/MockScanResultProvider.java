/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.os.ParcelUuid;

import org.junit.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Will provide somewhat random mock {@link android.bluetooth.le.ScanResult} values to test the
 * filtering provided by the scanner
 *
 * Created by iowens on 9/4/18.
 */
public class MockScanResultProvider {

    private Random rnd;
    private ArrayList<ScanResult> scanResults;
    private Map<ParcelUuid, byte[]> serviceDataMap;

    MockScanResultProvider(int numberOfMockResults, int minRssi, int maxRssi){
        rnd = new Random(System.currentTimeMillis());
        scanResults = new ArrayList<>(numberOfMockResults);
        serviceDataMap = new HashMap<>();
        byte[] randomData = new byte[16];
        rnd.nextBytes(randomData);
        serviceDataMap.put(new ParcelUuid(UUID.fromString("adabfb00-6e7d-4601-bda2-bffaa68956ba")), randomData);
        for(int i=0; i < numberOfMockResults; i++) {
            ScanResult result = mock(ScanResult.class);
            BluetoothDevice device = mock(BluetoothDevice.class);
            ScanRecord record = mock(ScanRecord.class);
            when(device.getAddress()).thenReturn(randomMACAddress());
            when(device.getName()).thenReturn("foobar-" + String.valueOf(i));
            when(result.getDevice()).thenReturn(device);
            when(result.getRssi()).thenReturn(-1 * (rnd.nextInt(Math.abs(minRssi) + 1 - Math.abs(maxRssi)) + Math.abs(maxRssi)));
            Assert.assertTrue("Rssi is less than zero", result.getRssi() < 0);
            when(record.getDeviceName()).thenReturn("foobar-" + String.valueOf(i));
            when(record.getServiceData()).thenReturn(serviceDataMap);
            scanResults.add(result);
        }
    }

    List<ScanResult> getAllResults() {
        return scanResults;
    }

    private String randomMACAddress(){
        Random rand = new Random();
        byte[] macAddr = new byte[6];
        rand.nextBytes(macAddr);

        macAddr[0] = (byte)(macAddr[0] & (byte)254);  //zeroing last 2 bytes to make it unicast and locally adminstrated

        StringBuilder sb = new StringBuilder(18);
        for(byte b : macAddr){

            if(sb.length() > 0)
                sb.append(":");

            sb.append(String.format("%02x", b));
        }


        return sb.toString();
    }

}
