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

import com.fitbit.bluetooth.fbgatt.GattClientTransaction;
import com.fitbit.bluetooth.fbgatt.commands.GattCommandInterface;

/**
 * Interface defining the common behaviour of Gatt client commands.
 */
public interface GattClientCommandInterface extends GattCommandInterface<GattClientTransaction, GattClientTransactionConfigInterface> {

}
