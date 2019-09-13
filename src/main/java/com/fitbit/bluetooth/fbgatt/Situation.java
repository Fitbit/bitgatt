/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

public enum Situation {
    TRACKER_WENT_AWAY_DURING_GATT_OPERATION,
    DELAY_ANDROID_SUBSCRIPTION_EVENT,
    CLEAR_GATT_SERVER_SERVICES_DEVICE_FUNKY_BT_IMPL,
    DEFAULT
}
