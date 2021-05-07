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

/**
 * Interface to be used for providing necessary Bluetooth arguments' indexes.
 */
public interface BluetoothArgsInterface {
    int getMacArgIndex();

    int getServiceArgIndex();

    int getCharacteristicArgIndex();

    int getDescriptorArgIndex();

    int getPermissionsArgIndex();

    int getPropertiesArgIndex();

    int getDataArgIndex();

    int getTxPhyArgIndex();

    int getRxPhyArgIndex();

    int getPhyOptionsArgIndex();

    int getRequestIdArgIndex();

    int getStatusArgIndex();

    int getOffsetArgIndex();

    int getConnectionIntervalArgIndex();

    int getMtuArgIndex();
}
