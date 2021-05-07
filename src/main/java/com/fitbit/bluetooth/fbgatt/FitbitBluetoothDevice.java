/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.util.Bytes;
import com.fitbit.bluetooth.fbgatt.util.GattUtils;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanRecord;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import timber.log.Timber;

/**
 * A device wrapper to allow for mocking, will hold the bt device and abstract some of the
 * functionality ultimately will be held by the fitbit Device object
 *
 * Created by iowens on 11/7/17.
 */

public class FitbitBluetoothDevice {

    public enum DeviceOrigin {
        UNKNOWN,
        SCANNED,
        CONNECTED,
        BONDED
    }

    public interface DevicePropertiesChangedCallback {
        /**
         * Will indicate that the properties of one of the bluetooth devices that are cached
         * have changed, could be rssi, device name, or the {@link ScanRecord}
         * @param device The newly updated device
         */
        void onBluetoothPeripheralDevicePropertiesChanged(@NonNull FitbitBluetoothDevice device);
    }

    BluetoothDevice device;
    private String bluetoothAddress;
    private int rssi;
    @Nullable
    private ScanRecord scanRecord;
    private String name;
    DeviceOrigin origin = DeviceOrigin.UNKNOWN;
    private CopyOnWriteArrayList<DevicePropertiesChangedCallback> devicePropertiesChangedListeners = new CopyOnWriteArrayList<>();

    public FitbitBluetoothDevice(@NonNull BluetoothDevice device) {
        this.device = device;
        this.bluetoothAddress = device.getAddress();
        // this can throw a parcelable null pointer exception down in the stack
        try {
            this.name = new GattUtils().debugSafeGetBtDeviceName(device);
        } catch (NullPointerException ex) {
            this.name = "Unknown Device";
        }
    }


    public FitbitBluetoothDevice(String bluetoothAddress, String name) {
        this.bluetoothAddress = bluetoothAddress;
        this.name = name;
        this.device = FitbitGatt.getInstance().getBluetoothDevice(bluetoothAddress);
        Timber.i("new fitbit bluetooth Device %s", device.toString());
    }

    @VisibleForTesting
     FitbitBluetoothDevice(String bluetoothAddress, String name, BluetoothDevice device) {
        this.bluetoothAddress = bluetoothAddress;
        this.name = name;
        this.device = device;
        Timber.i("new fitbit bluetooth Device %s",device.toString());
    }

    public String getName(){
        return name;
    }

    void setName(String name) {
        this.name = name;
        notifyListenersOfPropertyChanged();
    }

    public BluetoothDevice getBtDevice(){
        return device;
    }

    public int getRssi() { return rssi; }

    /**
     * Register a listener for changes in this fitbit bluetooth device, will notify on change of name
     * or rssi or scanrecord
     * @param callback The callback to notify if the values change
     */
    public void addDevicePropertiesChangedListener(DevicePropertiesChangedCallback callback) {
        this.devicePropertiesChangedListeners.add(callback);
    }

    /**
     * Unregister a listener for changes in this fitbit bluetooth device, will notify on change of name
     * or rssi or scanrecord
     * @param callback The callback to notify if the values change
     */
    public void removeDevicePropertiesChangedListener(DevicePropertiesChangedCallback callback) {
        this.devicePropertiesChangedListeners.remove(callback);
    }

    /**
     * Unregister all devicePropertiesChangedListeners from this fitbit bluetooth device
     */

    public void removeAllDevicePropertiesChangedListeners(){
        this.devicePropertiesChangedListeners.clear();
    }

    /**
     * Will indicate the last origin of the device, whether it was discovered in a scan, added
     * because it was an already connected device, or because it was a bonded device
     * @return The device origin of the device
     */
    public DeviceOrigin getOrigin() {
        return origin;
    }

    /**
     * Scan Record which is useful for getting service data for further filtering
     * @return The scan record
     */

    public @Nullable ScanRecord getScanRecord() {
        return scanRecord;
    }



    public boolean equals(BluetoothDevice obj) {
        return ((BluetoothDevice) obj).getAddress().equals(this.bluetoothAddress);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FitbitBluetoothDevice){
            return ((FitbitBluetoothDevice)obj).getAddress().equals(this.bluetoothAddress);
        } else {
            throw new RuntimeException("Can't compare this kind of thing and FitbitBluetoothDevice");
        }
    }

    /**
     * Overriding hashCode so that when stored in a map as a key, a new instance of the wrapper can
     * be used to retrieve a {@link GattConnection} that was stored with a different instance of
     * {@link FitbitBluetoothDevice}
     * @return an int representing this object
     */
    @Override
    public int hashCode() {
        return bluetoothAddress.hashCode();
    }

    public String getAddress() {
        return bluetoothAddress;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
        notifyListenersOfPropertyChanged();
    }

    void setScanRecord(ScanRecord scanRecord) {
        this.scanRecord = scanRecord;
        notifyListenersOfPropertyChanged();
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH,
                "[FitbitBluetoothDevice Address: %s, Name: %s, Rssi: %s, Advertising Data: %s, Device Origin: %s]",
                getAddress(), getName(), getRssi(),
                getScanRecord() == null ? null : Bytes.byteArrayToHexString(getScanRecord().getBytes()), origin.name());
    }

    private void notifyListenersOfPropertyChanged(){
        for(DevicePropertiesChangedCallback callback : devicePropertiesChangedListeners) {
            callback.onBluetoothPeripheralDevicePropertiesChanged(this);
        }
    }
}
