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

public class CharacteristicNotificationDescriptor extends BluetoothGattDescriptor {

    private static final UUID notificationDescriptorTwentyNineOhTwo = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    /**
     * Will create a 0x2902 characteristic descriptor
     */
    public CharacteristicNotificationDescriptor() {
        super(notificationDescriptorTwentyNineOhTwo, BluetoothGattDescriptor.PERMISSION_READ |
                BluetoothGattDescriptor.PERMISSION_WRITE |
                BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED |
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED);
    }
}
