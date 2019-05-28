/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

/**
 * Gatt disconnect reason enum, codes from O source
 * https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/5738f83aeb59361a0a2eda2460113f6dc9194271/stack/include/gatt_api.h
 *
 * Created by iowens on 8/10/17.
 */

enum GattDisconnectReason {
    GATT_CONN_UNKNOWN(0),
    GATT_CONN_NO_RESOURCES(4),              /* connection fail for l2cap resource failure */
    GATT_CONN_TIMEOUT(8),                   /* 0x08 connection timeout  */
    GATT_CONN_TERMINATE_PEER_USER(19),      /* 0x13 connection terminated by peer user  */
    GATT_CONN_TERMINATE_LOCAL_HOST(22),     /* 0x16 connection terminated by local host  */
    GATT_CONN_FAIL_ESTABLISH(62),           /* 0x03E connection failed to establish  */
    GATT_CONN_LMP_TIMEOUT(34),              /* 0x22 connection failed for LMP response tout */
    GATT_CONN_CANCEL(256);                  /* 0x0100 L2CAP connection cancelled  */

    private int code;

    GattDisconnectReason(int code) {
        this.code = code;
    }

    int getCode() {
        return this.code;
    }

    static GattDisconnectReason getReasonForCode(int code) {
        for (int i = 0; i < GattDisconnectReason.values().length; i++) {
            if (GattDisconnectReason.values()[i].getCode() == code) {
                return GattDisconnectReason.values()[i];
            }
        }
        return GATT_CONN_UNKNOWN;
    }

}
