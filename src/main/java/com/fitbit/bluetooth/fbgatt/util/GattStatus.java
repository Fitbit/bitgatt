/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.util;

import android.bluetooth.BluetoothGatt;

/**
 * To transfer gatt status codes to english, from Android O source
 * https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/5738f83aeb59361a0a2eda2460113f6dc9194271/stack/include/gatt_api.h
 *
 *
 * Created by iowens on 8/10/17.
 */

public enum GattStatus {
    GATT_SUCCESS(BluetoothGatt.GATT_SUCCESS),
    GATT_INVALID_HANDLE(1),
    GATT_READ_NOT_PERMIT(BluetoothGatt.GATT_READ_NOT_PERMITTED),
    GATT_WRITE_NOT_PERMIT(BluetoothGatt.GATT_WRITE_NOT_PERMITTED),
    GATT_INVALID_PDU(4),
    GATT_INSUF_AUTHENTICATION(BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION),
    GATT_REQ_NOT_SUPPORTED(BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED),
    GATT_INVALID_OFFSET(BluetoothGatt.GATT_INVALID_OFFSET),
    GATT_INSUF_AUTHORIZATION(8),
    GATT_PREPARE_Q_FULL(9),
    GATT_NOT_FOUND(10),
    GATT_NOT_LONG(11),
    GATT_INSUF_KEY_SIZE(12),
    GATT_INVALID_ATTR_LEN(BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH),
    GATT_ERR_UNLIKELY(14),
    GATT_INSUF_ENCRYPTION(BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION),
    GATT_UNSUPPORT_GRP_TYPE(16),
    GATT_INSUF_RESOURCE(17),
    GATT_ILLEGAL_PARAMETER(0x0083),
    GATT_NO_RESOURCES(0x0080),
    GATT_INTERNAL_ERROR(0x0081),
    GATT_WRONG_STATE(0x0082),
    GATT_DB_FULL(0x0083),
    GATT_BUSY(0x0084),
    GATT_ERROR(0x0085),
    GATT_CMD_STARTED(0x0086),
    GATT_PENDING(0x0088),
    GATT_AUTH_FAIL(0x0089),
    GATT_MORE(0x008a),
    GATT_INVALID_CFG(0x008b),
    GATT_SERVICE_STARTED(0x008c),
    GATT_ENCRYPED_MITM(GATT_SUCCESS.getCode()),
    GATT_ENCRYPED_NO_MITM(0x008d),
    GATT_NOT_ENCRYPTED(0x008e),
    GATT_CONGESTED(143), // requires L, not present in BluetoothGatt below api 20
    GATT_UNKNOWN(BluetoothGatt.GATT_FAILURE);

    private int code;

    GattStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }

    public static GattStatus getStatusForCode(int code) {
        for (int i = 0; i < GattStatus.values().length; i++) {
            if (GattStatus.values()[i].getCode() == code) {
                return GattStatus.values()[i];
            }
        }
        return GATT_UNKNOWN;
    }

}
