/*
 *
 *  Copyright 2021 Fitbit, Inc. All rights reserved.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package com.fitbit.bluetooth.fbgatt.commands.server;

import com.fitbit.bluetooth.fbgatt.GattServerConnection;
import androidx.annotation.NonNull;

/**
 * Holder of the config data to be used to generate GattServerTransactions.
 */
public class GattServerTransactionConfig implements GattServerTransactionConfigInterface {
    private final String mac;
    private final String serviceUuid;
    private final String characteristicUuid;
    private final String descriptorUuid;
    private final int permissions;
    private final int properties;
    private final byte[] data;
    private final String requestId;
    private final String status;
    private final String offset;

    private final GattServerConnection serverConnection;

    public GattServerTransactionConfig(@NonNull GattServerConnection serverConnection, String mac, String serviceUuid, String characteristicUuid, String descriptorUuid, int permissions, int properties, byte[] data, String requestId, String status, String offset) {
        this.serverConnection = serverConnection;
        this.mac = mac;
        this.serviceUuid = serviceUuid;
        this.characteristicUuid = characteristicUuid;
        this.descriptorUuid = descriptorUuid;
        this.permissions = permissions;
        this.properties = properties;
        this.data = data;
        this.requestId = requestId;
        this.status = status;
        this.offset = offset;
    }

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
    public String getRequestId() {
        return this.requestId;
    }

    @Override
    public String getStatus() {
        return this.status;
    }

    @Override
    public String getOffset() {
        return this.offset;
    }

    @NonNull
    @Override
    public GattServerConnection getServerConnection() {
        return this.serverConnection;
    }
}
