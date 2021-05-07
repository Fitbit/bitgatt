/*
 *
 *  Copyright 2021 Fitbit, Inc. All rights reserved.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package com.fitbit.bluetooth.fbgatt.commands.client;

import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattServerConnection;
import androidx.annotation.NonNull;

/**
 * Holder of the config data to be used to generate GattClientTransactions.
 */
public class GattClientTransactionConfig implements GattClientTransactionConfigInterface {
    @NonNull
    private final String mac;
    private final String serviceUuid;
    private final String characteristicUuid;
    private final String descriptorUuid;
    private final int permissions;
    private final int properties;
    private final byte[] data;
    private final int txPhy;
    private final int rxPhy;
    private final int phyOptions;
    private final String connectionInterval;
    private final int mtu;

    private GattConnection gattConnection;
    private final GattServerConnection serverConnection;

    GattClientTransactionConfig(@NonNull GattServerConnection serverConnection, @NonNull String mac, String serviceUuid, String characteristicUuid, String descriptorUuid, int permissions, int properties, byte[] data, int txPhy, int rxPhy, int phyOptions, String connectionInterval, int mtu) {
        this.serverConnection = serverConnection;
        this.mac = mac;
        this.serviceUuid = serviceUuid;
        this.characteristicUuid = characteristicUuid;
        this.descriptorUuid = descriptorUuid;
        this.permissions = permissions;
        this.properties = properties;
        this.data = data;
        this.txPhy = txPhy;
        this.rxPhy = rxPhy;
        this.phyOptions = phyOptions;
        this.connectionInterval = connectionInterval;
        this.mtu = mtu;
    }

    @NonNull
    @Override
    public String getMac() {
        return this.mac;
    }

    @Override
    public String getServiceUuid() {
        return this.serviceUuid;
    }

    @Override
    public String getCharacteristicUuid() {
        return this.characteristicUuid;
    }

    @Override
    public String getDescriptorUuid() {
        return this.descriptorUuid;
    }

    @Override
    public int getPermissions() {
        return this.permissions;
    }

    @Override
    public int getProperties() {
        return this.properties;
    }

    @Override
    public byte[] getData() {
        return this.data;
    }

    @Override
    public int getTxPhy() {
        return this.txPhy;
    }

    @Override
    public int getRxPhy() {
        return this.rxPhy;
    }

    @Override
    public int getPhyOptions() {
        return this.phyOptions;
    }

    @Override
    public String getConnectionInterval() {
        return this.connectionInterval;
    }

    @Override
    public int getMtu() {
        return this.mtu;
    }

    @Override
    public void setConnection(@NonNull GattConnection connection) {
        this.gattConnection = connection;
    }

    @NonNull
    @Override
    public GattConnection getConnection() {
        if (this.gattConnection == null) {
            throw new IllegalStateException("GattConnection was null.");
        }

        return this.gattConnection;
    }

    @NonNull
    @Override
    public GattServerConnection getServerConnection() {
        return this.serverConnection;
    }
}
