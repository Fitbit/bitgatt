/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.util;

public enum ScanFailedReason {
    SCAN_SUCCESS_NO_ERROR(0),
    SCAN_FAILED_ALREADY_STARTED(1),
    SCAN_FAILED_APPLICATION_REGISTRATION_FAILED(2),
    SCAN_FAILED_INTERNAL_ERROR(3),
    SCAN_FAILED_FEATURE_UNSUPPORTED(4),
    SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES(5),
    SCAN_FAILED_SCANNING_TOO_FREQUENTLY(6),
    SCAN_FAILED_UNKNOWN_REASON(7);

    private final int code;

    ScanFailedReason(int code) {
        this.code = code;
    }

    public int getCode(){
        return this.code;
    }

    public static ScanFailedReason getReasonForCode(int code) {
        for(ScanFailedReason reason : ScanFailedReason.values()) {
            if(reason.code == code) {
                return reason;
            }
        }
        return SCAN_FAILED_UNKNOWN_REASON;
    }
}
