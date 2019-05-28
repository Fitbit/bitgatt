/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.descriptors;

import android.bluetooth.BluetoothGattDescriptor;

import java.util.UUID;

public class CharacteristicNamespaceDescriptor extends BluetoothGattDescriptor {
    private static final UUID notificationDescriptorTwentyNineOhFour = UUID.fromString("00002904-0000-1000-8000-00805f9b34fb");
    /**
     * Will create a 0x2904 characteristic descriptor
     */
    public CharacteristicNamespaceDescriptor() {
        super(notificationDescriptorTwentyNineOhFour, BluetoothGattDescriptor.PERMISSION_READ |
                BluetoothGattDescriptor.PERMISSION_WRITE |
                BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED |
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED);
    }
}
